package de.locki.lockiprefixes.gui;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.gui.ChatEditSession.EditType;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Prefix Manager GUI for the latest MC version (1.20-1.21).
 * Uses Adventure API for rich text and clickable chat prompts.
 */
public class PrefixMenuManager {

    // ─── Constants ──────────────────────────────────────────────────────────
    public static final String MENU_TITLE     = "§8✦ §bRank Style Studio §8✦";
    public static final String LIST_TITLE     = "§8✦ §3Group Style List §8✦";
    public static final String TEMPLATE_TITLE = "§8✦ §dStyle Templates §8✦";
    public static final String REVIEW_TITLE   = "§8✦ §aCreate Rank §8✦";

    // Slot layout for the 45-slot (5-row) main menu
    private static final int SLOT_SKULL       = 13; // Row 2, Center
    private static final int SLOT_TEMPLATES   = 15; // Row 2, Right
    private static final int SLOT_PREFIX_INFO = 22; // Row 3, Center
    private static final int SLOT_TAB_GUIDE   = 20; // Row 3, Left — TAB integration help
    private static final int SLOT_EDIT_PREFIX = 29; // Row 4, Left
    private static final int SLOT_CREATE_RANK = 31; // Row 4, Middle
    private static final int SLOT_LIST_PREFIX = 33; // Row 4, Right
    private static final int SLOT_CLOSE       = 40; // Row 5, Center

    // ─── State ───────────────────────────────────────────────────────────────
    private final LockiPrefixesPlugin plugin;
    private final LuckPermsFacade luckPermsFacade;

    /** Active edit sessions keyed by player UUID */
    private final Map<UUID, ChatEditSession> sessions = new ConcurrentHashMap<>();

    /** Active target group for each editing player */
    private final Map<UUID, String> editingGroup = new ConcurrentHashMap<>();

    /** Target group for template picker */
    private final Map<UUID, String> templateTargetGroup = new ConcurrentHashMap<>();

    /** Pending rank name during Create-Rank review (stored before save) */
    private final Map<UUID, String> pendingCreateName = new ConcurrentHashMap<>();

    /** Template selected in the Create-Rank review menu */
    private final Map<UUID, String> selectedTemplateId = new ConcurrentHashMap<>();

    // Slots for the Create-Rank review menu (45-slot, 5-row)
    // Row 2 (10-16): 6 templates, slot 13 is a fill pane
    // Row 3 (18-26): rank info at 22, rest fill panes
    // Row 4 (27-35): Cancel(29), Create(31), Edit(33)
    private static final int   REVIEW_SLOT_RANK_INFO = 22;
    private static final int   REVIEW_SLOT_CANCEL    = 29;
    private static final int   REVIEW_SLOT_CREATE    = 31;
    private static final int   REVIEW_SLOT_EDIT      = 33;
    private static final int[] REVIEW_TEMPLATE_SLOTS = {10, 11, 12, 14, 15, 16};

    private enum InputMode {
        EDIT_GROUP_FORMAT,
        CREATE_GROUP_NAME
    }

    private final Map<UUID, InputMode> inputModes = new ConcurrentHashMap<>();

    /** UI thread guard: menus that are open (stored as Set of UUIDs) */
    private final Set<UUID> openMenus = ConcurrentHashMap.newKeySet();

