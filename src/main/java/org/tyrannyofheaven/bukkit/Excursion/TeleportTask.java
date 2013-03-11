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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            // Clear saved task ID first
            plugin.clearTeleportTaskId(player);
            teleportHelper.teleport(player, destPrimaryWorldName);
        }
    }

}
