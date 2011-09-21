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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.copyResourceToFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class ExcursionPlugin extends JavaPlugin {

    private final Map<String, String> aliasMap = new HashMap<String, String>();

    private final Map<String, String> groupMap = new HashMap<String, String>();
    
    private final Set<String> blacklist = new HashSet<String>();

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
        log("%s disabled.", getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        log("%s enabled.", getDescription().getVersion());
        
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        // Create config file, if needed
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            copyResourceToFile(this, "config.yml", configFile);
            // Re-load config
            getConfiguration().load();
        }

        // Read config
        readConfig();

        int rows = 0;
        
        // Create tables if they don't already exist
        try {
            // Check if the main table exists
            rows = getDatabase().createQuery(SavedLocation.class).findRowCount();
        }
        catch (PersistenceException e) {
            log("Creating SQL tables...");
            installDDL();
            log("Done.");
        }

        log("Database contains %d saved location%s.", rows, rows == 1 ? "" : "s");

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
    }

    void log(String format, Object... args) {
        ToHUtils.log(this, Level.INFO, format, args);
    }

    private void readConfig() {
        Configuration config = getConfiguration();
        config.load();
        
        // Aliases
        aliasMap.clear();
        ConfigurationNode aliasNode = config.getNode("aliases");
        if (aliasNode != null) {
            for (String key : aliasNode.getKeys()) {
                // keyed by world name
                List<String> aliases = aliasNode.getStringList(key, null);
                for (String alias : aliases)
                    aliasMap.put(alias, key);
            }
        }
        
        // Groups
        groupMap.clear();
        ConfigurationNode groupNode = config.getNode("groups");
        if (groupNode != null) {
            for (String key : groupNode.getKeys()) {
                // keyed by name of primary world
                List<String> members = groupNode.getStringList(key, null);
                for (String member : members)
                    groupMap.put(member, key);
            }
        }

        // Blacklist
        blacklist.clear();
        blacklist.addAll(config.getStringList("blacklist", null));
    }

    void reload() {
        getConfiguration().load();
        readConfig();
    }

}
