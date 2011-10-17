package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.tyrannyofheaven.bukkit.util.ToHUtils;

public class ExcursionEntityListener extends EntityListener {

    private static final String DAMAGE_CANCEL_MSG = "{RED}Teleport cancelled due to damage!";

    private static final String COMBAT_CANCEL_MSG = "{RED}Teleport cancelled due to combat!";

    private final ExcursionPlugin plugin;

    public ExcursionEntityListener(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }
    
    void registerEvents() {
        ToHUtils.registerEvent("ENTITY_DAMAGE", this, Priority.Monitor, plugin);
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;

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
