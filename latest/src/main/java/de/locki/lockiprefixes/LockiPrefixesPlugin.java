package de.locki.lockiprefixes;

import de.locki.lockiprefixes.chat.AdventureChatListener;
import de.locki.lockiprefixes.command.ReloadCommand;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.gui.PrefixChatInputListener;
import de.locki.lockiprefixes.gui.PrefixGuiListener;
import de.locki.lockiprefixes.gui.PrefixMenuManager;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.papi.LockiPrefixesExpansion;
import de.locki.lockiprefixes.placeholder.PlayerData;
import de.locki.lockiprefixes.tablist.TablistManager;
import de.locki.lockiprefixes.update.UpdateNotifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * LockiPrefixes Plugin - Latest version for Minecraft 1.20-1.21
 * Uses Paper's Adventure API for optimal chat handling.
 * Supports Folia for multi-threaded servers.
 */
public class LockiPrefixesPlugin extends JavaPlugin implements Listener {

    private static final String CHANGELOG_RAW_URL = "https://raw.githubusercontent.com/leifiyoo/lockiprefixes/main/CHANGELOG.json";
    private static final String CHANGELOG_PAGE_URL = "https://github.com/leifiyoo/lockiprefixes/blob/main/CHANGELOG.json";

    private static LockiPrefixesPlugin instance;

    private LockiConfig lockiConfig;
    private LuckPermsFacade luckPermsFacade;
    private ChatFormatter chatFormatter;
    private TablistManager tablistManager;
    private PrefixMenuManager prefixMenuManager;
    private UpdateNotifier updateNotifier;
    
    private boolean luckPermsAvailable = false;
    private boolean placeholderApiAvailable = false;
    private boolean tabPluginAvailable = false;
    private boolean secureProfileEnforced = true;
    private final List<String> missingDependencies = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load configuration
        lockiConfig = new LockiConfig();
        lockiConfig.load(getConfig());

        int pluginId = 29406;
        new Metrics(this, pluginId);

        // Check dependencies
        checkDependencies();
        
        // Check secure profile setting
        checkSecureProfile();

