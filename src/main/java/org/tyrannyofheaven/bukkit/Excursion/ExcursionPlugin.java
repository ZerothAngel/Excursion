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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

public class ExcursionPlugin extends JavaPlugin {

    private final Logger logger = Logger.getLogger("Minecraft");

    private final Map<String, String> aliasMap = new HashMap<String, String>();

    private final Map<String, String> groupMap = new HashMap<String, String>();
    
    private final Set<String> blacklist = new HashSet<String>();

    private ExcursionDao dao;

    private static final Set<Material> unsafeGround;

    static {
        // Set up unsafe ground. Make configurable someday?
        Set<Material> materials = new HashSet<Material>();
        materials.add(Material.LAVA);
        materials.add(Material.STATIONARY_LAVA);
        materials.add(Material.FIRE);
        unsafeGround = Collections.unmodifiableSet(materials);
    }

    ExcursionDao getDao() {
        return dao;
    }

    private void setDao(ExcursionDao dao) {
        this.dao = dao;
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

        // Create config if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())
            writeDefaultConfig(configFile);

        // Read config
        parseConfig();

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

        setDao(new AvajeExcursionDao(this));
        getCommand("visit").setExecutor(this);
    }

    private void writeDefaultConfig(File configFile) {
        try {
            // Copy from internal version
            OutputStream os = new FileOutputStream(configFile);
            try {
                InputStream is = getClass().getResourceAsStream("config.yml");
                byte[] buffer = new byte[4096];
                int readLen;
                while ((readLen = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readLen);
                }
            }
            finally {
                os.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace(); // This really the right way to handle errors in bukkit?
        }
    }

    boolean visit(Player player, String group) {
        // Resolve destination world
        String alias = aliasMap.get(group);
        if (alias != null)
            group = alias;

        // Group members map to their primary world
        String primaryWorld = groupMap.get(group);
        if (primaryWorld == null)
            primaryWorld = group; // not in a group or is a primary world
            
        // Check if world exists
        World world = getServer().getWorld(primaryWorld);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Invalid world.");
            return true;
        }

        // Check world access
        // (group access based on primary world)
        if (!player.hasPermission("excursion.access.*") && !player.hasPermission("excursion.access." + primaryWorld)) {
            player.sendMessage(ChatColor.RED + "You need one of the following permissions to do this:");
            player.sendMessage(ChatColor.GREEN + "- excursion.access.*");
            player.sendMessage(ChatColor.GREEN + "- excursion.access." + primaryWorld);
            return true;
        }

        // Get current location
        Location currentLocation = player.getLocation();

        // Resolve primary world for current world
        String currentWorld = currentLocation.getWorld().getName();
        String currentPrimaryWorld = groupMap.get(currentWorld);
        if (currentPrimaryWorld == null)
            currentPrimaryWorld = currentWorld;

        if (currentPrimaryWorld.equals(primaryWorld)) {
            player.sendMessage(ChatColor.RED + "You are already there.");
            return true;
        }

        // Save location
        if (!blacklist.contains(currentPrimaryWorld))
            getDao().saveLocation(player, currentPrimaryWorld, currentLocation);

        // Get destination location
        Location newLocation = null;
        if (!blacklist.contains(primaryWorld)) {
            // Load previous location, if any
            newLocation = getDao().loadLocation(player, primaryWorld);

            // Check if destination is safe
            if (newLocation != null && !checkDestination(newLocation)) {
                player.sendMessage(ChatColor.YELLOW + "Destination is unsafe; teleporting to spawn.");
                // NB: If this is a group, the player goes to the primary world's spawn
                newLocation = null;
            }
        }
        if (newLocation == null) {
            // Player is visiting a new place, teleport to spawn
            newLocation = world.getSpawnLocation();
        }

        // Go there!
        player.teleport(newLocation);
        return true;
    }

    void log(String format, Object... args) {
        logger.info(String.format("[%s] %s", getDescription().getName(), String.format(format, args)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player)sender;

        // Check permission
        if (!player.hasPermission("excursion.visit")) {
            player.sendMessage(ChatColor.RED + "You need the following permission to do this:");
            player.sendMessage(ChatColor.GREEN + "- excursion.visit");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        return visit(player, args[0]);
    }

    private void parseConfig() {
        Configuration config = getConfiguration();
        
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

    private boolean isSolidBlock(Block block) {
        // Thanks to @Crash for pointing this out
        return block.getTypeId() != 0 && net.minecraft.server.Block.byId[block.getTypeId()].a();
    }

    private boolean checkDestination(Location location) {
        Block legs = location.getBlock();
        Block head = legs.getRelative(0, 1, 0);
        
        if (isSolidBlock(legs) || isSolidBlock(head))
            return false; // space is occupied
        
        final int MAX_HEIGHT = -4; // maximum number of air blocks to allow between legs and ground (relative to legs, so negative)
        Block ground = null;
        for (int i = 0; i >= MAX_HEIGHT; i--) { // NB: start at zero to allow for non-air, transparent blocks
            Block check = legs.getRelative(0, i, 0);
            if (!check.isEmpty()) {
                ground = check;
                break;
            }
        }
        
        if (ground == null)
            return false; // would take damage from falling
        
        return !unsafeGround.contains(ground.getType());
    }

}
