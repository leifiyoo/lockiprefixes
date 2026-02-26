package de.locki.lockiprefixes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Intercepts chat messages when a player has an active prefix/suffix edit session.
 * Cancels the chat event and sends a live preview instead.
 */
@SuppressWarnings("deprecation")
public class PrefixChatInputListener implements Listener {

    private final PrefixMenuManager menuManager;

    public PrefixChatInputListener(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    /**
     * Fires on LOWEST priority so we capture it before any other plugin.
     * The standard AdventureChatListener fires on HIGHEST, and since we cancel
     * here on LOWEST, the AdventureChatListener will receive ignoreCancelled=true
     * and skip it automatically.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatEditSession session = menuManager.getSession(player.getUniqueId());
        if (session == null) return;

        // Cancel so the message is NOT broadcast
        event.setCancelled(true);

        String input = event.getMessage();

        // "cancel" keyword as text fallback (in case click event doesn't work)
        if (input.equalsIgnoreCase("cancel")) {
            menuManager.cancelSession(player);
            return;
        }

        // "accept" keyword as text fallback
        if (input.equalsIgnoreCase("accept")) {
            if (menuManager.isCreateRankMode(player.getUniqueId())) {
                player.sendMessage("§eNo /accept needed here. Type the rank key directly.");
                return;
            }
            menuManager.acceptSession(player);
            return;
        }

        // Update the live preview
        menuManager.updateSessionDraft(player, input);
    }
}
