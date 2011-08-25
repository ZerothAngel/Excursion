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
    public Location loadLocation(Player player, World world) {
        SavedLocationId id = new SavedLocationId(world.getName(), player.getName());
        SavedLocation sl = plugin.getDatabase().find(SavedLocation.class, id);
        if (sl != null) {
            return new Location(world, sl.getX(), sl.getY(), sl.getZ(), sl.getYaw(), sl.getPitch());
        }
        return null;
    }

    @Override
    public void saveLocation(Player player, Location location) {
        boolean success = false;
        plugin.getDatabase().beginTransaction();
        try {
            SavedLocationId id = new SavedLocationId(location.getWorld().getName(), player.getName());
            SavedLocation sl = plugin.getDatabase().find(SavedLocation.class, id);
            if (sl == null) {
                // Never visited this world
                sl = new SavedLocation(location.getWorld().getName(), player.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            }
            else {
                // Update location in this world
                sl.setX(location.getX());
                sl.setY(location.getY());
                sl.setZ(location.getZ());
                sl.setYaw(location.getYaw());
                sl.setPitch(location.getPitch());
            }
            plugin.getDatabase().save(sl);
            success = true;
        }
        finally {
            if (success)
                plugin.getDatabase().commitTransaction();
            else
                plugin.getDatabase().rollbackTransaction();
        }
    }

}
