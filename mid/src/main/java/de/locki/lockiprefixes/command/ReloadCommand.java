package de.locki.lockiprefixes.command;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.gui.PrefixMenuManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command handler for /lockiprefixes (mid versions).
 * Subcommands: reload | menu | accept | cancel
 */
public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final LockiPrefixesPlugin plugin;
    private PrefixMenuManager menuManager;

    public ReloadCommand(LockiPrefixesPlugin plugin) {
        this.plugin = plugin;
    }

    public void setMenuManager(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            handleReload(sender);
        } else if (sub.equals("menu")) {
            openMenu(sender);
        } else if (sub.equals("accept")) {
            handleAccept(sender);
        } else if (sub.equals("cancel")) {
            handleCancel(sender);
        } else {
            sendHelp(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(ChatColor.RED + "✘ You don't have permission to use this command.");
            return;
        }
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "✔ LockiPrefixes configuration reloaded!");
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "✘ This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("lockiprefixes.menu")) {
            player.sendMessage(ChatColor.RED + "✘ You don't have permission to use the Prefix Manager.");
            return;
        }
        if (menuManager == null) {
            player.sendMessage(ChatColor.RED + "✘ The Prefix Manager is not available (LuckPerms missing?).");
            return;
        }
        menuManager.openMainMenu(player);
    }

    private void handleAccept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "✘ This command can only be used by players.");
            return;
        }
        if (menuManager == null) return;
        menuManager.acceptSession((Player) sender);
    }

    private void handleCancel(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "✘ This command can only be used by players.");
            return;
        }
        if (menuManager == null) return;
        menuManager.cancelSession((Player) sender);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_GRAY + "━━━━ " + ChatColor.GOLD + "" + ChatColor.BOLD + "LockiPrefixes"
            + ChatColor.YELLOW + " v" + plugin.getDescription().getVersion() + ChatColor.DARK_GRAY + " ━━━━");
        if (sender.hasPermission("lockiprefixes.menu")) {
            sender.sendMessage(ChatColor.WHITE + "  /lpx menu   " + ChatColor.GRAY + "→ Open Rank Manager");
        }
        if (sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(ChatColor.WHITE + "  /lpx reload " + ChatColor.GRAY + "→ Reload config");
        }
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<String>(Arrays.asList("menu", "reload"));
            List<String> result = new ArrayList<String>();
            for (String o : opts) {
                if (o.startsWith(args[0].toLowerCase())) result.add(o);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
