/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.Excursion.dao.AvajeExcursionDao;
import org.tyrannyofheaven.bukkit.Excursion.dao.ExcursionDao;
import org.tyrannyofheaven.bukkit.Excursion.dao.TransactionWrapperExcursionDao;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocation;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocationId;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.uuid.MojangUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.UuidResolver;

public class ExcursionPlugin extends JavaPlugin {

    private static final String PLAYER_METADATA_KEY = "Excursion.PlayerState";

    private VersionInfo versionInfo;

    private final Map<String, String> aliasMap = new HashMap<>();

    private final Map<String, String> groupMap = new HashMap<>();

    private final Map<String, GroupOptions> optionsMap = new HashMap<>();

    private final GroupOptions DEFAULT_GROUP_OPTIONS = new GroupOptions();

    // Default max attempts (after the first) to complete a transaction
    private static final int DEFAULT_TXN_MAX_RETRIES = 3;

    private final Set<String> blacklist = new HashSet<>();

    private FileConfiguration config;

    private ExcursionDao dao;

    private AvajeExcursionDao avajeDao;

    private ExecutorService asyncExecutor;

    // Maximum number of times to retry transactions (so total attempts is +1)
    private int txnMaxRetries;

    static final Set<Material> solidBlocks;

    static final Set<Material> unsafeGround;

    static {
        // Make these lists configurable someday?

        // Build list of blocks that would harm the player should they teleport
        // inside.
        Set<Material> solids = new HashSet<>();
        for (Material m : Material.values()) {
            if (!m.isSolid()) continue; // Must be solid
            if (m.isTransparent()) continue; // Can't harm if fully transparent
            if (!m.isOccluding()) continue; // Can't harm if not fully occluding (e.g. STEP)
            solids.add(m);
        }
        solidBlocks = Collections.unmodifiableSet(EnumSet.copyOf(solids));

        // Unsafe ground
        unsafeGround = Collections.unmodifiableSet(EnumSet.of(
                Material.LAVA, Material.STATIONARY_LAVA, Material.FIRE,
                Material.CACTUS
                ));
    }

    ExcursionDao getDao() {
        return dao;
    }

    Map<String, String> getAliasMap() {
        return aliasMap;
    }

    Map<String, String> getGroupMap() {
        return groupMap;
    }

    GroupOptions getGroupOptions(String group) {
        GroupOptions options = optionsMap.get(group);
        return options == null ? DEFAULT_GROUP_OPTIONS : options;
    }

    Set<String> getBlacklist() {
        return blacklist;
    }

    @Override
    public void onLoad() {
        versionInfo = ToHUtils.getVersion(this);
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<>();
        result.add(SavedLocationId.class);
        result.add(SavedLocation.class);
        return result;
    }

