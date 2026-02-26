package de.locki.lockiprefixes.placeholder;

import de.locki.lockiprefixes.config.LockiConfig;
import de.locki.lockiprefixes.lp.LuckPermsFacade;

import java.util.List;

/**
 * Handles built-in placeholder replacement.
 * Placeholders: {world}, {prefix}, {prefixes}, {name}, {displayname},
 *               {suffix}, {suffixes}, {username-color}, {message-color}
 */
public class BuiltInPlaceholders {

    private final LockiConfig config;
    private final LuckPermsFacade luckPermsFacade;

    public BuiltInPlaceholders(LockiConfig config, LuckPermsFacade luckPermsFacade) {
        this.config = config;
        this.luckPermsFacade = luckPermsFacade;
    }

    /**
     * Replaces all built-in placeholders in the format string.
     *
     * @param format      The format string containing placeholders
     * @param playerData  The player data holder
     * @return The format string with placeholders replaced
     */
    public String replace(String format, PlayerData playerData) {
        if (format == null) {
            return "";
        }

        String result = format;

        // {world}
        result = result.replace("{world}", playerData.getWorld() != null ? playerData.getWorld() : "");

        // {name}
        result = result.replace("{name}", playerData.getName() != null ? playerData.getName() : "");

        // {user} alias for {name}
        result = result.replace("{user}", playerData.getName() != null ? playerData.getName() : "");

        // {displayname}
        result = result.replace("{displayname}", playerData.getDisplayName() != null ? playerData.getDisplayName() : playerData.getName());

        // {prefix} - player meta override first, then LuckPerms prefix
        String prefix = playerData.getMetaValue("prefix");
        if (prefix == null || prefix.isEmpty()) {
            prefix = playerData.getPrefix();
        }
        result = replacePrefixPlaceholder(result, prefix);

        // {prefixes} - all prefixes sorted by priority, joined
        List<String> prefixes = playerData.getPrefixes();
        String prefixesJoined = prefixes != null ? String.join(config.getPrefixSeparator(), prefixes) : "";
        result = result.replace("{prefixes}", prefixesJoined);

        // {suffix} - player meta override first, then LuckPerms suffix
        String suffix = playerData.getMetaValue("suffix");
        if (suffix == null || suffix.isEmpty()) {
            suffix = playerData.getSuffix();
        }
        result = result.replace("{suffix}", suffix != null ? suffix : "");

        // {suffixes} - all suffixes sorted by priority, joined
        List<String> suffixes = playerData.getSuffixes();
        String suffixesJoined = suffixes != null ? String.join(config.getSuffixSeparator(), suffixes) : "";
        result = result.replace("{suffixes}", suffixesJoined);

        // {username-color} - from group config or player meta or default
        String usernameColor = resolveUsernameColor(playerData);
        result = result.replace("{username-color}", usernameColor);

        // {message-color} - from group config or player meta or default
        String messageColor = resolveMessageColor(playerData);
        result = result.replace("{message-color}", messageColor);

        return result;
    }

    private String replacePrefixPlaceholder(String format, String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            return format.replace("{prefix}", prefix);
        }

        String cleaned = format;
        cleaned = cleaned.replaceAll("\\{prefix\\}\\s*(?:(?:[&§][0-9A-FK-ORXa-fk-orx])+\\s*)?\\|\\s*", "");
        cleaned = cleaned.replace("{prefix}", "");
        return cleaned;
    }

    /**
     * Resolves the username color for a player.
     * Priority: Player meta > Group config > Default config
     */
    private String resolveUsernameColor(PlayerData playerData) {
        // Check player meta first
        String metaColor = playerData.getMetaValue("username-color");
        if (metaColor != null && !metaColor.isEmpty()) {
            return metaColor;
        }

        // Check group format
        String primaryGroup = playerData.getPrimaryGroup();
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = config.getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getUsernameColor() != null) {
                return groupFormat.getUsernameColor();
            }
        }

        // Default
        return config.getDefaultUsernameColor();
    }

    /**
     * Resolves the message color for a player.
     * Priority: Player meta > Group config > Default config
     */
    private String resolveMessageColor(PlayerData playerData) {
        // Check player meta first
        String metaColor = playerData.getMetaValue("message-color");
        if (metaColor != null && !metaColor.isEmpty()) {
            return metaColor;
        }

        // Check group format
        String primaryGroup = playerData.getPrimaryGroup();
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = config.getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getMessageColor() != null) {
                return groupFormat.getMessageColor();
            }
        }

        // Default
        return config.getDefaultMessageColor();
    }
}
