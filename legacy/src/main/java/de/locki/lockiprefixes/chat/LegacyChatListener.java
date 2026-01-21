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
 * Chat listener for Legacy versions (1.7-1.12).
 * Uses AsyncPlayerChatEvent and legacy color codes.
 */
public class LegacyChatListener implements Listener {

    private final LockiPrefixesPlugin plugin;
    private final ChatFormatter chatFormatter;
    private final LuckPermsFacade luckPermsFacade;

    public LegacyChatListener(LockiPrefixesPlugin plugin, ChatFormatter chatFormatter, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.chatFormatter = chatFormatter;
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

        // Format the message
        String formatted = chatFormatter.formatChat(playerData, event.getMessage());

        // Set the format - use %1$s for name and %2$s for message
        // Since we already formatted everything, we use empty placeholders
        event.setFormat(formatted.replace("%", "%%"));
    }
}
