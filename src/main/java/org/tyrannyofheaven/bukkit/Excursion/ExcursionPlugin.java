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
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;

public class ExcursionPlugin extends JavaPlugin {

    private VersionInfo versionInfo;

    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

    private final Map<String, String> aliasMap = new HashMap<String, String>();

    private final Map<String, String> groupMap = new HashMap<String, String>();

    private final Map<String, GroupOptions> optionsMap = new HashMap<String, GroupOptions>();

    private final GroupOptions DEFAULT_GROUP_OPTIONS = new GroupOptions();

    private final Set<String> blacklist = new HashSet<String>();

    private FileConfiguration config;

    private ExcursionDao dao;

    private AvajeExcursionDao avajeDao;

    private ExecutorService asyncExecutor;

    static final Set<Material> solidBlocks;

    static final Set<Material> unsafeGround;

    static {
        // Make these lists configurable someday?
        
        // Solid blocks. Current as of Bukkit's Material.java 69374c7
        solidBlocks = Collections.unmodifiableSet(EnumSet.of(
                Material.STONE, Material.GRASS, Material.DIRT,
                Material.COBBLESTONE, Material.WOOD, Material.BEDROCK,
                Material.SAND, Material.GRAVEL, Material.GOLD_ORE,
                Material.IRON_ORE, Material.COAL_ORE, Material.LOG,
                Material.LEAVES, Material.SPONGE, Material.LAPIS_ORE,
                Material.LAPIS_BLOCK, Material.DISPENSER, Material.SANDSTONE,
                Material.NOTE_BLOCK, Material.WOOL, Material.GOLD_BLOCK,
                Material.IRON_BLOCK, Material.DOUBLE_STEP, Material.BRICK,
                Material.TNT, Material.BOOKSHELF, Material.MOSSY_COBBLESTONE,
                Material.OBSIDIAN, Material.DIAMOND_ORE,
                Material.DIAMOND_BLOCK, Material.WORKBENCH, Material.FURNACE,
                Material.BURNING_FURNACE, Material.REDSTONE_ORE,
                Material.GLOWING_REDSTONE_ORE, Material.SNOW_BLOCK,
                Material.CLAY, Material.JUKEBOX, Material.PUMPKIN,
                Material.NETHERRACK, Material.SOUL_SAND, Material.GLOWSTONE,
                Material.JACK_O_LANTERN, Material.LOCKED_CHEST,
                Material.MONSTER_EGGS, Material.SMOOTH_BRICK,
                Material.HUGE_MUSHROOM_1, Material.HUGE_MUSHROOM_2,
                Material.MELON_BLOCK, Material.MYCEL, Material.NETHER_BRICK,
                Material.ENDER_STONE, Material.REDSTONE_LAMP_OFF,
                Material.REDSTONE_LAMP_ON, Material.WOOD_DOUBLE_STEP,
                Material.EMERALD_ORE, Material.EMERALD_BLOCK, Material.COMMAND
                ));

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
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(SavedLocationId.class);
        result.add(SavedLocation.class);
        return result;
    }

    @Override
    public void onDisable() {
        // Cancel any pending teleports and clear state
        getServer().getScheduler().cancelTasks(this);

        asyncExecutor.shutdown();

        playerStates.clear();

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
        AsyncTransactionStrategy transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(getDatabase(), 1), asyncExecutor);
        avajeDao = new AvajeExcursionDao(getDatabase(), transactionStrategy.getExecutor());
        dao = new TransactionWrapperExcursionDao(avajeDao, transactionStrategy);
        avajeDao.load();

        (new ToHCommandExecutor<ExcursionPlugin>(this, new ExcursionCommand(this)))
            .registerTypeCompleter("destination", new ExcursionTypeCompleter(this))
            .registerCommands();
        
        (new ExcursionPlayerListener(this)).registerEvents();
        (new ExcursionEntityListener(this)).registerEvents();

        // Cheap way to determine solid blocks.
        // However, relies on obfuscated function.
        // Subvert to build our solid block list for now.
//        List<String> solids = new ArrayList<String>();
//        for (Material m : Material.values()) {
//            if (!m.isBlock()) continue;
//            if (m.getId() != 0 && net.minecraft.server.Block.byId[m.getId()].c())
//                solids.add("Material." + m);
//        }
//        log(this, "solids = %s", solids);

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
                }
            }
        }

        // Blacklist
        blacklist.clear();
        for (Object entry : config.getList("blacklist", Collections.emptyList())) {
            blacklist.add(entry.toString());
        }
        
        // Debug logging
        getLogger().setLevel(config.getBoolean("debug", false) ? Level.FINE : null);
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
        getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                avajeDao.load();
            }
        });
    }

    private PlayerState getPlayerState(String playerName, boolean create) {
        PlayerState ps;
        ps = playerStates.get(playerName);
        if (ps == null && create) {
            ps = new PlayerState();
            playerStates.put(playerName, ps);
        }
        return ps;
    }

    private PlayerState removePlayerState(String playerName) {
        return playerStates.remove(playerName);
    }

    void setTeleportTaskId(Player player, int taskId) {
        PlayerState ps = getPlayerState(player.getName(), true);
        // Just in case... cancel previous teleport task
        if (ps.getTaskId() != -1)
            getServer().getScheduler().cancelTask(ps.getTaskId());
        ps.setTaskId(taskId);
    }

    // NB: Does NOT cancel teleport task
    int clearTeleportTaskId(String playerName) {
        PlayerState ps = removePlayerState(playerName);
        return ps == null ? -1 : ps.getTaskId();
    }

    boolean cancelTeleportTask(String playerName) {
        int taskId = clearTeleportTaskId(playerName);
        if (taskId != -1) {
            debug(this, "Clearing teleport task for %s (%d)", playerName, taskId);
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
