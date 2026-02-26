package de.locki.lockiprefixes;

import de.locki.lockiprefixes.chat.MidChatListener;
import de.locki.lockiprefixes.command.ReloadCommand;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.gui.PrefixChatInputListener;
import de.locki.lockiprefixes.gui.PrefixGuiListener;
import de.locki.lockiprefixes.gui.PrefixMenuManager;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.papi.LockiPrefixesExpansion;
import de.locki.lockiprefixes.placeholder.PlayerData;
import de.locki.lockiprefixes.update.UpdateNotifier;
import net.luckperms.api.LuckPerms;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LockiPrefixes Plugin - Mid version for Minecraft 1.13-1.16
 * Uses AsyncPlayerChatEvent with hex color support (1.16+).
 */
public class LockiPrefixesPlugin extends JavaPlugin {

    private static final String CHANGELOG_RAW_URL = "https://raw.githubusercontent.com/leifiyoo/lockiprefixes/main/CHANGELOG.json";
    private static final String CHANGELOG_PAGE_URL = "https://github.com/leifiyoo/lockiprefixes/blob/main/CHANGELOG.json";

    private static LockiPrefixesPlugin instance;

    private LockiConfig lockiConfig;
    private LuckPermsFacade luckPermsFacade;
    private ChatFormatter chatFormatter;
    private PrefixMenuManager prefixMenuManager;
    private boolean supportsHex;
    private UpdateNotifier updateNotifier;

    @Override
    public void onEnable() {
        instance = this;

        // Check server version for hex support
        supportsHex = checkHexSupport();

        // Save default config
        saveDefaultConfig();

        // Load configuration
        lockiConfig = new LockiConfig();
        lockiConfig.load(getConfig());

        int pluginId = 29406;
        new Metrics(this, pluginId);

        // Initialize LuckPerms
        LuckPerms luckPerms = loadLuckPerms();
        if (luckPerms == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        luckPermsFacade = new LuckPermsFacade(luckPerms);

        // Initialize formatter
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, supportsHex);

        // Register chat listener
        getServer().getPluginManager().registerEvents(
            new MidChatListener(this, chatFormatter, luckPermsFacade),
            this
        );

        // Initialize Prefix Manager GUI
        prefixMenuManager = new PrefixMenuManager(this, luckPermsFacade);
        getServer().getPluginManager().registerEvents(new PrefixGuiListener(prefixMenuManager), this);
        getServer().getPluginManager().registerEvents(new PrefixChatInputListener(prefixMenuManager), this);

        // Register commands
        ReloadCommand reloadCommand = new ReloadCommand(this);
        reloadCommand.setMenuManager(prefixMenuManager);
        org.bukkit.command.PluginCommand cmd = getCommand("lockiprefixes");
        if (cmd != null) {
            cmd.setExecutor(reloadCommand);
            cmd.setTabCompleter(reloadCommand);
        } else {
            getLogger().severe("Command 'lockiprefixes' not found in plugin.yml — skipping registration.");
        }
        if (getCommand("prefixmenu") != null) {
            getCommand("prefixmenu").setExecutor(reloadCommand);
        }

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LockiPrefixesExpansion(this, this::getChatFormatter, this::createPlayerData).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        updateNotifier = new UpdateNotifier(this, CHANGELOG_RAW_URL, CHANGELOG_PAGE_URL);
        updateNotifier.start();

        getLogger().info("LockiPrefixes (Mid 1.13-1.16) enabled! Hex support: " + supportsHex);
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

    private LuckPerms loadLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = 
            getServer().getServicesManager().getRegistration(LuckPerms.class);
        return provider != null ? provider.getProvider() : null;
    }

    private boolean checkHexSupport() {
        try {
            String version = getServer().getBukkitVersion();
            // Extract the minor version number (e.g. "1.16.5-R0.1-SNAPSHOT" -> 16)
            // Hex colors were introduced in MC 1.16; any future version also supports them.
            String[] parts = version.split("[-. ]");
            if (parts.length >= 2) {
                int minor = Integer.parseInt(parts[1]);
                return minor >= 16;
            }
        } catch (Exception e) {
            getLogger().warning("Could not determine server version for hex support: " + e.getMessage());
        }
        return false;
    }

    /**
     * Creates PlayerData for a player.
     */
    public PlayerData createPlayerData(Player player) {
        PlayerData data = new PlayerData();
        data.setUuid(player.getUniqueId());
        data.setName(player.getName());
        data.setDisplayName(player.getDisplayName());
        data.setWorld(player.getWorld().getName());

        luckPermsFacade.populatePlayerData(data);
        return data;
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reload() {
        reloadConfig();
        lockiConfig.load(getConfig());
        luckPermsFacade.clearCache();
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, supportsHex);
        getLogger().info("Configuration reloaded.");
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
}
