package de.locki.lockiprefixes.chat;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Chat listener for Mid versions (1.13-1.16).
 * Uses AsyncPlayerChatEvent with hex color support for 1.16+.
 */
public class MidChatListener implements Listener {

    private final LockiPrefixesPlugin plugin;
    private final LuckPermsFacade luckPermsFacade;

    public MidChatListener(LockiPrefixesPlugin plugin, ChatFormatter chatFormatter, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        // chatFormatter parameter kept for API compatibility — we resolve it via plugin.getChatFormatter() per event.
        this.luckPermsFacade = luckPermsFacade;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Build player data
        PlayerData playerData = new PlayerData();
        playerData.setUuid(player.getUniqueId());
        playerData.setName(player.getName());
        playerData.setDisplayName(player.getDisplayName());
        playerData.setWorld(player.getWorld().getName());

        // Populate LuckPerms data
        luckPermsFacade.populatePlayerData(playerData);

        // Always resolve the formatter via the plugin so post-reload config is applied.
        ChatFormatter formatter = plugin.getChatFormatter();
        if (formatter == null) return;

        // Format the message
        String formatted = formatter.formatChat(playerData, event.getMessage());

        // Set the format - escape % characters
        event.setFormat(formatted.replace("%", "%%"));
    }
}
