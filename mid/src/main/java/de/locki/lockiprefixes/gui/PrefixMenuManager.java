package de.locki.lockiprefixes.gui;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.gui.ChatEditSession.EditType;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Prefix Manager GUI for mid-range MC versions (1.13-1.16).
 * Uses legacy ChatColor API and typed accept/cancel chat input.
 */
public class PrefixMenuManager {

    // ─── Menu titles ──────────────────────────────────────────────────────────
    public static final String MAIN_TITLE  = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "Prefix Manager" + ChatColor.DARK_GRAY + " ✦";
    public static final String LIST_TITLE  = ChatColor.DARK_GRAY + "✦ " + ChatColor.YELLOW + "All Prefixes" + ChatColor.DARK_GRAY + " ✦";

    // ─── Slot indices (45 slots) ──────────────────────────────────────────────
    public static final int SLOT_SKULL       = 13;
    public static final int SLOT_PREFIX_INFO = 20;
    public static final int SLOT_EDIT_PREFIX = 22;
    public static final int SLOT_LIST_PREFIX = 24;
    public static final int SLOT_CLOSE       = 40;

    // ─── State ───────────────────────────────────────────────────────────────
    private final LockiPrefixesPlugin plugin;
    private final LuckPermsFacade luckPermsFacade;
    private final Map<UUID, ChatEditSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> openMenus = ConcurrentHashMap.newKeySet();