    @Override
    public void onDisable() {
        // Cancel any pending teleports and clear state
        getServer().getScheduler().cancelTasks(this);

        asyncExecutor.shutdown();
        try {
            asyncExecutor.awaitTermination(60L, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            // Ignore
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Tasks already cancelled above, so just clear metadata
            player.removeMetadata(PLAYER_METADATA_KEY, this);
        }

        log(this, "%s disabled.", versionInfo.getVersionString());
    }

    @Override
    public void onEnable() {
        // Read config
        config = ToHFileUtils.getConfig(this);
        config.options().header(null);
        readConfig();

        // Upgrade/save config
        ToHFileUtils.upgradeConfig(this, config);

        int rows = 0;
        
        // Create tables if they don't already exist
        try {
            // Check if the main table exists
            rows = getDatabase().createQuery(SavedLocation.class).findRowCount();
        }
        catch (PersistenceException e) {
            log(this, "Creating SQL tables...");
            installDDL();
            log(this, "Done.");
        }

        log(this, "Database contains %d saved location%s.", rows, rows == 1 ? "" : "s");

        // Set up DAO
        asyncExecutor = Executors.newSingleThreadExecutor();
        AsyncTransactionStrategy transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(getDatabase(), txnMaxRetries), asyncExecutor);
        avajeDao = new AvajeExcursionDao(getDatabase(), transactionStrategy.getExecutor());
        dao = new TransactionWrapperExcursionDao(avajeDao, transactionStrategy);
        UuidResolver uuidResolver = new MojangUuidResolver(100, 5L, TimeUnit.MINUTES);
        try {
            avajeDao.migrate(uuidResolver);
        }
        catch (Exception e) {
            error(this, "Exception while migrating database:", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        avajeDao.load();

        (new ToHCommandExecutor<ExcursionPlugin>(this, new ExcursionCommand(this)))
            .registerTypeCompleter("destination", new ExcursionTypeCompleter(this))
            .registerCommands();
        
        (new ExcursionPlayerListener(this)).registerEvents();
        (new ExcursionEntityListener(this)).registerEvents();

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    private void readConfig() {
        // Aliases
        aliasMap.clear();
        ConfigurationSection aliasNode = config.getConfigurationSection("aliases");
        if (aliasNode != null) {
            for (String key : aliasNode.getKeys(false)) {
                // keyed by world name
                List<?> aliases = aliasNode.getList(key, Collections.emptyList());
                for (Object alias : aliases) {
                    aliasMap.put(alias.toString(), key);
                }
            }
        }
        
        // Groups
        groupMap.clear();
        ConfigurationSection groupNode = config.getConfigurationSection("groups");
        if (groupNode != null) {
            for (String key : groupNode.getKeys(false)) {
                // keyed by name of primary world
                List<?> members = groupNode.getList(key, Collections.emptyList());
                for (Object member : members)
                    groupMap.put(member.toString(), key);
            }
        }

        optionsMap.clear();

        // Options
        ConfigurationSection optionsNode = config.getConfigurationSection("options");
        if (optionsNode != null) {
            for (Map.Entry<String, Object> entry : optionsNode.getValues(false).entrySet()) {
                if (entry.getValue() instanceof ConfigurationSection) {
                    GroupOptions options = getGroupOptions(entry.getKey(), true);
                    ConfigurationSection groupOptions = (ConfigurationSection)entry.getValue();
                    
                    options.setDelay(groupOptions.getInt("delay", options.getDelay()));
                    options.setCancelOnAttack(groupOptions.getBoolean("attack-cancel", options.isCancelOnAttack()));
                    options.setCancelOnDamage(groupOptions.getBoolean("damage-cancel", options.isCancelOnDamage()));
                    options.setCancelOnMove(groupOptions.getBoolean("move-cancel", options.isCancelOnMove()));
                }
            }
        }

        // Blacklist
        blacklist.clear();
        for (Object entry : config.getList("blacklist", Collections.emptyList())) {
            blacklist.add(entry.toString());
        }
        
        txnMaxRetries = config.getInt("txn-max-retries", DEFAULT_TXN_MAX_RETRIES); // FIXME hidden

        // Debug logging
        getLogger().setLevel(config.getBoolean("debug", false) ? Level.CONFIG : null);
    }

    private GroupOptions getGroupOptions(String group, boolean create) {
        GroupOptions options = optionsMap.get(group);
        if (options == null && create) {
            options = new GroupOptions();
            optionsMap.put(group, options);
        }
        return options;
    }

    void reload() {
        config = ToHFileUtils.getConfig(this);
        readConfig();
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                avajeDao.load();
            }
        });
    }

    private PlayerState getPlayerState(Player player, boolean create) {
        PlayerState ps = getPlayerState(player);
        if (ps == null && create) {
            ps = new PlayerState();
            player.setMetadata(PLAYER_METADATA_KEY, new FixedMetadataValue(this, ps));
        }
        return ps;
    }

    private PlayerState getPlayerState(Player player) {
        PlayerState ps = null;
        for (MetadataValue mv : player.getMetadata(PLAYER_METADATA_KEY)) {
            if (mv.getOwningPlugin() == this) {
                ps = (PlayerState)mv.value();
                break;
            }
        }
        return ps;
    }

    private PlayerState removePlayerState(Player player) {
        PlayerState ps = getPlayerState(player);
        player.removeMetadata(PLAYER_METADATA_KEY, this);
        return ps;
    }

    void setTeleportTaskId(Player player, int taskId) {
        PlayerState ps = getPlayerState(player, true);
        // Just in case... cancel previous teleport task
        if (ps.getTaskId() != -1)
            getServer().getScheduler().cancelTask(ps.getTaskId());
        ps.setTaskId(taskId);
    }

    // NB: Does NOT cancel teleport task
    int clearTeleportTaskId(Player player) {
        PlayerState ps = removePlayerState(player);
        return ps == null ? -1 : ps.getTaskId();
    }

    boolean cancelTeleportTask(Player player) {
        int taskId = clearTeleportTaskId(player);
        if (taskId != -1) {
            debug(this, "Clearing teleport task for %s (%d)", player.getName(), taskId);
            getServer().getScheduler().cancelTask(taskId);
            return true;
        }
        return false;
    }

    String resolvePrimaryWorld(String worldName) {
        String primaryWorldName = getGroupMap().get(worldName);
        return primaryWorldName == null ? worldName : primaryWorldName;
    }

    private static class PlayerState {

        private int taskId = -1;

        public int getTaskId() {
            return taskId;
        }

        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

    }

}
