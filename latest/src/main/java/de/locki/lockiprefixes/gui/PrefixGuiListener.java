package de.locki.lockiprefixes.gui;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles inventory interactions for the Prefix Manager GUI (latest 1.20-1.21).
 */
public class PrefixGuiListener implements Listener {

    private final PrefixMenuManager menuManager;

    private static final LegacyComponentSerializer SECTION =
        LegacyComponentSerializer.legacySection();

    public PrefixGuiListener(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = SECTION.serialize(player.getOpenInventory().title());

        boolean isOurMenu = title.equals(PrefixMenuManager.MENU_TITLE)
            || title.equals(PrefixMenuManager.LIST_TITLE)
            || title.equals(PrefixMenuManager.TEMPLATE_TITLE)
            || title.equals(PrefixMenuManager.REVIEW_TITLE);
        if (!isOurMenu) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (event.getSlot() < 0) return;

        Inventory inv = event.getInventory();
        menuManager.handleClick(player, inv, event.getSlot(), title);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        menuManager.onInventoryClosed(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menuManager.clearSession(event.getPlayer().getUniqueId());
    }
}
