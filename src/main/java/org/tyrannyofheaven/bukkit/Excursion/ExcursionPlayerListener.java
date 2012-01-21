package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ExcursionPlayerListener implements Listener {

    private final ExcursionPlugin plugin;
    
    ExcursionPlayerListener(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(event=PlayerQuitEvent.class, priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.cancelTeleportTask(event.getPlayer().getName());
    }

    @EventHandler(event=PlayerChangedWorldEvent.class, priority=EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (plugin.cancelTeleportTask(event.getPlayer().getName())) {
            sendMessage(event.getPlayer(), colorize("{GRAY}(Teleport cancelled due to world change)"));
        }
    }

}
