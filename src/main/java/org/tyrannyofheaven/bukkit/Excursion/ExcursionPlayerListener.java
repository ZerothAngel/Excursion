package org.tyrannyofheaven.bukkit.Excursion;

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
        plugin.cancelTeleportTask(event.getPlayer().getName());
    }

    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (plugin.cancelTeleportTask(event.getPlayer().getName())) {
            sendMessage(event.getPlayer(), colorize("{GRAY}(Teleport cancelled due to world change)"));
        }
    }

}
