package org.tyrannyofheaven.bukkit.Excursion;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class AvajeExcursionDao implements ExcursionDao {

    private final ExcursionPlugin plugin;
    
    AvajeExcursionDao(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Location loadLocation(Player player, String group) {
        plugin.getDatabase().beginTransaction();
        try {
            SavedLocationId id = new SavedLocationId(group, player.getName());
            SavedLocation sl = plugin.getDatabase().find(SavedLocation.class, id);
            if (sl != null) {
                World world = plugin.getServer().getWorld(sl.getWorld());
                if (world != null)
                    return new Location(world, sl.getX(), sl.getY(), sl.getZ(), sl.getYaw(), sl.getPitch());
                else {
                    // Hm, world is no longer there, delete this SavedLocation
                    plugin.getDatabase().delete(sl);
                    plugin.getDatabase().commitTransaction();
                }
            }
            return null;
        }
        finally {
            plugin.getDatabase().endTransaction();
        }
    }

    @Override
    public void saveLocation(Player player, String group, Location location) {
        plugin.getDatabase().beginTransaction();
        try {
            SavedLocationId id = new SavedLocationId(group, player.getName());
            SavedLocation sl = plugin.getDatabase().find(SavedLocation.class, id);
            if (sl == null) {
                // Never visited this world
                sl = new SavedLocation(group, location.getWorld().getName(), player.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            }
            else {
                // Update location in this world
                sl.setWorld(location.getWorld().getName());
                sl.setX(location.getX());
                sl.setY(location.getY());
                sl.setZ(location.getZ());
                sl.setYaw(location.getYaw());
                sl.setPitch(location.getPitch());
            }
            plugin.getDatabase().save(sl);
            plugin.getDatabase().commitTransaction();
        }
        finally {
            plugin.getDatabase().endTransaction();
        }
    }

}
