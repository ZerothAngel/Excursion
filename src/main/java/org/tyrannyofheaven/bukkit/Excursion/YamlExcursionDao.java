package org.tyrannyofheaven.bukkit.Excursion;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

public class YamlExcursionDao implements ExcursionDao {

    private final File dbFile;
    
    YamlExcursionDao(File dir) {
        dbFile = new File(dir, "config.yml");
    }

    private Configuration load() {
        Configuration db = new Configuration(dbFile);
        db.load();
        return db;
    }

    @Override
    public synchronized Location loadLocation(Player player, World world) {
        Configuration db = load();
        String prefix = String.format("%s.%s", player.getName(), world.getName());
        Double x = (Double)db.getProperty(prefix + ".x");
        Double y = (Double)db.getProperty(prefix + ".y");
        Double z = (Double)db.getProperty(prefix + ".z");
        Double yaw = (Double)db.getProperty(prefix + ".yaw");
        Double pitch = (Double)db.getProperty(prefix + ".pitch");
        if (x != null && y != null && z != null && yaw != null && pitch != null) {
            return new Location(world, x, y, z, yaw.floatValue(), pitch.floatValue());
        }
        return null;
    }

    @Override
    public synchronized void saveLocation(Player player, Location location) {
        Configuration db = load();
        String prefix = String.format("%s.%s", player.getName(), location.getWorld().getName());
        db.setProperty(prefix + ".x", location.getX());
        db.setProperty(prefix + ".y", location.getY());
        db.setProperty(prefix + ".z", location.getZ());
        db.setProperty(prefix + ".yaw", location.getYaw());
        db.setProperty(prefix + ".pitch", location.getPitch());
        db.save();
    }

}
