package de.locki.lockiprefixes;

import de.locki.lockiprefixes.chat.MidChatListener;
import de.locki.lockiprefixes.command.ReloadCommand;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.papi.LockiPrefixesExpansion;
import de.locki.lockiprefixes.placeholder.PlayerData;
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

    private static LockiPrefixesPlugin instance;

    private LockiConfig lockiConfig;
    private LuckPermsFacade luckPermsFacade;
    private ChatFormatter chatFormatter;
    private boolean supportsHex;

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

        // Register command
        getCommand("lockiprefixes").setExecutor(new ReloadCommand(this));

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LockiPrefixesExpansion(this, chatFormatter, this::createPlayerData).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("LockiPrefixes (Mid 1.13-1.16) enabled! Hex support: " + supportsHex);
    }

    @Override
    public void onDisable() {
        if (luckPermsFacade != null) {
            luckPermsFacade.clearCache();
        }
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
            // 1.16+ supports hex colors
            if (version.contains("1.16") || version.contains("1.17") || 
                version.contains("1.18") || version.contains("1.19") ||
                version.contains("1.20") || version.contains("1.21")) {
                return true;
            }
        } catch (Exception e) {
            getLogger().warning("Could not determine server version for hex support.");
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
