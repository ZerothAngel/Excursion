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
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TeleportHelper {

    private final ExcursionPlugin plugin;

    public TeleportHelper(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, String destPrimaryWorldName) {
        if (player.getHealth() < 1) {
            sendMessage(player, colorize("{RED}Teleport cancelled because you are dead!"));
            return;
        }

        World destPrimaryWorld = plugin.getServer().getWorld(destPrimaryWorldName);
        if (destPrimaryWorld == null) {
            sendMessage(player, colorize("{RED}Invalid world."));
            return;
        }

        CurrentLocation cl = validateCurrentLocation(player, destPrimaryWorldName);
        if (cl == null) return;

        teleport(player, destPrimaryWorldName, destPrimaryWorld, cl);
    }

    public CurrentLocation validateCurrentLocation(Player player, String destPrimaryWorldName) {
        // Get current location
        Location currentLocation = player.getLocation();
        debug(plugin, "Player %s current location: %s", player.getName(), currentLocation);

        // Resolve primary world for current world
        String currentPrimaryWorldName = plugin.resolvePrimaryWorld(currentLocation.getWorld().getName());

        // Don't allow teleporting to the same world/group
        if (currentPrimaryWorldName.equals(destPrimaryWorldName)) {
            sendMessage(player, colorize("{RED}You are already there."));
            return null;
        }

        // Grab options
        GroupOptions options = plugin.getGroupOptions(currentPrimaryWorldName);

        return new CurrentLocation(currentPrimaryWorldName, currentLocation, options.getDelay());
    }

    public void teleport(Player player, String destPrimaryWorldName, World destPrimaryWorld, CurrentLocation cl) {
        // Save location
        if (!plugin.getBlacklist().contains(cl.getCurrentPrimaryWorldName()))
            plugin.getDao().saveLocation(player, cl.getCurrentPrimaryWorldName(), cl.getLocation());

        // Get destination location
        Location newLocation = null;
        if (!plugin.getBlacklist().contains(destPrimaryWorldName)) {
            // Load previous location, if any
            newLocation = plugin.getDao().loadLocation(player, destPrimaryWorldName);
            debug(plugin, "Player %s saved location: %s", player.getName(), newLocation);

            // Check if destination is safe
            if (newLocation != null && !checkDestination(newLocation)) {
                sendMessage(player, colorize("{YELLOW}Destination is unsafe; teleporting to spawn."));
                // NB: If this is a group, the player goes to the primary world's spawn
                newLocation = null;
            }
        }
        if (newLocation == null) {
            // Player is visiting a new place, teleport to spawn
            newLocation = destPrimaryWorld.getSpawnLocation();
            debug(plugin, "Player %s location defaulted to spawn", player.getName());
        }

        // Go there!
        player.teleport(newLocation);
    }

    private boolean isSolidBlock(Block block) {
        return ExcursionPlugin.solidBlocks.contains(block.getType());
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
        
        return !ExcursionPlugin.unsafeGround.contains(ground.getType());
    }

}
