package de.locki.lockiprefixes.papi;

import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.placeholder.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

/**
 * PlaceholderAPI expansion for LockiPrefixes.
 * Provides placeholders: %lockiprefixes_world%, %lockiprefixes_prefix%, etc.
 */
public class LockiPrefixesExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ChatFormatter chatFormatter;
    private final Function<Player, PlayerData> playerDataProvider;

    public LockiPrefixesExpansion(Plugin plugin, ChatFormatter chatFormatter, Function<Player, PlayerData> playerDataProvider) {
        this.plugin = plugin;
        this.chatFormatter = chatFormatter;
        this.playerDataProvider = playerDataProvider;
    }

    @Override
    public String getIdentifier() {
        return "lockiprefixes";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() 
            ? "Locki" 
            : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        PlayerData playerData = playerDataProvider.apply(player);
        if (playerData == null) {
            return "";
        }

        // Handle placeholders
        switch (params.toLowerCase()) {
            case "world":
                return playerData.getWorld() != null ? playerData.getWorld() : "";

            case "name":
                return playerData.getName() != null ? playerData.getName() : "";

            case "displayname":
                return playerData.getDisplayName() != null ? playerData.getDisplayName() : playerData.getName();

            case "prefix":
                return playerData.getPrefix() != null ? playerData.getPrefix() : "";

            case "prefixes":
                return playerData.getPrefixes() != null 
                    ? String.join(chatFormatter.getConfig().getPrefixSeparator(), playerData.getPrefixes()) 
                    : "";

            case "suffix":
                return playerData.getSuffix() != null ? playerData.getSuffix() : "";

            case "suffixes":
                return playerData.getSuffixes() != null 
                    ? String.join(chatFormatter.getConfig().getSuffixSeparator(), playerData.getSuffixes()) 
                    : "";

            case "group":
            case "primarygroup":
                return playerData.getPrimaryGroup() != null ? playerData.getPrimaryGroup() : "";

            case "username-color":
            case "username_color":
                return resolveUsernameColor(playerData);

            case "message-color":
            case "message_color":
                return resolveMessageColor(playerData);

            case "chat":
                // Returns the formatted chat format (without message)
                return chatFormatter.formatCustom(
                    chatFormatter.getFormatResolver().resolveChatFormat(
                        playerData.getPrimaryGroup(),
                        playerData.getWorld(),
                        playerData.getServer()
                    ).replace("{message}", ""),
                    playerData
                );

            case "leaderboard":
                return chatFormatter.formatLeaderboard(playerData);

            case "formatted":
            case "formatted_name":
                // Returns: [Prefix] ColoredName [Suffix]
                String prefix = playerData.getPrefix() != null ? playerData.getPrefix() : "";
                String suffix = playerData.getSuffix() != null ? playerData.getSuffix() : "";
                String color = resolveUsernameColor(playerData);
                String name = playerData.getName() != null ? playerData.getName() : "";
                return de.locki.lockiprefixes.color.ColorParser.translateHex(prefix + color + name + suffix);

            default:
                // Check for leaderboard_<position> placeholder
                if (params.toLowerCase().startsWith("leaderboard_")) {
                    try {
                        int position = Integer.parseInt(params.substring(12));
                        return chatFormatter.formatLeaderboardPosition(playerData, position);
                    } catch (NumberFormatException ignored) {
                    }
                }
                // Check for meta_ prefix
                if (params.toLowerCase().startsWith("meta_")) {
                    String metaKey = params.substring(5);
                    String metaValue = playerData.getMetaValue(metaKey);
                    return metaValue != null ? metaValue : "";
                }
                return null;
        }
    }

    private String resolveUsernameColor(PlayerData playerData) {
        // Check player meta first
        String metaColor = playerData.getMetaValue("username-color");
        if (metaColor != null && !metaColor.isEmpty()) {
            return metaColor;
        }

        // Check group format
        String primaryGroup = playerData.getPrimaryGroup();
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = chatFormatter.getConfig().getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getUsernameColor() != null) {
                return groupFormat.getUsernameColor();
            }
        }

        return chatFormatter.getConfig().getDefaultUsernameColor();
    }

    private String resolveMessageColor(PlayerData playerData) {
        // Check player meta first
        String metaColor = playerData.getMetaValue("message-color");
        if (metaColor != null && !metaColor.isEmpty()) {
            return metaColor;
        }

        // Check group format
        String primaryGroup = playerData.getPrimaryGroup();
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = chatFormatter.getConfig().getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getMessageColor() != null) {
                return groupFormat.getMessageColor();
            }
        }

        return chatFormatter.getConfig().getDefaultMessageColor();
    }
}