        // Initialize LuckPerms if available
        if (luckPermsAvailable) {
            LuckPerms luckPerms = loadLuckPerms();
            if (luckPerms != null) {
                luckPermsFacade = new LuckPermsFacade(luckPerms);
                
                // Initialize formatter with hex support
                chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, true);

                // Register chat listener (Adventure-based)
                getServer().getPluginManager().registerEvents(
                    new AdventureChatListener(this, chatFormatter, luckPermsFacade),
                    this
                );
                
                // Register tablist manager only when TAB plugin is NOT present
                if (!tabPluginAvailable) {
                    tablistManager = new TablistManager(this, chatFormatter, luckPermsFacade);
                    getServer().getPluginManager().registerEvents(tablistManager, this);
                } else {
                    getLogger().info("TAB plugin detected - internal tablist disabled. Use placeholder %lockiprefixes_formatted% in TAB. (Guide: https://leifiyo.dev/docs/placeholders)");
                }

                // Initialize Prefix Manager GUI
                prefixMenuManager = new PrefixMenuManager(this, luckPermsFacade);
                getServer().getPluginManager().registerEvents(new PrefixGuiListener(prefixMenuManager), this);
                getServer().getPluginManager().registerEvents(new PrefixChatInputListener(prefixMenuManager), this);
            } else {
                luckPermsAvailable = false;
                missingDependencies.add("LuckPerms");
            }
        }

        // Register admin join listener for notifications
        getServer().getPluginManager().registerEvents(this, this);

        updateNotifier = new UpdateNotifier(this, CHANGELOG_RAW_URL, CHANGELOG_PAGE_URL);
        updateNotifier.start();

        // Register commands
        ReloadCommand reloadCommand = new ReloadCommand(this);
        reloadCommand.setMenuManager(prefixMenuManager);
        org.bukkit.command.PluginCommand cmd = getCommand("lockiprefixes");
        if (cmd != null) {
            cmd.setExecutor(reloadCommand);
            cmd.setTabCompleter(reloadCommand);
        } else {
            getLogger().severe("Command 'lockiprefixes' not found in plugin.yml — command registration skipped.");
        }

        // Register PlaceholderAPI expansion if available
        if (placeholderApiAvailable && chatFormatter != null) {
            new LockiPrefixesExpansion(this, this::getChatFormatter, this::createPlayerData).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // Log startup status
        if (!missingDependencies.isEmpty()) {
            getLogger().warning("======================================");
            getLogger().warning("LockiPrefixes - Missing Dependencies!");
            getLogger().warning("======================================");
            for (String dep : missingDependencies) {
                getLogger().warning("- " + dep + " is not installed!");
            }
            if (!luckPermsAvailable) {
                getLogger().warning("Chat formatting is DISABLED without LuckPerms!");
            }
            if (!placeholderApiAvailable) {
                getLogger().warning("PlaceholderAPI placeholders are not available.");
            }
            getLogger().warning("======================================");
        }
        
        // Warn about secure profile
        if (secureProfileEnforced) {
            getLogger().warning("");
            getLogger().warning("╔════════════════════════════════════════════════════════════╗");
            getLogger().warning("║  IMPORTANT: Secure Chat must be disabled for chat to work! ║");
            getLogger().warning("╠════════════════════════════════════════════════════════════╣");
            getLogger().warning("║  1. Open your server.properties file                       ║");
            getLogger().warning("║  2. Find: enforce-secure-profile=true                      ║");
            getLogger().warning("║  3. Change to: enforce-secure-profile=false                ║");
            getLogger().warning("║  4. Restart the server                                     ║");
            getLogger().warning("╚════════════════════════════════════════════════════════════╝");
            getLogger().warning("");
        }

        String foliaStatus = isFolia() ? " (Folia detected)" : "";
        getLogger().info("LockiPrefixes (Latest 1.20-1.21 with Adventure API) enabled!" + foliaStatus);
    }

    @Override
    public void onDisable() {
        if (updateNotifier != null) {
            updateNotifier.stop();
        }
        if (luckPermsFacade != null) {
            luckPermsFacade.clearCache();
        }
        instance = null;
        getLogger().info("LockiPrefixes disabled.");
    }

    private void checkDependencies() {
        // Check LuckPerms
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsAvailable = true;
        } else {
            missingDependencies.add("LuckPerms");
            getLogger().severe("LuckPerms not found! Chat formatting will be disabled.");
        }

        // Check PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiAvailable = true;
        } else {
            missingDependencies.add("PlaceholderAPI (optional)");
            getLogger().warning("PlaceholderAPI not found. PAPI placeholders will not be available.");
        }

        // Detect TAB plugin (any common variant)
        tabPluginAvailable = getServer().getPluginManager().getPlugin("TAB") != null
            || getServer().getPluginManager().getPlugin("tab-master") != null
            || getServer().getPluginManager().getPlugin("TABReborn") != null;
    }

    private void checkSecureProfile() {
        secureProfileEnforced = false; // Default to false, only warn if we confirm it's enabled
        
        try {
            // Resolve server.properties relative to the plugin's data folder parent
            // (i.e., the plugins/ folder -> server root), not the JVM working directory.
            java.io.File serverRoot = getDataFolder().getParentFile() != null
                ? getDataFolder().getParentFile().getParentFile()
                : new java.io.File(".");
            java.io.File serverProps = new java.io.File(serverRoot, "server.properties");
            if (!serverProps.exists()) {
                // Fallback: try CWD (standard MC server launch from server root)
                serverProps = new java.io.File("server.properties");
            }
            if (serverProps.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(serverProps)) {
                    props.load(fis);
                    String enforceSecure = props.getProperty("enforce-secure-profile", "true");
                    secureProfileEnforced = Boolean.parseBoolean(enforceSecure);
                }
            }
        } catch (Exception e) {
            // If we can't read, don't warn (assume user has it configured)
            secureProfileEnforced = false;
        }
    }

    private LuckPerms loadLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = 
            getServer().getServicesManager().getRegistration(LuckPerms.class);
        return provider != null ? provider.getProvider() : null;
    }

    /**
     * Check if running on Folia
     */
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Notify admins about missing dependencies and secure profile on join
     */
    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("lockiprefixes.notify")) {
            return;
        }

        // Delay the message slightly so it appears after join messages
        getServer().getGlobalRegionScheduler().runDelayed(this, task -> {
            if (!player.isOnline()) return;
            
            // Warn about secure profile
            if (secureProfileEnforced) {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("⚠ LockiPrefixes - Setup Required!", NamedTextColor.GOLD, TextDecoration.BOLD));
                player.sendMessage(Component.text("  Chat will not work until you disable secure chat!", NamedTextColor.RED));
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("  How to fix:", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("  1. Open ", NamedTextColor.GRAY)
                    .append(Component.text("server.properties", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  2. Set ", NamedTextColor.GRAY)
                    .append(Component.text("enforce-secure-profile=false", NamedTextColor.GREEN)));
                player.sendMessage(Component.text("  3. Restart the server", NamedTextColor.GRAY));
                player.sendMessage(Component.empty());
            }
            
            // Warn about missing dependencies
            if (!missingDependencies.isEmpty()) {
                player.sendMessage(Component.text("⚠ LockiPrefixes - Missing Dependencies!", NamedTextColor.GOLD, TextDecoration.BOLD));
                
                for (String dep : missingDependencies) {
                    if (dep.contains("optional")) {
                        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                            .append(Component.text(dep, NamedTextColor.YELLOW)));
                    } else {
                        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                            .append(Component.text(dep + " is not installed!", NamedTextColor.RED)));
                    }
                }
                
                if (!luckPermsAvailable) {
                    player.sendMessage(Component.text("  → Chat formatting is DISABLED!", NamedTextColor.RED, TextDecoration.ITALIC));
                }
                player.sendMessage(Component.empty());
            }
        }, 40L); // 2 second delay
    }

    /**
     * Creates PlayerData for a player.
     */
    public PlayerData createPlayerData(Player player) {
        PlayerData data = new PlayerData();
        data.setUuid(player.getUniqueId());
        data.setName(player.getName());
        // Use Adventure API for display name
        data.setDisplayName(PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        data.setWorld(player.getWorld().getName());

        if (luckPermsFacade != null) {
            luckPermsFacade.populatePlayerData(data);
        }
        return data;
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reload() {
        reloadConfig();
        lockiConfig.load(getConfig());
        if (luckPermsFacade != null) {
            luckPermsFacade.clearCache();
            chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, true);
            // Update tablist for all players
            if (tablistManager != null) {
                tablistManager.updateAll();
            }
        }
        getLogger().info("Configuration reloaded.");
    }

    /**
     * Updates groups.<group>.chat-format in config.yml and reloads runtime state.
     */
    public void updateGroupChatFormat(String groupName, String chatFormat) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        String normalized = groupName.toLowerCase();
        String chatKey = "groups." + normalized + ".chat-format";
        String tabKey = "groups." + normalized + ".tablist-format";
        getConfig().set(chatKey, chatFormat);

        String tabFormat = PrefixMenuManager.deriveTablistFormat(chatFormat);
        getConfig().set(tabKey, tabFormat);

        // Save config asynchronously to avoid blocking the server tick with disk I/O,
        // then reload the in-memory runtime state on the main thread.
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            saveConfig();
            getServer().getScheduler().runTask(this, this::reload);
        });
    }

    /**
     * Creates (or updates) a group block in config with baseline fields.
     */
    public void ensureGroupExists(String groupName, String chatFormat, String tablistFormat, int priority) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }

        String normalized = groupName.toLowerCase();
        String base = "groups." + normalized + ".";

        if (getConfig().getString(base + "chat-format") == null) {
            getConfig().set(base + "chat-format", chatFormat);
        }
        if (getConfig().getString(base + "tablist-format") == null) {
            getConfig().set(base + "tablist-format", tablistFormat);
        }
        if (getConfig().getString(base + "rank-tag") == null) {
            getConfig().set(base + "rank-tag", capitalize(normalized));
        }
        if (!getConfig().contains(base + "priority")) {
            getConfig().set(base + "priority", priority);
        }

        // Save config asynchronously to avoid blocking the server tick with disk I/O.
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            saveConfig();
            getServer().getScheduler().runTask(this, this::reload);
        });
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "Rank";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static LockiPrefixesPlugin getInstance() {
        return instance;
    }

    public LockiConfig getLockiConfig() {
        return lockiConfig;
    }

    public ChatFormatter getChatFormatter() {
        return chatFormatter;
    }

    public LuckPermsFacade getLuckPermsFacade() {
        return luckPermsFacade;
    }
    
    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }
    
    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
