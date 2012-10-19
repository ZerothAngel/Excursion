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
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requireOnePermission;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class ExcursionCommand {

    private final ExcursionPlugin plugin;
    
    private final SubCommand subCommand;

    private final TeleportHelper teleportHelper;

    ExcursionCommand(ExcursionPlugin plugin) {
        this.plugin = plugin;
        subCommand = new SubCommand(plugin);
        teleportHelper = new TeleportHelper(plugin);
    }

    @Command(value="visit", description="Visit the specified world")
    @Require("excursion.visit")
    public void visit(CommandSender sender, @Option(value="world", completer="destination") String group) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("{RED}Only usable by players!"));
            return;
        }

        Player player = (Player)sender;

        // Resolve destination world
        String alias = plugin.getAliasMap().get(group);
        if (alias != null)
            group = alias;

        // Group members map to their primary world
        String destPrimaryWorldName = plugin.getGroupMap().get(group);
        if (destPrimaryWorldName == null)
            destPrimaryWorldName = group; // not in a group or is a primary world
            
        // Check if world exists
        World destPrimaryWorld = plugin.getServer().getWorld(destPrimaryWorldName);
        if (destPrimaryWorld == null) {
            sendMessage(player, colorize("{RED}Invalid world."));
            return;
        }

        // Check world access
        // (group access based on primary world)
        requireOnePermission(player, "excursion.access.*", String.format("excursion.access.%s", destPrimaryWorldName));

        CurrentLocation cl = teleportHelper.validateCurrentLocation(player, destPrimaryWorldName);
        if (cl == null) return;

        // Clear any previously-scheduled teleport task for this player
        int taskId = plugin.clearTeleportTaskId(player.getName());
        if (taskId != -1) {
            debug(plugin, "Clearing previous teleport task for %s (%d)", player.getName(), taskId);
            plugin.getServer().getScheduler().cancelTask(taskId);
        }

        if (cl.getDelay() < 1) {
            // Teleport immediately
            teleportHelper.teleport(player, destPrimaryWorldName, destPrimaryWorld, cl);
        }
        else {
            // Set up delayed teleport
            taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new TeleportTask(plugin, teleportHelper, player.getName(), destPrimaryWorldName), cl.getDelay() * ToHUtils.TICKS_PER_SECOND);
            if (taskId == -1) {
                error(plugin, "Failed to schedule teleport!");
                sendMessage(player, colorize("{RED}Server error; failed to schedule teleport."));
                return;
            }

            // Save task ID
            plugin.setTeleportTaskId(player, taskId);

            debug(plugin, "Scheduled teleport task for %s (%d)", player.getName(), taskId);
            sendMessage(player, colorize("{GRAY}(You will teleport in %d second%s)"), cl.getDelay(), cl.getDelay() == 1 ? "" : "s");
        }
    }

    @Command("excursion")
    @Require("excursion.reload")
    public SubCommand excursion(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(subCommand)
                .forCommand("reload")
                .show();
            return null;
        }
        
        return subCommand;
    }

}
