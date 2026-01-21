package de.locki.lockiprefixes.tablist;

import de.locki.lockiprefixes.LockiPrefixesPlugin;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the TAB list (player list) formatting.
 * Features:
 * - Automatic prefix/suffix display
 * - Sorting by rank priority
 * - Auto-update when rank changes
 * - Animated gradients
 */
public class TablistManager implements Listener {

    private final LockiPrefixesPlugin plugin;
    private final ChatFormatter chatFormatter;
    private final LuckPermsFacade luckPermsFacade;
    private final LockiConfig config;
    
    // Animation state
    private int animationFrame = 0;
    private boolean animationEnabled = false;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask animationTask = null;
    
    // Track player teams for sorting
    private final Map<UUID, String> playerTeams = new HashMap<>();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public TablistManager(LockiPrefixesPlugin plugin, ChatFormatter chatFormatter, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.chatFormatter = chatFormatter;
        this.luckPermsFacade = luckPermsFacade;
        this.config = chatFormatter.getConfig();
        
        // Register LuckPerms listener for rank changes
        registerLuckPermsListener();
        
        // Check if animation is enabled
        startAnimationIfEnabled();
    }

    /**
     * Register listener for LuckPerms rank changes.
     */
    private void registerLuckPermsListener() {
        try {
            LuckPerms lp = luckPermsFacade.getLuckPerms();
            if (lp != null) {
                EventBus eventBus = lp.getEventBus();
                eventBus.subscribe(plugin, UserDataRecalculateEvent.class, event -> {
                    // Run on main thread
                    Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
                        if (player != null && player.isOnline()) {
                            updatePlayer(player);
                        }
                    });
                });
                plugin.getLogger().info("LuckPerms rank change listener registered.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register LuckPerms listener: " + e.getMessage());
        }
    }

    /**
     * Start animation task if enabled in config.
     */
    public void startAnimationIfEnabled() {
        // Check config for animation
        animationEnabled = plugin.getConfig().getBoolean("tablist.animation.enabled", false);
        int speed = plugin.getConfig().getInt("tablist.animation.speed", 5); // ticks between frames
        
        if (animationEnabled && animationTask == null) {
            animationTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                animationFrame++;
                if (animationFrame > 360) animationFrame = 0;
                updateAll();
            }, speed, speed);
            plugin.getLogger().info("Tablist animation started.");
        }
    }

    /**
     * Stop animation task.
     */
    public void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    /**
     * Update tablist for a player when they join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Small delay to ensure LuckPerms data is loaded
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            updatePlayer(event.getPlayer());
            // Also update for all other players (for sorting)
            updateAll();
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerTeams.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Updates the tablist name for a single player.
     */
    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (chatFormatter == null || luckPermsFacade == null) return;

        PlayerData playerData = createPlayerData(player);
        
        String formatted;
        
        // Apply gradient animation if enabled for this group
        String group = playerData.getPrimaryGroup();
        String gradientConfig = group != null ? 
            plugin.getConfig().getString("tablist.animation.groups." + group.toLowerCase()) : null;
        
        if (animationEnabled && gradientConfig != null) {
            // Use animated gradient
            formatted = applyAnimatedGradient(null, playerData);
        } else {
            // Use normal format
            formatted = chatFormatter.formatLeaderboard(playerData);
        }
        
        Component component = LEGACY_SERIALIZER.deserialize(formatted);
        player.playerListName(component);
        
        // Update sorting
        updatePlayerSorting(player, playerData);
    }

    /**
     * Updates player sorting in the tablist.
     * Uses scoreboard teams to sort players by rank priority.
     */
    private void updatePlayerSorting(Player player, PlayerData playerData) {
        if (!plugin.getConfig().getBoolean("tablist.sorting.enabled", true)) {
            return;
        }
        
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Get priority for this player's group
        int priority = 999; // Default low priority
        String group = playerData.getPrimaryGroup();
        if (group != null) {
            LockiConfig.GroupFormat groupFormat = config.getGroupFormat(group);
            if (groupFormat != null) {
                priority = 999 - groupFormat.getPriority(); // Invert so higher priority = lower team name
            }
        }
        
        // Create team name that sorts correctly (lower = higher in list)
        String teamName = String.format("%03d_%s", priority, player.getName());
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        // Remove from old team
        String oldTeam = playerTeams.get(player.getUniqueId());
        if (oldTeam != null && !oldTeam.equals(teamName)) {
            Team old = scoreboard.getTeam(oldTeam);
            if (old != null) {
                old.removeEntry(player.getName());
                if (old.getEntries().isEmpty()) {
                    old.unregister();
                }
            }
        }
        
        // Add to new team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.addEntry(player.getName());
        playerTeams.put(player.getUniqueId(), teamName);
    }

    /**
     * Apply animated gradient to the RANK TAG only.
     * Format: "Owner | Steve" with gradient on "Owner"
     */
    private String applyAnimatedGradient(String text, PlayerData playerData) {
        String group = playerData.getPrimaryGroup();
        if (group == null) return text;
        
        // Check if this group has animation
        String gradientConfig = plugin.getConfig().getString("tablist.animation.groups." + group.toLowerCase());
        if (gradientConfig == null) return text;
        
        // Get rank tag from config (e.g. "Owner")
        String rankTag = plugin.getConfig().getString("groups." + group.toLowerCase() + ".rank-tag");
        if (rankTag == null || rankTag.isEmpty()) {
            rankTag = group; // Use group name as fallback
        }
        
        // Parse gradient colors
        String[] colors = gradientConfig.split(",");
        if (colors.length < 2) return text;
        
        // Create animated gradient for the RANK TAG with BOLD
        String gradientTag = createAnimatedGradient(rankTag, colors, animationFrame, true);
        
        // Build: gradient rank tag + gray | + white name
        String name = playerData.getName();
        return gradientTag + " §7| §f" + name;
    }

    /**
     * Create an animated gradient text.
     * Returns text with proper §x§R§R§G§G§B§B color codes.
     */
    private String createAnimatedGradient(String text, String[] colors, int frame, boolean bold) {
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        // Animation offset based on frame (cycles through colors)
        float animationOffset = (frame % 100) / 100.0f;
        
        for (int i = 0; i < length; i++) {
            // Calculate position in gradient with animation offset
            float position = (float) i / Math.max(length - 1, 1);
            float animatedPosition = (position + animationOffset) % 1.0f;
            
            // Get color at this position (returns §x§R§R§G§G§B§B format)
            String color = getGradientColor(colors, animatedPosition);
            result.append(color);
            if (bold) result.append("§l"); // Add bold
            result.append(text.charAt(i));
        }
        
        return result.toString();
    }
    
    // Overload for backwards compatibility
    private String createAnimatedGradient(String text, String[] colors, int frame) {
        return createAnimatedGradient(text, colors, frame, false);
    }

    /**
     * Get color at a specific position in a gradient.
     */
    private String getGradientColor(String[] colors, float position) {
        int segments = colors.length - 1;
        float segmentSize = 1.0f / segments;
        int segment = (int) (position / segmentSize);
        if (segment >= segments) segment = segments - 1;
        
        float segmentPosition = (position - (segment * segmentSize)) / segmentSize;
        
        String color1 = colors[segment].trim();
        String color2 = colors[segment + 1].trim();
        
        return interpolateColor(color1, color2, segmentPosition);
    }

    /**
     * Interpolate between two hex colors.
     * Returns Minecraft format: §x§R§R§G§G§B§B
     */
    private String interpolateColor(String hex1, String hex2, float ratio) {
        // Remove # if present
        hex1 = hex1.replace("#", "").replace("&#", "").trim();
        hex2 = hex2.replace("#", "").replace("&#", "").trim();
        
        try {
            int r1 = Integer.parseInt(hex1.substring(0, 2), 16);
            int g1 = Integer.parseInt(hex1.substring(2, 4), 16);
            int b1 = Integer.parseInt(hex1.substring(4, 6), 16);
            
            int r2 = Integer.parseInt(hex2.substring(0, 2), 16);
            int g2 = Integer.parseInt(hex2.substring(2, 4), 16);
            int b2 = Integer.parseInt(hex2.substring(4, 6), 16);
            
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            
            // Convert to Minecraft format: §x§R§R§G§G§B§B
            String hex = String.format("%02X%02X%02X", r, g, b);
            StringBuilder mc = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                mc.append("§").append(c);
            }
            return mc.toString();
        } catch (Exception e) {
            return "§f"; // White fallback
        }
    }

    /**
     * Updates the tablist for all online players.
     */
    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private PlayerData createPlayerData(Player player) {
        PlayerData data = new PlayerData();
        data.setUuid(player.getUniqueId());
        data.setName(player.getName());
        data.setDisplayName(PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        data.setWorld(player.getWorld().getName());
        luckPermsFacade.populatePlayerData(data);
        return data;
    }
}
