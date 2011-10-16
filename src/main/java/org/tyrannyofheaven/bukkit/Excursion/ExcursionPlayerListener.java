package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.registerEvent;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ExcursionPlayerListener extends PlayerListener {

    private final ExcursionPlugin plugin;
    
    ExcursionPlayerListener(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        registerEvent("PLAYER_QUIT", this, Priority.Monitor, plugin);
        registerEvent("PLAYER_CHANGED_WORLD", this, Priority.Monitor, plugin);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        int taskId = plugin.clearTeleportTaskId(event.getPlayer().getName());
        if (taskId != -1) {
            debug(plugin, "Clearing teleport task for %s (%d)", event.getPlayer().getName(), taskId);
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        int taskId = plugin.clearTeleportTaskId(event.getPlayer().getName());
        if (taskId != -1) {
            debug(plugin, "Clearing teleport task for %s (%d)", event.getPlayer().getName(), taskId);
            plugin.getServer().getScheduler().cancelTask(taskId);
            sendMessage(event.getPlayer(), colorize("{GRAY}(Teleport cancelled due to world change)"));
        }
    }

}
