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

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class ExcursionEntityListener implements Listener {

    private static final String DAMAGE_CANCEL_MSG = "{RED}Teleport cancelled due to damage!";

    private static final String COMBAT_CANCEL_MSG = "{RED}Teleport cancelled due to combat!";

    private final ExcursionPlugin plugin;

    public ExcursionEntityListener(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }
    
    void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Resolve group of victim's world
        String primaryWorldName = plugin.resolvePrimaryWorld(event.getEntity().getWorld().getName());
        GroupOptions options = plugin.getGroupOptions(primaryWorldName);

        // Cancel victim's teleport, if needed
        if (options.isCancelOnDamage()) {
            if (event.getEntity() instanceof Player) {
                Player victim = (Player)event.getEntity();
                if (plugin.cancelTeleportTask(victim.getName())) {
                    sendMessage(victim, colorize(DAMAGE_CANCEL_MSG));
                }
            }
        }

        // Cancel attacker's teleport, if needed
        if (options.isCancelOnAttack()) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
                if (e.getDamager() instanceof Player) {
                    Player attacker = (Player)e.getDamager();
                    if (plugin.cancelTeleportTask(attacker.getName())) {
                        sendMessage(attacker, colorize(COMBAT_CANCEL_MSG));
                    }
                }
                // Also check for projectiles
                else if (e.getDamager() instanceof Projectile) {
                    Projectile p = (Projectile)e.getDamager();
                    if (p.getShooter() instanceof Player) {
                        Player shooter = (Player)p.getShooter();
                        if (plugin.cancelTeleportTask(shooter.getName())) {
                            sendMessage(shooter, colorize(COMBAT_CANCEL_MSG));
                        }
                    }
                }
            }
        }
    }

}
