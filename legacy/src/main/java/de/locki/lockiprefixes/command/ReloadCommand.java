package de.locki.lockiprefixes.command;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command handler for /lockiprefixes reload
 */
public class ReloadCommand implements CommandExecutor {

    private final LockiPrefixesPlugin plugin;

    public ReloadCommand(LockiPrefixesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "LockiPrefixes v" + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Usage: /lockiprefixes reload");
            return true;
        }

        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "LockiPrefixes configuration reloaded!");
        return true;
    }
}
