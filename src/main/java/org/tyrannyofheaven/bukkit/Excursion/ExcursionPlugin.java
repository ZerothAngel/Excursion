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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class ExcursionPlugin extends JavaPlugin {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Map<String, String> aliasMap = new HashMap<String, String>();

    private final Map<String, String> groupMap = new HashMap<String, String>();
    
    private final Set<String> blacklist = new HashSet<String>();

    private FileConfiguration config;

    private ExcursionDao dao;

    static final Set<Material> solidBlocks;

    static final Set<Material> unsafeGround;

    static {
        // Make these lists configurable someday?
        
        // Solid blocks
        Material[] solids = { Material.STONE, Material.GRASS, Material.DIRT,
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
                Material.MELON_BLOCK };
        solidBlocks = Collections.unmodifiableSet(new HashSet<Material>(Arrays.asList(solids)));

        // Unsafe ground
        Material[] unsafe = { Material.LAVA, Material.STATIONARY_LAVA,
                Material.FIRE, Material.CACTUS };
        unsafeGround = Collections.unmodifiableSet(new HashSet<Material>(Arrays.asList(unsafe)));
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

    Set<String> getBlacklist() {
        return blacklist;
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
        log(this, "%s disabled.", getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        // Read config
        config = ToHFileUtils.getConfig(this);
        readConfig();

        // Save config
        config.options().copyDefaults(true);
        ToHFileUtils.saveConfig(this, config);

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
        dao = new AvajeExcursionDao(this);

        (new ToHCommandExecutor<ExcursionPlugin>(this, new ExcursionCommand(this))).registerCommands();
        
        // Cheap way to determine solid blocks.
        // However, relies on obfuscated function.
        // Subvert to build our solid block list for now.
//        List<String> solids = new ArrayList<String>();
//        for (Material m : Material.values()) {
//            if (!m.isBlock()) continue;
//            if (m.getId() != 0 && net.minecraft.server.Block.byId[m.getId()].a())
//                solids.add("Material." + m);
//        }
//        log("solids = %s", solids);

        log(this, "%s enabled.", getDescription().getVersion());
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

        // Blacklist
        blacklist.clear();
        for (Object entry : config.getList("blacklist", Collections.emptyList()))
            blacklist.add(entry.toString());
        
        // Debug logging
        logger.setLevel(null);
        if (config.getBoolean("debug", false))
            logger.setLevel(Level.FINE);
    }

    void reload() {
        config = ToHFileUtils.getConfig(this);
        readConfig();
    }

}