    public PrefixMenuManager(LockiPrefixesPlugin plugin, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.luckPermsFacade = luckPermsFacade;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════════

    /** Opens the main prefix manager GUI for a player. */
    public void openMainMenu(Player player) {
        PlayerData data = plugin.createPlayerData(player);
        Inventory inv = buildMainMenu(player, data);
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    /** Opens the group format list submenu for a player. */
    public void openPrefixListMenu(Player player) {
        Inventory inv = buildPrefixListMenu(player);
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void openTemplateMenu(Player player, String groupName) {
        templateTargetGroup.put(player.getUniqueId(), groupName.toLowerCase(Locale.ROOT));
        Inventory inv = buildTemplateMenu(groupName);
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    /** Opens the Create-Rank review menu for a player with the pending rank name. */
    public void openCreateReviewMenu(Player player, String pendingName) {
        pendingCreateName.put(player.getUniqueId(), pendingName);
        Inventory inv = buildCreateReviewMenu(pendingName, selectedTemplateId.get(player.getUniqueId()));
        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    /** Returns true if this player has the main menu open. */
    public boolean hasOpenMenu(UUID uuid) {
        return openMenus.contains(uuid);
    }

    /** Called when a player closes any of our inventories. */
    public void onInventoryClosed(UUID uuid) {
        openMenus.remove(uuid);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Session Management
    // ═══════════════════════════════════════════════════════════════════════

    /** Starts an edit session for the player and instructs them to type in chat. */
    public void startEditSession(Player player, EditType type) {
        PlayerData data = plugin.createPlayerData(player);
        String groupName = data.getPrimaryGroup() != null ? data.getPrimaryGroup().toLowerCase(Locale.ROOT) : "default";
        startEditSessionForGroup(player, type, groupName);
    }

    public void startEditSessionForGroup(Player player, EditType type, String groupName) {
        LockiConfig.GroupFormat groupFormat = plugin.getLockiConfig().getGroupFormat(groupName);
        String current = groupFormat != null && groupFormat.getChatFormat() != null
            ? groupFormat.getChatFormat()
            : plugin.getLockiConfig().getDefaultChatFormat();
        if (current == null || current.isEmpty()) {
            current = "{prefix} &7| &f{name} &7» &f{message}";
        }
        ChatEditSession session = new ChatEditSession(player.getUniqueId(), type, current != null ? current : "");
        sessions.put(player.getUniqueId(), session);
        editingGroup.put(player.getUniqueId(), groupName);
        inputModes.put(player.getUniqueId(), InputMode.EDIT_GROUP_FORMAT);

        // Close inventory first (sync, must be on main thread)
        player.closeInventory();

        sendEditIntro(player, groupName, current);
    }

    public void startCreateGroupSession(Player player) {
        sessions.put(player.getUniqueId(), new ChatEditSession(player.getUniqueId(), EditType.PREFIX, ""));
        inputModes.put(player.getUniqueId(), InputMode.CREATE_GROUP_NAME);
        editingGroup.remove(player.getUniqueId());
        pendingCreateName.remove(player.getUniqueId());
        selectedTemplateId.remove(player.getUniqueId());
        player.closeInventory();

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ Create New Rank", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("Type a rank key in chat (e.g. owner, vip, admin).", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Allowed: a-z, 0-9, _, -  │  type 'cancel' to abort", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    public boolean isCreateRankMode(UUID uuid) {
        return inputModes.getOrDefault(uuid, InputMode.EDIT_GROUP_FORMAT) == InputMode.CREATE_GROUP_NAME;
    }

    /** Returns the active session for a player, or null if none. */
    public ChatEditSession getSession(UUID uuid) {
        ChatEditSession session = sessions.get(uuid);
        if (session != null && session.isExpired()) {
            sessions.remove(uuid); // clean up expired
            return null;
        }
        return session;
    }

    /** Called when the player types a chat message during an edit session. */
    public void updateSessionDraft(Player player, String input) {
        ChatEditSession session = getSession(player.getUniqueId());
        if (session == null) return;

        session.setDraft(input);

        InputMode mode = inputModes.getOrDefault(player.getUniqueId(), InputMode.EDIT_GROUP_FORMAT);
        if (mode == InputMode.CREATE_GROUP_NAME) {
            if (input.equalsIgnoreCase("cancel")) {
                sessions.remove(player.getUniqueId());
                inputModes.remove(player.getUniqueId());
                pendingCreateName.remove(player.getUniqueId());
                player.sendMessage(Component.text("✘ Cancelled.", NamedTextColor.RED));
                Bukkit.getScheduler().runTask(plugin, () -> openMainMenu(player));
                return;
            }

            String normalized = normalizeGroupKey(input);
            if (!normalized.matches("[a-z0-9_-]+")) {
                player.sendMessage(Component.text("✘ Invalid name. Only a-z, 0-9, _, - are allowed.", NamedTextColor.RED));
                return;
            }

            // Store draft name and open the review menu (no LP check / no save yet)
            sessions.remove(player.getUniqueId());
            inputModes.remove(player.getUniqueId());
            pendingCreateName.put(player.getUniqueId(), normalized);
            selectedTemplateId.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> openCreateReviewMenu(player, normalized));
            return;
        }

        String sampleName = player.getName();
        String sampleMessage = "hello world";
        String groupName = editingGroup.get(player.getUniqueId());
        LockiConfig.GroupFormat groupFormat = groupName != null ? plugin.getLockiConfig().getGroupFormat(groupName) : null;
        String previous = groupFormat != null && groupFormat.getChatFormat() != null
            ? groupFormat.getChatFormat()
            : plugin.getLockiConfig().getDefaultChatFormat();

        Component previousPreview = buildPreviewComponent(previous, sampleName, sampleMessage);
        Component newPreview = buildPreviewComponent(input, sampleName, sampleMessage);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓", NamedTextColor.DARK_GRAY));
        player.sendMessage(
            Component.text("  ✎ ", NamedTextColor.AQUA, TextDecoration.BOLD)
            .append(Component.text("Live Preview", NamedTextColor.WHITE, TextDecoration.BOLD))
        );
        player.sendMessage(Component.text("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫", NamedTextColor.DARK_GRAY));

        player.sendMessage(
            Component.text("  Before Format: ", NamedTextColor.GRAY)
                .append(deserialize(previous != null ? previous : "{name}: {message}"))
        );
        player.sendMessage(
            Component.text("  Before Chat:   ", NamedTextColor.DARK_GRAY)
                .append(previousPreview)
        );
        player.sendMessage(Component.text("  ───────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(
            Component.text("  New Format:    ", NamedTextColor.GRAY)
                .append(deserialize(input))
        );
        player.sendMessage(
            Component.text("  New Chat:      ", NamedTextColor.AQUA)
                .append(newPreview)
        );

        if (!containsUserPlaceholder(input)) {
            player.sendMessage(Component.text("  ⚠ Warning: format does not contain {user}/{name}.", NamedTextColor.YELLOW));
        }
        if (!containsMessagePlaceholder(input)) {
            player.sendMessage(Component.text("  ⚠ Warning: format does not contain {message}.", NamedTextColor.YELLOW));
        }
        
        player.sendMessage(Component.empty());
        sendAcceptCancelBar(player);
        player.sendMessage(Component.text("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    /** Accepts the current draft and writes it into groups.<group>.chat-format. */
    public void acceptSession(Player player) {
        ChatEditSession session = getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("No active edit session.", NamedTextColor.RED));
            return;
        }

        InputMode mode = inputModes.getOrDefault(player.getUniqueId(), InputMode.EDIT_GROUP_FORMAT);
        if (mode == InputMode.CREATE_GROUP_NAME) {
            player.sendMessage(Component.text("No /accept needed in create mode. Type rank key directly.", NamedTextColor.YELLOW));
            return;
        }

        String groupName = editingGroup.remove(player.getUniqueId());
        if (groupName == null || groupName.isEmpty()) {
            player.sendMessage(Component.text("No target group selected.", NamedTextColor.RED));
            return;
        }

        String value = session.getDraft();
        String normalized = value.replace("{user}", "{name}");

        if (!containsUserPlaceholder(normalized)) {
            player.sendMessage(Component.text("⚠ Saved format has no {user}/{name}. Player name may not show.", NamedTextColor.YELLOW));
        }
        if (!containsMessagePlaceholder(normalized)) {
            player.sendMessage(Component.text("⚠ Saved format has no {message}. Chat text may not show.", NamedTextColor.YELLOW));
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.updateGroupChatFormat(groupName, normalized);
            luckPermsFacade.invalidateCache(player.getUniqueId());
            player.sendMessage(Component.text("✔ Saved group format for " + groupName + "!", NamedTextColor.GREEN));
            Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 20L);
        });

        sessions.remove(player.getUniqueId());
        inputModes.remove(player.getUniqueId());
    }

    /** Cancels the current edit session without applying changes. */
    public void cancelSession(Player player) {
        ChatEditSession session = sessions.remove(player.getUniqueId());
        editingGroup.remove(player.getUniqueId());
        inputModes.remove(player.getUniqueId());
        templateTargetGroup.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("No active edit session.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✘ Edit cancelled. No changes were made.", NamedTextColor.RED));
        player.sendMessage(Component.empty());
        // Reopen the menu on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> openMainMenu(player));
    }

    /** Removes a session without feedback (e.g. on disconnect). */
    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
        editingGroup.remove(uuid);
        inputModes.remove(uuid);
        templateTargetGroup.remove(uuid);
        pendingCreateName.remove(uuid);
        selectedTemplateId.remove(uuid);
        openMenus.remove(uuid);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private helpers ─ GUI building
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("deprecation")
    private Inventory buildMainMenu(Player player, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, MENU_TITLE);

        ItemStack frame = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack fill = makeItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) inv.setItem(i, fill);
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,37,38,39,40,41,42,43,44
        };
        for (int s : borderSlots) inv.setItem(s, frame);
        
        String groupName = data.getPrimaryGroup() != null ? data.getPrimaryGroup().toLowerCase(Locale.ROOT) : "default";
        LockiConfig.GroupFormat groupFormat = plugin.getLockiConfig().getGroupFormat(groupName);
        String activeFormat = groupFormat != null && groupFormat.getChatFormat() != null
            ? groupFormat.getChatFormat()
            : plugin.getLockiConfig().getDefaultChatFormat();

        // Slot 13: Player skull (Info)
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.displayName(Component.text("§e" + player.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> skullLore = new ArrayList<>();
            skullLore.add(Component.text("  Group: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(data.getPrimaryGroup() != null ? data.getPrimaryGroup() : "Default", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            skullLore.add(Component.text("  Editing: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(groupName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            skullLore.add(Component.empty());
            skullMeta.lore(skullLore);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(SLOT_SKULL, skull);

        ItemStack templatePicker = makeItemWithLore(Material.ENDER_CHEST,
            "§d§lTemplates",
            Arrays.asList(
                Component.empty(),
                Component.text("  Pick a starter style", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  and customize colors/text", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to open]", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(SLOT_TEMPLATES, templatePicker);

        // Slot 20: TAB integration guide
        ItemStack tabGuide = makeItemWithLore(Material.FILLED_MAP,
            "§e§lTablist Setup",
            Arrays.asList(
                Component.empty(),
                Component.text("  Click to open the setup guide", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  for the TAB plugin integration.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to open guide]", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(SLOT_TAB_GUIDE, tabGuide);

        // Slot 22: Current Prefix Info (Center)
        ItemStack prefixItem = makeItemWithLore(Material.LECTERN,
            "§6§lCurrent Group Format",
            Arrays.asList(
                Component.empty(),
                Component.text("  Group: " + groupName, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(deserialize(activeFormat != null ? activeFormat : "{name}: {message}").decoration(TextDecoration.ITALIC, false)),
                Component.empty()
            )
        );
        addGlow(prefixItem);
        inv.setItem(SLOT_PREFIX_INFO, prefixItem);

        // Slot 29: Edit Prefix button (Left)
        ItemStack editPrefix = makeItemWithLore(Material.NETHER_STAR,
            "§e§l✎ Edit Style",
            Arrays.asList(
                Component.empty(),
                Component.text("  Edit current group's", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  chat-format from config.yml", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  with live before/after preview", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to edit]", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        addGlow(editPrefix);
        inv.setItem(SLOT_EDIT_PREFIX, editPrefix);

        ItemStack createRank = makeItemWithLore(Material.ANVIL,
            "§a§lCreate New Rank",
            Arrays.asList(
                Component.empty(),
                Component.text("  Create a new rank key", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  like owner, vipplus, creator", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to create]", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(SLOT_CREATE_RANK, createRank);

        // Slot 33: View all LuckPerms prefixes (Right)
        ItemStack listItem = makeItemWithLore(Material.COMPASS,
            "§b§lEdit Other Groups",
            Arrays.asList(
                Component.empty(),
                Component.text("  Owner, Admin, VIP ...", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  directly from config groups.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to browse]", NamedTextColor.AQUA).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(SLOT_LIST_PREFIX, listItem);

        // Slot 40: Close button
        ItemStack close = makeItemWithLore(Material.BARRIER,
            "§c§lClose",
            Arrays.asList(
                Component.empty(),
                Component.text("  Close menu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(SLOT_CLOSE, close);

        return inv;
    }

    @SuppressWarnings("deprecation")
    private Inventory buildPrefixListMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, LIST_TITLE);

        ItemStack frame = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack fill = makeItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) inv.setItem(i, fill);
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,37,38,39,40,41,42,43,44
        };
        for (int s : borderSlots) inv.setItem(s, frame);

        List<String> groups = new ArrayList<>(plugin.getLockiConfig().getGroupFormats().keySet());
        Collections.sort(groups);

        if (!groups.isEmpty()) {
            // Use center area slots
            int[] contentSlots = {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34
            };
            int idx = 0;
            for (String groupName : groups) {
                if (idx >= contentSlots.length) break;
                LockiConfig.GroupFormat gf = plugin.getLockiConfig().getGroupFormat(groupName);
                String chatFormat = (gf != null && gf.getChatFormat() != null)
                    ? gf.getChatFormat()
                    : plugin.getLockiConfig().getDefaultChatFormat();
                ItemStack pfxItem = makeItemWithLore(Material.WRITABLE_BOOK,
                    "§e" + groupName,
                    Arrays.asList(
                        Component.empty(),
                        Component.text("  Chat-Format:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        deserialize(chatFormat != null ? chatFormat : "{name}: {message}").decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("  [Click to edit this group]", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.empty()
                    )
                );
                inv.setItem(contentSlots[idx], pfxItem);
                idx++;
            }
        } else {
            // Empty info
            ItemStack info = makeItemWithLore(Material.PAPER, "§cNo Groups Found", Collections.singletonList(Component.text("No groups section found in config.")));
            inv.setItem(22, info);
        }

        // Back button at slot 40
        ItemStack back = makeItemWithLore(Material.ARROW,
            "§a§l← Back",
            Arrays.asList(
                Component.empty(),
                Component.text("  Return to main menu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(40, back);
        return inv;
    }

    @SuppressWarnings("deprecation")
    private Inventory buildTemplateMenu(String targetGroup) {
        Inventory inv = Bukkit.createInventory(null, 45, TEMPLATE_TITLE);

        ItemStack frame = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack fill = makeItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) inv.setItem(i, fill);
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,37,38,39,40,41,42,43,44
        };
        for (int s : borderSlots) inv.setItem(s, frame);

        List<TemplateDef> templates = getTemplates();
        int[] contentSlots = {11,13,15,29,31,33};
        for (int i = 0; i < templates.size() && i < contentSlots.length; i++) {
            TemplateDef t = templates.get(i);
            String preview = resolveTemplate(t.format, targetGroup);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  Preview:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(deserialize(preview).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("  [Click to use template]", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("TEMPLATE:" + t.id, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            ItemStack item = makeItemWithLore(t.icon,
                "§d" + t.name,
                lore
            );
            inv.setItem(contentSlots[i], item);
        }

        ItemStack back = makeItemWithLore(Material.ARROW,
            "§a§l← Back",
            Arrays.asList(
                Component.empty(),
                Component.text("  Return to main menu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(40, back);

        return inv;
    }

    @SuppressWarnings("deprecation")
    private Inventory buildCreateReviewMenu(String pendingName, String selectedId) {
        Inventory inv = Bukkit.createInventory(null, 45, REVIEW_TITLE);

        ItemStack frame = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack fill  = makeItem(Material.GREEN_STAINED_GLASS_PANE,  " ");
        for (int i = 0; i < 45; i++) inv.setItem(i, fill);

        // Row 1, row-2/3/4 sides, row-5 – border
        // Row 3 inner (19-25) stays as fill; rank info placed at slot 22
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,37,38,39,40,41,42,43,44
        };
        for (int s : borderSlots) inv.setItem(s, frame);

        // Six template previews – row 2 slots (slot 13 left as fill gap)
        List<TemplateDef> templates = getTemplates();
        for (int i = 0; i < templates.size() && i < REVIEW_TEMPLATE_SLOTS.length; i++) {
            TemplateDef t    = templates.get(i);
            boolean selected = t.id.equalsIgnoreCase(selectedId);
            String preview   = resolveTemplate(t.format, pendingName)
                .replace("{name}",    "§fPlayer")
                .replace("{message}", "§7Message");

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  Preview:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(deserialize(preview).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            if (selected) {
                lore.add(Component.text("  ✔ Selected", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("  [Click to select]", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("TEMPLATE:" + t.id, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            ItemStack item = makeItemWithLore(t.icon, "§d" + t.name + (selected ? " §a✔" : ""), lore);
            if (selected) addGlow(item);
            inv.setItem(REVIEW_TEMPLATE_SLOTS[i], item);
        }

        // Rank info – row 3 center (slot 22), below all templates
        ItemStack rankInfo = makeItemWithLore(Material.NAME_TAG,
            "§a§lNew rank: §f" + toTitle(pendingName),
            Arrays.asList(
                Component.empty(),
                Component.text("  Key: §f" + pendingName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  Pick a template above, then click Create.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(REVIEW_SLOT_RANK_INFO, rankInfo);

        // Row 4 buttons: Cancel (29), Create (31), Change Name (33)
        ItemStack cancelBtn = makeItemWithLore(Material.BARRIER,
            "§c§lCancel",
            Arrays.asList(
                Component.empty(),
                Component.text("  Return to main menu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  Nothing will be saved.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(REVIEW_SLOT_CANCEL, cancelBtn);

        ItemStack createBtn = makeItemWithLore(Material.EMERALD,
            "§a§lCreate",
            Arrays.asList(
                Component.empty(),
                Component.text("  Rank §f" + toTitle(pendingName) + " §8will be saved.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  If the LuckPerms group doesn't exist yet,", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  it will be created automatically.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  [Click to create]", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        addGlow(createBtn);
        inv.setItem(REVIEW_SLOT_CREATE, createBtn);

        ItemStack editBtn = makeItemWithLore(Material.ANVIL,
            "§e§lChange Name",
            Arrays.asList(
                Component.empty(),
                Component.text("  Type a new name in chat.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
            )
        );
        inv.setItem(REVIEW_SLOT_EDIT, editBtn);

        return inv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private helpers ─ chat UI
    // ═══════════════════════════════════════════════════════════════════════

    private void sendAcceptCancelBar(Player player) {
        Component acceptBtn = Component.text(" [ ✔ SAVE ] ", NamedTextColor.GREEN, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/lockiprefixes accept"))
            .hoverEvent(HoverEvent.showText(Component.text("Apply the current draft.", NamedTextColor.GREEN)));

        Component cancelBtn = Component.text(" [ ✘ CANCEL ] ", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/lockiprefixes cancel"))
            .hoverEvent(HoverEvent.showText(Component.text("Discard changes and go back.", NamedTextColor.RED)));

        player.sendMessage(
            Component.text("  ", NamedTextColor.GRAY)
            .append(acceptBtn)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(cancelBtn)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private helpers ─ shared util
    // ═══════════════════════════════════════════════════════════════════════

    /** Reads TEMPLATE:<id> from an item's lore. Returns null if not found. */
    private String extractTemplateId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        List<Component> lore = meta.lore();
        if (lore == null) return null;
        for (Component line : lore) {
            String plain = stripColor(LegacyComponentSerializer.legacySection().serialize(line));
            if (plain != null && plain.startsWith("TEMPLATE:")) {
                return plain.substring("TEMPLATE:".length()).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private helpers ─ item building
    // ═══════════════════════════════════════════════════════════════════════

    private ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItemWithLore(Material material, String rawName, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(deserialize(rawName).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Paper 1.20.5+ supports enchantment glow override natively
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private helpers ─ util
    // ═══════════════════════════════════════════════════════════════════════

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();

    /** Deserializes a legacy-formatted (§/&) string into an Adventure Component. */
    private static Component deserialize(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return LEGACY.deserialize(text);
    }

    /** Strips color/format codes from a string (for display in item names). */
    private static String stripColor(String text) {
        if (text == null) return null;
        return text.replaceAll("(?i)(&|§)([0-9a-fk-or])", "")
                   .replaceAll("(?i)(&|§)#[0-9a-fA-F]{6}", "");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Click routing ─ delegated from PrefixGuiListener
    // ═══════════════════════════════════════════════════════════════════════

    /** @return true if this was one of our menus and the click was handled */
    public boolean handleClick(Player player, Inventory inv, int slot, String invTitle) {
        if (invTitle.equals(MENU_TITLE)) {
            handleMainMenuClick(player, slot);
            return true;
        }
        if (invTitle.equals(LIST_TITLE)) {
            handlePrefixListClick(player, inv, slot);
            return true;
        }
        if (invTitle.equals(TEMPLATE_TITLE)) {
            handleTemplateClick(player, inv, slot);
            return true;
        }
        if (invTitle.equals(REVIEW_TITLE)) {
            handleCreateReviewClick(player, inv, slot);
            return true;
        }
        return false;
    }

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case SLOT_TAB_GUIDE: {
                player.closeInventory();
                Component guide = Component.text("\n")
                    .append(Component.text("  ✦ Tablist Setup Guide", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text("\n  Click the link below to open the setup guide in your browser:\n  ", NamedTextColor.GRAY))
                    .append(Component.text("▶ Open guide", NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://github.com/locki/lockiprefixes#tab-plugin"))
                        .hoverEvent(HoverEvent.showText(Component.text("https://github.com/locki/lockiprefixes#tab-plugin", NamedTextColor.GRAY))))
                    .append(Component.text("\n", NamedTextColor.GRAY));
                player.sendMessage(guide);
                break;
            }
            case SLOT_TEMPLATES: {
                String group = editingGroup.get(player.getUniqueId());
                if (group == null || group.isEmpty()) {
                    PlayerData data = plugin.createPlayerData(player);
                    group = data.getPrimaryGroup() != null ? data.getPrimaryGroup().toLowerCase(Locale.ROOT) : "default";
                }
                openTemplateMenu(player, group);
                break;
            }
            case SLOT_EDIT_PREFIX:
                startEditSession(player, EditType.PREFIX);
                break;
            case SLOT_CREATE_RANK:
                startCreateGroupSession(player);
                break;
            case SLOT_LIST_PREFIX:
                openPrefixListMenu(player);
                break;
            case SLOT_CLOSE:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handlePrefixListClick(Player player, Inventory inv, int slot) {
        // Back button
        if (slot == 40) {
            openMainMenu(player);
            return;
        }

        ItemStack clicked = inv.getItem(slot);
        if (clicked == null
            || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE
            || clicked.getType() == Material.BLUE_STAINED_GLASS_PANE
            || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (clicked.getType() == Material.ARROW) return; // handled above with slot 40

        // Get selected group from display name
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String groupName = stripColor(LegacyComponentSerializer.legacySection().serialize(meta.displayName()));
            if (groupName != null && !groupName.trim().isEmpty()) {
                groupName = groupName.toLowerCase(Locale.ROOT);
                LockiConfig.GroupFormat gf = plugin.getLockiConfig().getGroupFormat(groupName);
                if (gf != null) {
                    String current = gf.getChatFormat() != null ? gf.getChatFormat() : plugin.getLockiConfig().getDefaultChatFormat();
                    startEditSessionForGroup(player, EditType.PREFIX, groupName);
                }
            }
        }
    }

    private void handleTemplateClick(Player player, Inventory inv, int slot) {
        if (slot == 40) {
            openMainMenu(player);
            return;
        }

        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.PURPLE_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (clicked.getType() == Material.ARROW) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String templateId = extractTemplateId(clicked);
        if (templateId == null || templateId.isEmpty()) return;

        String targetGroup = templateTargetGroup.get(player.getUniqueId());
        if (targetGroup == null || targetGroup.isEmpty()) {
            PlayerData data = plugin.createPlayerData(player);
            targetGroup = data.getPrimaryGroup() != null ? data.getPrimaryGroup().toLowerCase(Locale.ROOT) : "default";
        }

        TemplateDef selected = getTemplateById(templateId);
        if (selected == null) return;

        String templateFormat = resolveTemplate(selected.format, targetGroup);
        sessions.put(player.getUniqueId(), new ChatEditSession(player.getUniqueId(), EditType.PREFIX, templateFormat));
        editingGroup.put(player.getUniqueId(), targetGroup);
        inputModes.put(player.getUniqueId(), InputMode.EDIT_GROUP_FORMAT);
        player.closeInventory();
        sendEditIntro(player, targetGroup, templateFormat);
    }

    private void handleCreateReviewClick(Player player, Inventory inv, int slot) {
        String pendingName = pendingCreateName.get(player.getUniqueId());
        if (pendingName == null) {
            // Safety: something went wrong – send back to main menu
            openMainMenu(player);
            return;
        }

        if (slot == REVIEW_SLOT_CANCEL) {
            pendingCreateName.remove(player.getUniqueId());
            selectedTemplateId.remove(player.getUniqueId());
            openMainMenu(player);
            return;
        }

        if (slot == REVIEW_SLOT_EDIT) {
            pendingCreateName.remove(player.getUniqueId());
            selectedTemplateId.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> startCreateGroupSession(player));
            return;
        }

        if (slot == REVIEW_SLOT_CREATE) {
            handleCreateConfirm(player, pendingName);
            return;
        }

        // Template selection click
        String templateId = extractTemplateId(inv.getItem(slot));
        if (templateId == null) return;

        selectedTemplateId.put(player.getUniqueId(), templateId);
        Bukkit.getScheduler().runTask(plugin, () -> openCreateReviewMenu(player, pendingName));
    }

    private void handleCreateConfirm(Player player, String pendingName) {
        player.closeInventory();
        String chosenId = selectedTemplateId.getOrDefault(player.getUniqueId(), "classic");
        TemplateDef template = getTemplateById(chosenId);
        if (template == null) template = getTemplates().get(0);

        String chatFormat = resolveTemplate(template.format, pendingName);
        String tabFormat   = deriveTablistFormat(chatFormat);

        pendingCreateName.remove(player.getUniqueId());
        selectedTemplateId.remove(player.getUniqueId());

        final String finalChat   = chatFormat;
        final String finalTab    = tabFormat;
        final String finalName   = pendingName;

        luckPermsFacade.groupExists(finalName).thenAccept(exists -> {
            if (exists) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    plugin.ensureGroupExists(finalName, finalChat, finalTab, 10);
                    editingGroup.put(player.getUniqueId(), finalName);
                    player.sendMessage(Component.text("✔ Rank §a" + toTitle(finalName) + "§r created!", NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 5L);
                });
            } else {
                // Auto-create LP group, then save config
                luckPermsFacade.createGroup(finalName).thenAccept(group ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;
                        plugin.ensureGroupExists(finalName, finalChat, finalTab, 10);
                        editingGroup.put(player.getUniqueId(), finalName);
                        player.sendMessage(Component.text("✔ Rank §a" + toTitle(finalName) + "§r created (LuckPerms group added)!", NamedTextColor.GREEN));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 5L);
                    })
                );
            }
        });
    }

    private static class TemplateDef {
        final String id;
        final String name;
        final Material icon;
        final String format;

        TemplateDef(String id, String name, Material icon, String format) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.format = format;
        }
    }

    private List<TemplateDef> getTemplates() {
        List<TemplateDef> templates = new ArrayList<>();
        templates.add(new TemplateDef("classic", "Classic", Material.BOOK, "&b&l{RANK} &7| &f{name} &7» &f{message}"));
        templates.add(new TemplateDef("arrow", "Arrow", Material.SPECTRAL_ARROW, "&b{RANK} &8➜ &f{name} &8: &7{message}"));
        templates.add(new TemplateDef("brackets", "Brackets", Material.NAME_TAG, "&8[&b{RANK}&8] &f{name} &8» &7{message}"));
        templates.add(new TemplateDef("minimal", "Minimal", Material.FEATHER, "&f{name} &8» &7{message}"));
        templates.add(new TemplateDef("bold", "Bold Rank", Material.NETHER_STAR, "&b&l{RANK} &8- &f{name} &8: &7{message}"));
        templates.add(new TemplateDef("pipe", "Clean Pipe", Material.PAPER, "&b{RANK} &7| &f{name} &7| &f{message}"));
        return templates;
    }

    private TemplateDef getTemplateById(String id) {
        for (TemplateDef template : getTemplates()) {
            if (template.id.equalsIgnoreCase(id)) {
                return template;
            }
        }
        return null;
    }

    private String resolveTemplate(String template, String groupName) {
        return template.replace("{RANK}", toTitle(groupName));
    }

    /**
     * Derives the tablist format from a chat format.
     * Cuts everything after the last {name}/{user} placeholder so that
     * formatting codes like &l (bold) before {name} are preserved.
     */
    public static String deriveTablistFormat(String chatFormat) {
        if (chatFormat == null || chatFormat.isEmpty()) return "&f{name}";
        int nameIdx = chatFormat.lastIndexOf("{name}");
        int userIdx = chatFormat.lastIndexOf("{user}");
        int cutAt   = Math.max(nameIdx, userIdx);
        if (cutAt >= 0) {
            int endIdx = (nameIdx >= userIdx)
                ? cutAt + "{name}".length()
                : cutAt + "{user}".length();
            return chatFormat.substring(0, endIdx);
        }
        // No name placeholder: strip {message} and any trailing color codes
        return chatFormat
            .replaceAll("(?i)\\{message\\}", "")
            .replaceAll("[&§][0-9a-fk-or]*\\s*$", "")
            .trim() + " &f{name}";
    }

    private void sendEditIntro(Player player, String groupName, String currentFormat) {
        Component currentPreview = buildPreviewComponent(currentFormat, player.getName(), "hello world");
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  ✦ Group Style Editor", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  Group: " + groupName, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Current Format:", NamedTextColor.GRAY));
        player.sendMessage(Component.text("   ").append(deserialize(currentFormat != null ? currentFormat : "{name}: {message}")));
        player.sendMessage(Component.text("  Current Preview:", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("   ").append(currentPreview));
        player.sendMessage(Component.text("  Variables: {name}/{user}, {message}", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Type your new format in chat.", NamedTextColor.WHITE));
        sendAcceptCancelBar(player);
        player.sendMessage(Component.text("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    private Component buildPreviewComponent(String format, String sampleName, String sampleMessage) {
        String template = format != null && !format.isEmpty() ? format : "{name}: {message}";
        if (!template.contains("{message}")) {
            template = template + " &8» &7{message}";
        }
        if (!template.contains("{name}") && !template.contains("{user}")) {
            template = template + " &f{name}";
        }
        String preview = template
            .replace("{user}", sampleName)
            .replace("{name}", sampleName)
            .replace("{message}", sampleMessage);
        return deserialize(preview);
    }

    private boolean containsUserPlaceholder(String format) {
        if (format == null) return false;
        String lower = format.toLowerCase(Locale.ROOT);
        return lower.contains("{name}") || lower.contains("{user}");
    }

    private boolean containsMessagePlaceholder(String format) {
        if (format == null) return false;
        return format.toLowerCase(Locale.ROOT).contains("{message}");
    }

    private String normalizeGroupKey(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String toTitle(String value) {
        if (value == null || value.isEmpty()) {
            return "Rank";
        }
        String normalized = value.replace('_', ' ').replace('-', ' ');
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Rank" : builder.toString();
    }
}
