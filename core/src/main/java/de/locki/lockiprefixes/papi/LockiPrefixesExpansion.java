package de.locki.lockiprefixes.papi;

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

        switch (params.toLowerCase()) {

            // %lockiprefixes_prefix%
            // The raw rank prefix (e.g. "&4&lOwner ")
            case "prefix":
                return playerData.getPrefix() != null ? playerData.getPrefix() : "";

            // %lockiprefixes_tablist%
            // Full tablist display: prefix + separator + name (reads tablist-format from config)
            // Use this in TAB groups.yml: customtabname: "%lockiprefixes_tablist%"
            case "tab":
            case "tablist":
                return chatFormatter.formatLeaderboard(playerData);

            // %lockiprefixes_name%
            // Prefix + name without any extra separator — prefix directly followed by the player name
            case "name":
                String pfx = playerData.getPrefix() != null ? playerData.getPrefix() : "";
                String playerName = playerData.getName() != null ? playerData.getName() : "";
                return de.locki.lockiprefixes.color.ColorParser.translateHex(pfx + playerName);

            default:
                return null;
        }
    }

}
