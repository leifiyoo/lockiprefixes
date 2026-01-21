package de.locki.lockiprefixes.command;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command handler for /lockiprefixes reload
 * Uses Adventure API for messages.
 */
public class ReloadCommand implements CommandExecutor {

    private final LockiPrefixesPlugin plugin;

    public ReloadCommand(LockiPrefixesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("LockiPrefixes v" + plugin.getDescription().getVersion()).color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Usage: /lockiprefixes reload").color(NamedTextColor.GRAY));
            return true;
        }

        plugin.reload();
        sender.sendMessage(Component.text("LockiPrefixes configuration reloaded!").color(NamedTextColor.GREEN));
        return true;
    }
}
