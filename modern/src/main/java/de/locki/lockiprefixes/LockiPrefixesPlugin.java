package de.locki.lockiprefixes;

import de.locki.lockiprefixes.chat.ModernChatListener;
import de.locki.lockiprefixes.command.ReloadCommand;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.papi.LockiPrefixesExpansion;
import de.locki.lockiprefixes.placeholder.PlayerData;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LockiPrefixes Plugin - Modern version for Minecraft 1.17-1.19
 * Uses AsyncPlayerChatEvent with full hex color support.
 */
public class LockiPrefixesPlugin extends JavaPlugin {

    private static LockiPrefixesPlugin instance;

    private LockiConfig lockiConfig;
    private LuckPermsFacade luckPermsFacade;
    private ChatFormatter chatFormatter;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load configuration
        lockiConfig = new LockiConfig();
        lockiConfig.load(getConfig());

        // Initialize LuckPerms
        LuckPerms luckPerms = loadLuckPerms();
        if (luckPerms == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        luckPermsFacade = new LuckPermsFacade(luckPerms);

        // Initialize formatter with hex support
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, true);

        // Register chat listener
        getServer().getPluginManager().registerEvents(
            new ModernChatListener(this, chatFormatter, luckPermsFacade),
            this
        );

        // Register command
        getCommand("lockiprefixes").setExecutor(new ReloadCommand(this));

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LockiPrefixesExpansion(this, chatFormatter, this::createPlayerData).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("LockiPrefixes (Modern 1.17-1.19) enabled!");
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
        chatFormatter = new ChatFormatter(lockiConfig, luckPermsFacade, true);
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