    public PrefixMenuManager(LockiPrefixesPlugin plugin, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.luckPermsFacade = luckPermsFacade;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════════

    public void openMainMenu(Player player) {
        PlayerData data = plugin.createPlayerData(player);
        Inventory inv = buildMainMenu(player, data);
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void openPrefixListMenu(Player player) {
        // Fetch ALL available prefixes from groups
        List<String> allPrefixes = luckPermsFacade.getAllGroupPrefixes();
        Inventory inv = buildPrefixListMenu(allPrefixes);
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public boolean hasOpenMenu(UUID uuid) { return openMenus.contains(uuid); }
    public void onInventoryClosed(UUID uuid) { openMenus.remove(uuid); }

    // ═══════════════════════════════════════════════════════════════════════
    //  Session Management
    // ═══════════════════════════════════════════════════════════════════════

    public void startEditSession(Player player, EditType type) {
        if (type != EditType.PREFIX) return;

        PlayerData data = plugin.createPlayerData(player);
        String current = stripColor(data.getPrefix());
        if (current == null) current = "";
        
        sessions.put(player.getUniqueId(), new ChatEditSession(player.getUniqueId(), type, current));
        player.closeInventory();

        String typeName = "Prefix";
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "  ✦ " + ChatColor.YELLOW + "" + ChatColor.BOLD + typeName + " Editor");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GRAY + "  Current: " + ChatColor.translateAlternateColorCodes('&', current.isEmpty() ? ChatColor.DARK_GRAY + "(none)" : current));
        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "  Enter a new " + typeName.toLowerCase() + " in the chat.");
        player.sendMessage(ChatColor.GRAY + "  Colors: " + ChatColor.GREEN + "&0-&9, &a-&f" + ChatColor.GRAY + ".");
        player.sendMessage(ChatColor.GRAY + "  Variables: " + ChatColor.AQUA + "{user}" + ChatColor.GRAY + " (name), " + ChatColor.AQUA + "{message}" + ChatColor.GRAY + " (text)");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Preview updates automatically.");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Type " + ChatColor.GREEN + "" + ChatColor.BOLD + "accept "
            + ChatColor.GRAY + "to save, or " + ChatColor.RED + "" + ChatColor.BOLD + "cancel " + ChatColor.GRAY + "to discard.");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage("");
    }

    public ChatEditSession getSession(UUID uuid) {
        ChatEditSession s = sessions.get(uuid);
        if (s != null && s.isExpired()) { sessions.remove(uuid); return null; }
        return s;
    }

    public void updateSessionDraft(Player player, String input) {
        ChatEditSession session = getSession(player.getUniqueId());
        if (session == null) return;
        session.setDraft(input);

        String sampleName = player.getName();
        String sampleMsg = "Hello World!";
        
        // Preview logic
        String previewRaw = input
            .replace("{user}", sampleName)
            .replace("{message}", sampleMsg);

        if (!input.contains("{user}") && !input.contains("{message}")) {
            previewRaw = input + sampleName + "&8: &f" + sampleMsg;
        } else if (!input.contains("{message}")) {
            previewRaw = previewRaw + ": " + sampleMsg;
        }

        String rendered = ChatColor.translateAlternateColorCodes('&', previewRaw);

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GOLD + "  ✦ " + ChatColor.YELLOW + "Prefix Editor " + ChatColor.GRAY + "— Live Preview");
        player.sendMessage(ChatColor.GRAY + "  Input: " + ChatColor.translateAlternateColorCodes('&', input));
        player.sendMessage(ChatColor.GRAY + "  Result: " + rendered);
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Type '" + ChatColor.GREEN + "accept" + ChatColor.GRAY + "' or '" + ChatColor.RED + "cancel" + ChatColor.GRAY + "'.");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage("");
    }

    public void acceptSession(Player player) {
        ChatEditSession session = sessions.remove(player.getUniqueId());
        if (session == null) { player.sendMessage(ChatColor.RED + "No active session."); return; }
        
        final String value = session.getDraft();
        if (session.getEditType() == EditType.PREFIX) {
            luckPermsFacade.setPlayerMeta(player.getUniqueId(), "chat-format", "")
                .thenCompose(v -> luckPermsFacade.setPlayerMeta(player.getUniqueId(), "prefix", value))
                .thenRunAsync(() -> {
                    luckPermsFacade.invalidateCache(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "✔ Prefix updated: " + ChatColor.translateAlternateColorCodes('&', value));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 20L);
                });
        }
    }

    public void cancelSession(Player player) {
        ChatEditSession session = sessions.remove(player.getUniqueId());
        if (session == null) { player.sendMessage(ChatColor.RED + "No active session."); return; }
        player.sendMessage(ChatColor.RED + "✘ Edit cancelled.");
        Bukkit.getScheduler().runTask(plugin, () -> openMainMenu(player));
    }

    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
        openMenus.remove(uuid);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GUI Building (45 Slots)
    // ═══════════════════════════════════════════════════════════════════════

    private Inventory buildMainMenu(Player player, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, MAIN_TITLE);

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,37,38,39,41,42,43,44
        };
        for (int s : borderSlots) inv.setItem(s, border);

        String prefix = data.getPrefix();
        String displayPrefix = prefix != null ? prefix : "&8(none)";

        // Slot 13: Player Head
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + player.getName());
            List<String> skullLore = new ArrayList<>();
            skullLore.add("");
            skullLore.add(ChatColor.GRAY + "  Prefix: " + colorize(displayPrefix));
            skullLore.add("");
            skullMeta.setLore(skullLore);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(SLOT_SKULL, skull);

        // Slot 20: Prefix Info
        List<String> prefixLore = new ArrayList<>();
        prefixLore.add("");
        prefixLore.add(ChatColor.GRAY + "  Value: " + colorize(displayPrefix));
        prefixLore.add("");
        prefixLore.add(ChatColor.WHITE + "  Click " + ChatColor.GOLD + "Edit" + ChatColor.GRAY + " to change your prefix.");
        prefixLore.add("");
        inv.setItem(SLOT_PREFIX_INFO, makeGlowItem(Material.NAME_TAG, ChatColor.AQUA + "" + ChatColor.BOLD + "Info", prefixLore));

        // Slot 22: Edit Prefix
        List<String> editPfxLore = new ArrayList<>();
        editPfxLore.add("");
        editPfxLore.add(ChatColor.GRAY + "  Click to change your prefix");
        editPfxLore.add(ChatColor.GRAY + "  using chat.");
        editPfxLore.add("");
        editPfxLore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "  [Click to edit]");
        editPfxLore.add("");
        inv.setItem(SLOT_EDIT_PREFIX, makeGlowItem(Material.ANVIL, ChatColor.GOLD + "" + ChatColor.BOLD + "Edit Prefix", editPfxLore));

        // Slot 24: All Prefixes
        List<String> allPfxLore = new ArrayList<>();
        allPfxLore.add("");
        List<String> aps = luckPermsFacade.getAllGroupPrefixes();
        if (aps != null && !aps.isEmpty()) {
            allPfxLore.add(ChatColor.GRAY + "  Available: " + ChatColor.YELLOW + aps.size());
            int shown = Math.min(aps.size(), 8);
            for (int i = 0; i < shown; i++) allPfxLore.add(ChatColor.DARK_GRAY + "  • " + colorize(aps.get(i)));
            if (aps.size() > 8) allPfxLore.add(ChatColor.DARK_GRAY + "  ... + " + (aps.size() - 8) + " more");
        } else {
            allPfxLore.add(ChatColor.DARK_GRAY + "  No prefixes found.");
        }
        allPfxLore.add("");
        allPfxLore.add(ChatColor.YELLOW + "  [Click to view list]");
        allPfxLore.add("");
        inv.setItem(SLOT_LIST_PREFIX, makeItem(Material.BOOK, ChatColor.YELLOW + "" + ChatColor.BOLD + "All Prefixes", allPfxLore));

        // Slot 40: Close
        List<String> closeLore = new ArrayList<>();
        closeLore.add("");
        closeLore.add(ChatColor.GRAY + "  Click to close.");
        closeLore.add("");
        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Close", closeLore));

        return inv;
    }

    private Inventory buildPrefixListMenu(List<String> prefixes) {
        Inventory inv = Bukkit.createInventory(null, 45, LIST_TITLE);
        ItemStack bg = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < 45; i++) inv.setItem(i, bg);

        if (prefixes != null) {
            int slotIdx = 9;
            // Slots 9-35 are safe middle area
            for (int i = 0; i < prefixes.size() && slotIdx < 36; i++) {
                String p = prefixes.get(i);
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "  Preview: " + colorize(p));
                lore.add("");
                lore.add(ChatColor.GREEN + "  [Click to apply]");
                lore.add("");
                lore.add(ChatColor.BLACK + "ID: " + i); // Hidden ID

                inv.setItem(slotIdx++, makeItem(Material.NAME_TAG, ChatColor.WHITE + "Prefix #" + (i+1), lore));
            }
        }

        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "  Back to main menu.");
        backLore.add("");
        inv.setItem(40, makeItem(Material.ARROW, ChatColor.GREEN + "" + ChatColor.BOLD + "← Back", backLore));
        return inv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Click routing
    // ═══════════════════════════════════════════════════════════════════════

    public boolean handleClick(Player player, Inventory inv, int slot, String title) {
        if (title.equals(MAIN_TITLE)) {
            handleMainMenuClick(player, slot);
            return true;
        }
        if (title.equals(LIST_TITLE)) {
            handleListMenuClick(player, inv, slot);
            return true;
        }
        return false;
    }

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case SLOT_EDIT_PREFIX: startEditSession(player, EditType.PREFIX); break;
            case SLOT_LIST_PREFIX: openPrefixListMenu(player); break;
            case SLOT_CLOSE:       player.closeInventory(); break;
            default: break;
        }
    }

    private void handleListMenuClick(Player player, Inventory inv, int slot) {
        if (slot == 40) { openMainMenu(player); return; }

        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;

        // Find ID
        int idx = -1;
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "ID: ")) {
                try {
                    idx = Integer.parseInt(line.substring((ChatColor.BLACK + "ID: ").length()));
                } catch (NumberFormatException ignored) {}
                break;
            }
        }

        if (idx != -1) {
            List<String> allPrefixes = luckPermsFacade.getAllGroupPrefixes();
            if (idx < allPrefixes.size()) {
                String selected = allPrefixes.get(idx);
                luckPermsFacade.setPlayerMeta(player.getUniqueId(), "chat-format", "")
                    .thenCompose(v -> luckPermsFacade.setPlayerMeta(player.getUniqueId(), "prefix", selected))
                    .thenRunAsync(() -> {
                        luckPermsFacade.invalidateCache(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "✔ Prefix applied: " + colorize(selected));
                        Bukkit.getScheduler().runTask(plugin, () -> openMainMenu(player));
                    });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            try { meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS); } catch (Exception ignored) {}
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeGlowItem(Material mat, String name, List<String> lore) {
        ItemStack item = makeItem(mat, name, lore);
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try { meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); } catch (Exception ignored) {}
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
}
