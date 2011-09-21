package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requireOnePermission;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class ExcursionCommand {

    private final ExcursionPlugin plugin;
    
    private final SubCommand subCommand;

    ExcursionCommand(ExcursionPlugin plugin) {
        this.plugin = plugin;
        subCommand = new SubCommand(plugin);
    }

    @Command(value="visit", description="Visit the specified world")
    @Require("excursion.visit")
    public void visit(CommandSender sender, @Option("world") String group) {
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
        String primaryWorld = plugin.getGroupMap().get(group);
        if (primaryWorld == null)
            primaryWorld = group; // not in a group or is a primary world
            
        // Check if world exists
        World world = plugin.getServer().getWorld(primaryWorld);
        if (world == null) {
            sendMessage(player, colorize("{RED}Invalid world."));
            return;
        }

        // Check world access
        // (group access based on primary world)
        requireOnePermission(player, "excursion.access.*", String.format("excursion.access.%s", primaryWorld));

        // Get current location
        Location currentLocation = player.getLocation();

        // Resolve primary world for current world
        String currentWorld = currentLocation.getWorld().getName();
        String currentPrimaryWorld = plugin.getGroupMap().get(currentWorld);
        if (currentPrimaryWorld == null)
            currentPrimaryWorld = currentWorld;

        if (currentPrimaryWorld.equals(primaryWorld)) {
            sendMessage(player, colorize("{RED}You are already there."));
            return;
        }

        // Save location
        if (!plugin.getBlacklist().contains(currentPrimaryWorld))
            plugin.getDao().saveLocation(player, currentPrimaryWorld, currentLocation);

        // Get destination location
        Location newLocation = null;
        if (!plugin.getBlacklist().contains(primaryWorld)) {
            // Load previous location, if any
            newLocation = plugin.getDao().loadLocation(player, primaryWorld);

            // Check if destination is safe
            if (newLocation != null && !checkDestination(newLocation)) {
                sendMessage(player, colorize("{YELLOW}Destination is unsafe; teleporting to spawn."));
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
