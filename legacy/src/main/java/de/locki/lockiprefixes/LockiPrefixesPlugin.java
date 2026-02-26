package de.locki.lockiprefixes;

import de.locki.lockiprefixes.chat.LegacyChatListener;
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
 * LockiPrefixes Plugin - Legacy version for Minecraft 1.7-1.12
 * Uses AsyncPlayerChatEvent and legacy color codes only (no hex).
 */
public class LockiPrefixesPlugin extends JavaPlugin {

    private static final String CHANGELOG_RAW_URL = "https://raw.githubusercontent.com/leifiyoo/lockiprefixes/main/CHANGELOG.json";
    private static final String CHANGELOG_PAGE_URL = "https://github.com/leifiyoo/lockiprefixes/blob/main/CHANGELOG.json";

    private static LockiPrefixesPlugin instance;

    private LockiConfig lockiConfig;
    private LuckPermsFacade luckPermsFacade;
    private ChatFormatter chatFormatter;
    private PrefixMenuManager prefixMenuManager;
    private UpdateNotifier updateNotifier;

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

        // Initialize LuckPerms
        LuckPerms luckPerms = loadLuckPerms();
        if (luckPerms == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        luckPermsFacade = new LuckPermsFacade(luckPerms);

        // Initialize formatter (no hex support for legacy)
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, false);

        // Register chat listener
        getServer().getPluginManager().registerEvents(
            new LegacyChatListener(this, chatFormatter, luckPermsFacade),
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

        getLogger().info("LockiPrefixes (Legacy 1.7-1.12) enabled!");
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

    /**
     * Creates PlayerData for a player.
     * Used by PlaceholderAPI expansion.
     */
    public PlayerData createPlayerData(Player player) {
        PlayerData data = new PlayerData();
        data.setUuid(player.getUniqueId());
        data.setName(player.getName());
        data.setDisplayName(player.getDisplayName());
        data.setWorld(player.getWorld().getName());

        // Populate LuckPerms data
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
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, false);
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
