package org.tyrannyofheaven.bukkit.Excursion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class ExcursionPlugin extends JavaPlugin {

    private final Logger logger = Logger.getLogger("Minecraft");

    private PluginDescriptionFile pdf;

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
        pdf = getDescription();
        log("Starting up...");
        
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        // Create tables if they don't already exist
        try {
            // Check if the main table exists
            getDatabase().createQuery(SavedLocation.class).findRowCount();
        }
        catch (PersistenceException e) {
            log("Creating SQL tables...");
            installDDL();
        }

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

        // Check world access
        if (!player.hasPermission("excursion.access.*") && !player.hasPermission("excursion.access." + world.getName())) {
            player.sendMessage(ChatColor.RED + "You need one of the following permissions to do this:");
            player.sendMessage(ChatColor.GREEN + "- excursion.access.*");
            player.sendMessage(ChatColor.GREEN + "- excursion.access." + world.getName());
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
        logger.info(String.format("[%s] %s", pdf.getName(), String.format(format, args)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player)sender;

        // Check permission
        if (!player.hasPermission("excursion.visit")) {
            player.sendMessage(ChatColor.RED + "You need the following permission to do this:");
            player.sendMessage(ChatColor.GREEN + "- excursion.visit");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        return visit(player, args[0]);
    }

}
