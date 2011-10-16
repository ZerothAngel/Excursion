package org.tyrannyofheaven.bukkit.Excursion;

import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;

public class TeleportTask implements Runnable {

    private final ExcursionPlugin plugin;

    private final TeleportHelper teleportHelper;

    private final String playerName;
    
    private final String destPrimaryWorldName;

    public TeleportTask(ExcursionPlugin plugin, TeleportHelper teleportHelper, String playerName, String destPrimaryWorldName) {
        this.plugin = plugin;
        this.teleportHelper = teleportHelper;
        this.playerName = playerName;
        this.destPrimaryWorldName = destPrimaryWorldName;
    }

    @Override
    public void run() {
        ToHLoggingUtils.debug(plugin, "Executing delayed teleport for %s", playerName);
        // Clear saved task ID first
        plugin.clearTeleportTaskId(playerName);
        teleportHelper.teleport(playerName, destPrimaryWorldName);
    }

}
