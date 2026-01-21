package de.locki.lockiprefixes.chat;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Chat listener for Latest versions (1.20-1.21).
 * Uses legacy AsyncPlayerChatEvent to avoid secure chat signature issues.
 */
@SuppressWarnings("deprecation")
public class AdventureChatListener implements Listener {

    private final LockiPrefixesPlugin plugin;
    private final ChatFormatter chatFormatter;
    private final LuckPermsFacade luckPermsFacade;
    
    // Legacy serializer with hex support
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public AdventureChatListener(LockiPrefixesPlugin plugin, ChatFormatter chatFormatter, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.chatFormatter = chatFormatter;
        this.luckPermsFacade = luckPermsFacade;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        // Skip if formatter is not available (LuckPerms not installed)
        if (chatFormatter == null || luckPermsFacade == null) {
            return;
        }
        
        Player player = event.getPlayer();

        // Build player data
        PlayerData playerData = new PlayerData();
        playerData.setUuid(player.getUniqueId());
        playerData.setName(player.getName());
        playerData.setDisplayName(PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        playerData.setWorld(player.getWorld().getName());

        // Populate LuckPerms data
        luckPermsFacade.populatePlayerData(playerData);

        // Format the message using our formatter
        String formatted = chatFormatter.formatChat(playerData, event.getMessage());

        // Cancel original event
        event.setCancelled(true);

        // Convert to Adventure Component and broadcast
        Component formattedComponent = LEGACY_SERIALIZER.deserialize(formatted);
        Bukkit.getServer().sendMessage(formattedComponent);
        
        // Log to console
        plugin.getLogger().info(PlainTextComponentSerializer.plainText().serialize(formattedComponent));
    }
}
