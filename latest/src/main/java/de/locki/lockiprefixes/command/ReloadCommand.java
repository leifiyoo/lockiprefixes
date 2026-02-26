package de.locki.lockiprefixes.command;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.gui.PrefixMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /lockiprefixes.
 * Subcommands: reload | menu | accept | cancel
 */
public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final LockiPrefixesPlugin plugin;
    private PrefixMenuManager menuManager;

    public ReloadCommand(LockiPrefixesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Inject the menu manager after it is created. */
    public void setMenuManager(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /lockiprefixes
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "menu":
                openMenu(sender);
                break;
            // Internal: used by clickable chat buttons only (not shown in help/tab-complete)
            case "accept":
                handleAccept(sender);
                break;
            case "cancel":
                handleCancel(sender);
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    // -------------------------------------------------------------------------

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(Component.text("✘ You don't have permission to use this command.", NamedTextColor.RED));
            return;
        }
        plugin.reload();
        sender.sendMessage(Component.text("✔ LockiPrefixes configuration reloaded!", NamedTextColor.GREEN));
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✘ This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("lockiprefixes.menu")) {
            player.sendMessage(Component.text("✘ You don't have permission to use the Prefix Manager.", NamedTextColor.RED));
            return;
        }
        if (menuManager == null) {
            player.sendMessage(Component.text("✘ The Prefix Manager is not available (LuckPerms missing?).", NamedTextColor.RED));
            return;
        }
        menuManager.openMainMenu(player);
    }

    private void handleAccept(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✘ This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (menuManager == null) {
            player.sendMessage(Component.text("✘ The Prefix Manager is not available.", NamedTextColor.RED));
            return;
        }
        menuManager.acceptSession(player);
    }

    private void handleCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✘ This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (menuManager == null) {
            player.sendMessage(Component.text("✘ The Prefix Manager is not available.", NamedTextColor.RED));
            return;
        }
        menuManager.cancelSession(player);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━ ", NamedTextColor.DARK_GRAY)
            .append(Component.text("LockiPrefixes", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(" v" + plugin.getDescription().getVersion(), NamedTextColor.YELLOW))
            .append(Component.text(" ━━━━", NamedTextColor.DARK_GRAY)));
        if (sender.hasPermission("lockiprefixes.menu")) {
            sender.sendMessage(Component.text("  /lockiprefixes menu   ", NamedTextColor.WHITE)
                .append(Component.text("→ Open Prefix Manager", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("lockiprefixes.reload")) {
            sender.sendMessage(Component.text("  /lockiprefixes reload ", NamedTextColor.WHITE)
                .append(Component.text("→ Reload configuration", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("lockiprefixes.menu"))   options.add("menu");
            if (sender.hasPermission("lockiprefixes.reload")) options.add("reload");
            return options.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
