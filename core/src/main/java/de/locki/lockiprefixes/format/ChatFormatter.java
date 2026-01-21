package de.locki.lockiprefixes.format;

import de.locki.lockiprefixes.color.ColorParser;
import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.BuiltInPlaceholders;
import de.locki.lockiprefixes.placeholder.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Main formatter that combines format resolution, placeholder replacement,
 * and color parsing to produce the final formatted string.
 */
public class ChatFormatter {

    private final LockiConfig config;
    private final FormatResolver formatResolver;
    private final BuiltInPlaceholders builtInPlaceholders;
    private final boolean supportsHex;
    private final boolean papiAvailable;

    /**
     * Creates a new ChatFormatter.
     *
     * @param config           The plugin configuration
     * @param luckPermsFacade  The LuckPerms facade
     * @param supportsHex      Whether hex colors are supported (1.16+)
     */
    public ChatFormatter(LockiConfig config, LuckPermsFacade luckPermsFacade, boolean supportsHex) {
        this.config = config;
        this.formatResolver = new FormatResolver(config);
        this.builtInPlaceholders = new BuiltInPlaceholders(config, luckPermsFacade);
        this.supportsHex = supportsHex;
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Parses PlaceholderAPI placeholders if available.
     */
    private String parsePapi(String text, PlayerData playerData) {
        if (!papiAvailable || playerData.getUuid() == null) {
            return text;
        }
        try {
            Player player = Bukkit.getPlayer(playerData.getUuid());
            if (player != null) {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            }
        } catch (Exception ignored) {
        }
        return text;
    }

    /**
     * Formats a chat message for a player.
     *
     * @param playerData The player data
     * @param message    The chat message
     * @return The formatted chat string
     */
    public String formatChat(PlayerData playerData, String message) {
        // Resolve format based on context
        String format = formatResolver.resolveChatFormat(
            playerData.getPrimaryGroup(),
            playerData.getWorld(),
            playerData.getServer()
        );

        // Replace built-in placeholders
        String result = builtInPlaceholders.replace(format, playerData);

        // Parse PlaceholderAPI placeholders
        result = parsePapi(result, playerData);

        // Replace {message} placeholder
        result = result.replace("{message}", message != null ? message : "");

        // Parse colors
        if (supportsHex) {
            result = ColorParser.translateHex(result);
        } else {
            result = ColorParser.stripHex(result);
            result = ColorParser.translateLegacy(result);
        }

        return result;
    }

    /**
     * Formats a leaderboard entry for a player.
     *
     * @param playerData The player data
     * @return The formatted leaderboard string
     */
    public String formatLeaderboard(PlayerData playerData) {
        // Resolve format based on context
        String format = formatResolver.resolveLeaderboardFormat(
            playerData.getPrimaryGroup(),
            playerData.getWorld(),
            playerData.getServer()
        );

        // Replace built-in placeholders
        String result = builtInPlaceholders.replace(format, playerData);

        // Parse PlaceholderAPI placeholders
        result = parsePapi(result, playerData);

        // Parse colors
        if (supportsHex) {
            result = ColorParser.translateHex(result);
        } else {
            result = ColorParser.stripHex(result);
            result = ColorParser.translateLegacy(result);
        }

        return result;
    }

    /**
     * Formats a leaderboard entry for a player at a specific position.
     * Uses position-specific format if defined, otherwise falls back to default.
     *
     * @param playerData The player data
     * @param position   The leaderboard position (1, 2, 3, etc.)
     * @return The formatted leaderboard string
     */
    public String formatLeaderboardPosition(PlayerData playerData, int position) {
        // Check for position-specific format
        String format = config.getLeaderboardPositionFormat(position);
        
        // Fall back to regular leaderboard format
        if (format == null) {
            return formatLeaderboard(playerData);
        }

        // Replace built-in placeholders
        String result = builtInPlaceholders.replace(format, playerData);

        // Parse PlaceholderAPI placeholders
        result = parsePapi(result, playerData);

        // Parse colors
        if (supportsHex) {
            result = ColorParser.translateHex(result);
        } else {
            result = ColorParser.stripHex(result);
            result = ColorParser.translateLegacy(result);
        }

        return result;
    }

    /**
     * Formats a custom format string for a player.
     * Useful for PlaceholderAPI integration.
     *
     * @param format     The format string
     * @param playerData The player data
     * @return The formatted string
     */
    public String formatCustom(String format, PlayerData playerData) {
        // Replace built-in placeholders
        String result = builtInPlaceholders.replace(format, playerData);

        // Parse colors
        if (supportsHex) {
            result = ColorParser.translateHex(result);
        } else {
            result = ColorParser.stripHex(result);
            result = ColorParser.translateLegacy(result);
        }

        return result;
    }

    public LockiConfig getConfig() {
        return config;
    }

    public FormatResolver getFormatResolver() {
        return formatResolver;
    }

    public BuiltInPlaceholders getBuiltInPlaceholders() {
        return builtInPlaceholders;
    }
}
