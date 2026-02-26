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
import java.util.regex.Pattern;

/**
 * Manages the Prefix Manager GUI for legacy MC versions (1.7-1.12).
 * Uses 1.8-compatible materials and legacy ChatColor API.
 */
@SuppressWarnings({"deprecation", "ConstantConditions", "unused"})
public class PrefixMenuManager {

    // ─── Menu titles ──────────────────────────────────────────────────────────
    public static final String MAIN_TITLE = ChatColor.DARK_GRAY + ">> " + ChatColor.GOLD + "Prefix Manager" + ChatColor.DARK_GRAY + " <<";
    public static final String LIST_TITLE = ChatColor.DARK_GRAY + ">> " + ChatColor.YELLOW + "All Prefixes" + ChatColor.DARK_GRAY + " <<";

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
        // Fetch ALL available prefixes from groups, not just user's
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
        // Only PREFIX supported now
        if (type != EditType.PREFIX) return;

        PlayerData data = plugin.createPlayerData(player);
        String current = stripColor(data.getPrefix());
        if (current == null) current = "";
        
        sessions.put(player.getUniqueId(), new ChatEditSession(player.getUniqueId(), type, current));
        player.closeInventory();

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "  * " + ChatColor.YELLOW + "" + ChatColor.BOLD + "Prefix Editor");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GRAY + "  Current: " + ChatColor.translateAlternateColorCodes('&', current.isEmpty() ? ChatColor.DARK_GRAY + "(none)" : current));
        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "  Type in chat " + ChatColor.GRAY + "to set a new prefix.");
        player.sendMessage(ChatColor.GRAY + "  Supports: " + ChatColor.GREEN + "&0-&9, &a-&f color codes" + ChatColor.GRAY + ".");
        player.sendMessage(ChatColor.GRAY + "  Variables: " + ChatColor.AQUA + "{user}" + ChatColor.GRAY + " (name), " + ChatColor.AQUA + "{message}" + ChatColor.GRAY + " (text)");
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "  -> Each message updates the live preview.");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Type " + ChatColor.GREEN + "" + ChatColor.BOLD + "accept "
            + ChatColor.GRAY + "to apply, or " + ChatColor.RED + "" + ChatColor.BOLD + "cancel " + ChatColor.GRAY + "to discard.");
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
        
        // Replace variables for preview
        String previewRaw = input
            .replace("{user}", sampleName)
            .replace("{message}", sampleMsg);

        // If no variables used, default format: [Prefix] Name: Message
        if (!input.contains("{user}") && !input.contains("{message}")) {
            previewRaw = input + sampleName + "&8: &f" + sampleMsg;
        } else if (!input.contains("{message}")) {
            // Has user but no message
            previewRaw = previewRaw + ": " + sampleMsg;
        }

        String rendered = ChatColor.translateAlternateColorCodes('&', previewRaw);

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage(ChatColor.GOLD + "  * " + ChatColor.YELLOW + "Prefix Editor " + ChatColor.GRAY + "— Preview");
        player.sendMessage(ChatColor.GRAY + "  New Prefix: " + ChatColor.translateAlternateColorCodes('&', input));
        player.sendMessage(ChatColor.GRAY + "  Chat Preview: " + rendered);
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Type " + ChatColor.GREEN + "" + ChatColor.BOLD + "accept "
            + ChatColor.GRAY + "to apply, or " + ChatColor.RED + "" + ChatColor.BOLD + "cancel " + ChatColor.GRAY + "to discard.");
        player.sendMessage(ChatColor.DARK_GRAY + "======================================");
        player.sendMessage("");
    }

    public void acceptSession(Player player) {
        ChatEditSession session = sessions.remove(player.getUniqueId());
        if (session == null) { player.sendMessage(ChatColor.RED + "No active edit session."); return; }
        
        final String value = session.getDraft();
        
        // Always prefix
        if (session.getEditType() == EditType.PREFIX) {
            luckPermsFacade.setPlayerMeta(player.getUniqueId(), "chat-format", "")
                .thenCompose(v -> luckPermsFacade.setPlayerMeta(player.getUniqueId(), "prefix", value))
                .thenRunAsync(new Runnable() {
                    @Override public void run() {
                        luckPermsFacade.invalidateCache(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "* Prefix set to: " + ChatColor.translateAlternateColorCodes('&', value));
                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override public void run() { openMainMenu(player); }
                        }, 20L);
                    }
                }, new java.util.concurrent.Executor() {
                    @Override public void execute(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }
                });
        }
    }

    public void cancelSession(Player player) {
        ChatEditSession session = sessions.remove(player.getUniqueId());
        if (session == null) { player.sendMessage(ChatColor.RED + "No active edit session."); return; }
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "X Edit cancelled. No changes were made.");
        player.sendMessage("");
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() { openMainMenu(player); }
        });
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

        // Border pane: Black stained glass pane (1.8 id 15)
        ItemStack border = makeColoredPane((short) 15);
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,          // Row 1
            9,17,                       // Row 2 sides
            18,26,                      // Row 3 sides
            27,35,                      // Row 4 sides
            36,37,38,39,41,42,43,44     // Row 5 (skip 40)
        };
        for (int s : borderSlots) inv.setItem(s, border);

        String prefix = data.getPrefix();
        String dp = prefix != null ? prefix : "&8(none)";

        // Slot 13: Player skull
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwner(player.getName());
            skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + player.getName());
            List<String> sl = new ArrayList<>();
            sl.add("");
            sl.add(ChatColor.GRAY + "  Prefix: " + colorize(dp));
            sl.add("");
            skullMeta.setLore(sl);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(SLOT_SKULL, skull);

        // Slot 20: Info
        List<String> pl = new ArrayList<>();
        pl.add(""); pl.add(ChatColor.GRAY + "  Current: " + colorize(dp)); pl.add("");
        pl.add(ChatColor.WHITE + "  Click Edit Prefix to change."); pl.add("");
        inv.setItem(SLOT_PREFIX_INFO, makeGlowItem(Material.NAME_TAG, ChatColor.AQUA + "" + ChatColor.BOLD + "Info", pl));

        // Slot 22: Edit Prefix
        List<String> epl = new ArrayList<>();
        epl.add("");
        epl.add(ChatColor.GRAY + "  Click to change your prefix");
        epl.add(ChatColor.GRAY + "  via chat input.");
        epl.add("");
        epl.add(ChatColor.GOLD + "" + ChatColor.BOLD + "  [Click to edit]"); epl.add("");
        inv.setItem(SLOT_EDIT_PREFIX, makeGlowItem(Material.ANVIL, ChatColor.GOLD + "" + ChatColor.BOLD + "Edit Prefix", epl));

        // Slot 24: All Prefixes
        List<String> apl = new ArrayList<>();
        apl.add("");
        List<String> aps = luckPermsFacade.getAllGroupPrefixes(); // Use ALL group prefixes
        if (aps != null && !aps.isEmpty()) {
            apl.add(ChatColor.GRAY + "  Available Prefixes: " + ChatColor.YELLOW + aps.size());
            int shown = Math.min(aps.size(), 8);
            for (int i = 0; i < shown; i++) apl.add(ChatColor.DARK_GRAY + "  * " + colorize(aps.get(i)));
            if (aps.size() > 8) apl.add(ChatColor.DARK_GRAY + "  ... and " + (aps.size() - 8) + " more");
        } else { apl.add(ChatColor.DARK_GRAY + "  No prefixes found."); }
        apl.add(""); apl.add(ChatColor.YELLOW + "  [Click to view list]"); apl.add("");
        inv.setItem(SLOT_LIST_PREFIX, makeItem(Material.BOOK, ChatColor.YELLOW + "" + ChatColor.BOLD + "All Prefixes", apl));

        // Slot 40: Close
        List<String> cl = new ArrayList<>();
        cl.add(""); cl.add(ChatColor.GRAY + "  Click to close."); cl.add("");
        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Close", cl));

        return inv;
    }

    private Inventory buildPrefixListMenu(List<String> prefixes) {
        Inventory inv = Bukkit.createInventory(null, 45, LIST_TITLE); // 45 slots for list too
        ItemStack bg = makeColoredPane((short) 7); // gray
        for (int i = 0; i < 45; i++) inv.setItem(i, bg);

        if (prefixes != null) {
            // Center slots for 3 rows of content (9x3 = 27)
            int[] contentSlots = {
                10,11,12,13,14,15,16, // Row 2 partial (7)
                19,20,21,22,23,24,25, // Row 3 partial (7)
                28,29,30,31,32,33,34  // Row 4 partial (7) 
            };
            // Actually let's use full rows 2,3,4? 
            // 9,10...17
            // 18...26
            // 27...35
            // But usually we want some padding.
            // Let's stick to the list provided logic but ensure it doesn't overflow
            
            // Re-using same slot pattern logic but for 45 inv
            // Slots 0-8 border top
            // Slots 36-44 border bottom (except 40)
            
            // Content area: 9-35
            int slotIdx = 9;
            for (int i = 0; i < prefixes.size() && slotIdx < 36; i++) {
                String p = prefixes.get(i);
                List<String> lore = new ArrayList<>();
                lore.add(""); 
                lore.add(ChatColor.GRAY + "  Preview: " + colorize(p));
                lore.add(""); 
                lore.add(ChatColor.GREEN + "  [Click to apply]"); 
                lore.add("");
                lore.add(ChatColor.BLACK + "ID: " + i); // Hidden-ish ID
                
                inv.setItem(slotIdx++, makeItem(Material.NAME_TAG, ChatColor.WHITE + "Prefix #" + (i+1), lore));
            }
        }

        List<String> backLore = new ArrayList<>();
        backLore.add(""); backLore.add(ChatColor.GRAY + "  Return to main menu."); backLore.add("");
        inv.setItem(40, makeItem(Material.ARROW, ChatColor.GREEN + "" + ChatColor.BOLD + "<- Back", backLore));
        return inv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Click Routing
    // ═══════════════════════════════════════════════════════════════════════

    public boolean handleClick(Player player, Inventory inv, int slot, String title) {
        if (title.equals(MAIN_TITLE)) { 
            handleMainClick(player, slot); 
            return true; 
        }
        if (title.equals(LIST_TITLE)) { 
            handleListClick(player, inv, slot); 
            return true; 
        }
        return false;
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case SLOT_EDIT_PREFIX: startEditSession(player, EditType.PREFIX); break;
            case SLOT_LIST_PREFIX: openPrefixListMenu(player); break;
            case SLOT_CLOSE:       player.closeInventory(); break;
            default: break;
        }
    }

    private void handleListClick(Player player, Inventory inv, int slot) {
        if (slot == 40) { openMainMenu(player); return; }
        
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;
        
        // Find ID
        int prefixIndex = -1;
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "ID: ")) {
                try {
                    prefixIndex = Integer.parseInt(line.substring((ChatColor.BLACK + "ID: ").length()));
                } catch (NumberFormatException ignored) {}
                break;
            }
        }
        
        if (prefixIndex != -1) {
            final int idx = prefixIndex;
            List<String> allPrefixes = luckPermsFacade.getAllGroupPrefixes();
            if (idx < allPrefixes.size()) {
                final String selectedPrefix = allPrefixes.get(idx);
                luckPermsFacade.setPlayerMeta(player.getUniqueId(), "chat-format", "")
                    .thenCompose(v -> luckPermsFacade.setPlayerMeta(player.getUniqueId(), "prefix", selectedPrefix))
                    .thenRunAsync(new Runnable() {
                        @Override public void run() {
                            luckPermsFacade.invalidateCache(player.getUniqueId());
                            player.sendMessage(ChatColor.GREEN + "* Prefix applied: " + colorize(selectedPrefix));
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override public void run() { openMainMenu(player); }
                            });
                        }
                    }, new java.util.concurrent.Executor() {
                        @Override public void execute(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }
                    });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Item Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private ItemStack makeColoredPane(short damage) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, damage);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
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
