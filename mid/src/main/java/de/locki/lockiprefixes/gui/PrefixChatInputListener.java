package de.locki.lockiprefixes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Intercepts chat messages during prefix/suffix edit sessions (mid versions).
 */
@SuppressWarnings("deprecation")
public class PrefixChatInputListener implements Listener {

    private final PrefixMenuManager menuManager;

    public PrefixChatInputListener(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatEditSession session = menuManager.getSession(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        String input = event.getMessage();

        if (input.equalsIgnoreCase("cancel")) {
            menuManager.cancelSession(player);
            return;
        }
        if (input.equalsIgnoreCase("accept")) {
            menuManager.acceptSession(player);
            return;
        }

        menuManager.updateSessionDraft(player, input);
    }
}
