package org.tyrannyofheaven.bukkit.Excursion;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class SubCommand {

    private final ExcursionPlugin plugin;
    
    SubCommand(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(value="reload", description="Re-read config.yml")
    @Require("excursion.reload")
    public void reload(CommandSender sender) {
        plugin.reload();
        sendMessage(sender, colorize("{WHITE}config.yml{YELLOW} reloaded"));
    }

}
