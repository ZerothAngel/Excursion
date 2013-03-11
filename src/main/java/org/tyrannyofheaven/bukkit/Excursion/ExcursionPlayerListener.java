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

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.cancelTeleportTask(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (plugin.cancelTeleportTask(event.getPlayer())) {
            sendMessage(event.getPlayer(), colorize("{GRAY}(Teleport cancelled due to world change)"));
        }
    }

}
