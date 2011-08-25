package org.tyrannyofheaven.bukkit.Excursion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ExcursionPlugin extends JavaPlugin {

    private final Logger logger = Logger.getLogger("Minecraft");

    private ExcursionDao dao;

    ExcursionDao getDao() {
        return dao;
    }

    private void setDao(ExcursionDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(SavedLocationId.class);
        result.add(SavedLocation.class);
        return result;
    }

    @Override
    public void onDisable() {
        log("Shutting down...");
    }

    @Override
    public void onEnable() {
        log("Starting up...");

        if (!getDataFolder().exists())
            getDataFolder().mkdirs();
        if (!new File(getDataFolder(), "Excursion.db").exists())
            installDDL();

        setDao(new AvajeExcursionDao(this));
        getCommand("visit").setExecutor(this);
    }

    boolean visit(Player player, String worldName) {
        // Resolve destination world
        World world = getServer().getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Invalid world.");
            return true;
        }

        // Get location
        Location currentLocation = player.getLocation();
        if (currentLocation.getWorld() == world) {
            player.sendMessage(ChatColor.RED + "You are already there.");
            return true;
        }
        
        // Save location
        getDao().saveLocation(player, currentLocation);

        // Create destination location
        Location newLocation = getDao().loadLocation(player, world);
        if (newLocation == null) {
            // Player is visiting a new place, teleport to spawn
            newLocation = world.getSpawnLocation();
        }

        // Go there!
        player.teleport(newLocation);
        return true;
    }

    void log(String format, Object... args) {
        logger.info("[Excursion] " + String.format(format, args));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player)sender;

        if (args.length != 1) {
            return false;
        }

        return visit(player, args[0]);
    }

}
