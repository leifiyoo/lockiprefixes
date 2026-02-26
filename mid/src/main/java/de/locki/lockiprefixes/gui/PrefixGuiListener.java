package de.locki.lockiprefixes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles Prefix Manager GUI inventory interactions (mid versions).
 */
public class PrefixGuiListener implements Listener {

    private final PrefixMenuManager menuManager;

    public PrefixGuiListener(PrefixMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();

        boolean isOurMenu = title.equals(PrefixMenuManager.MAIN_TITLE)
            || title.equals(PrefixMenuManager.LIST_TITLE);
        if (!isOurMenu) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (event.getSlot() < 0) return;

        menuManager.handleClick(player, inv, event.getSlot(), title);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        menuManager.onInventoryClosed(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menuManager.clearSession(event.getPlayer().getUniqueId());
    }
}
