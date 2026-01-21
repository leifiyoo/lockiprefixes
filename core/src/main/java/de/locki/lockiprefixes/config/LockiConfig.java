package de.locki.lockiprefixes.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Configuration holder for LockiPrefixes.
 * Mirrors LPChat-style configuration structure.
 */
public class LockiConfig {

    // Default formats
    private String defaultChatFormat;
    private String defaultLeaderboardFormat;

    // Group-specific formats (group name -> format)
    private final Map<String, GroupFormat> groupFormats = new HashMap<>();

    // World-specific formats (world name -> format)
    private final Map<String, String> worldChatFormats = new HashMap<>();
    private final Map<String, String> worldLeaderboardFormats = new HashMap<>();

    // Server-specific formats (server name -> format)
    private final Map<String, String> serverChatFormats = new HashMap<>();
    private final Map<String, String> serverLeaderboardFormats = new HashMap<>();

    // Leaderboard position-specific formats (position -> format)
    private final Map<Integer, String> leaderboardPositionFormats = new HashMap<>();

    // Placeholder separator for {prefixes} and {suffixes}
    private String prefixSeparator = "";
    private String suffixSeparator = "";

    // Default colors
    private String defaultUsernameColor = "&f";
    private String defaultMessageColor = "&f";

    public void load(FileConfiguration config) {
        // Chat formats
        defaultChatFormat = config.getString("chat.format", "{prefix}{username-color}{name}{suffix}&r: {message-color}{message}");
        
        // Tablist/Leaderboard format (check both old and new config keys)
        defaultLeaderboardFormat = config.getString("tablist.format", 
            config.getString("leaderboard.format", "{prefix}{username-color}{name}{suffix}"));

        // Leaderboard position formats
        leaderboardPositionFormats.clear();
        ConfigurationSection positionsSection = config.getConfigurationSection("leaderboard.positions");
        if (positionsSection != null) {
            for (String posKey : positionsSection.getKeys(false)) {
                try {
                    int position = Integer.parseInt(posKey);
                    leaderboardPositionFormats.put(position, positionsSection.getString(posKey));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Separators
        prefixSeparator = config.getString("settings.prefix-separator", "");
        suffixSeparator = config.getString("settings.suffix-separator", "");

        // Default colors
        defaultUsernameColor = config.getString("settings.default-username-color", "&f");
        defaultMessageColor = config.getString("settings.default-message-color", "&f");

        // Load group formats (check both "groups" and "group-formats" keys)
        groupFormats.clear();
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection == null) {
            groupsSection = config.getConfigurationSection("group-formats");
        }
        if (groupsSection != null) {
            for (String groupName : groupsSection.getKeys(false)) {
                ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                if (groupSection != null) {
                    GroupFormat format = new GroupFormat();
                    format.setChatFormat(groupSection.getString("chat-format"));
                    // Check both "tablist-format" and "leaderboard-format"
                    format.setLeaderboardFormat(groupSection.getString("tablist-format",
                        groupSection.getString("leaderboard-format")));
                    format.setPrefix(groupSection.getString("prefix"));
                    format.setSuffix(groupSection.getString("suffix"));
                    format.setUsernameColor(groupSection.getString("username-color"));
                    format.setMessageColor(groupSection.getString("message-color"));
                    format.setPriority(groupSection.getInt("priority", 0));
                    groupFormats.put(groupName.toLowerCase(), format);
                }
            }
        }

        // Load world-specific formats (check both "worlds" and "world-formats" keys)
        worldChatFormats.clear();
        worldLeaderboardFormats.clear();
        
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String world : worldsSection.getKeys(false)) {
                ConfigurationSection ws = worldsSection.getConfigurationSection(world);
                if (ws != null) {
                    String chatFmt = ws.getString("chat-format");
                    String tabFmt = ws.getString("tablist-format");
                    if (chatFmt != null) worldChatFormats.put(world.toLowerCase(), chatFmt);
                    if (tabFmt != null) worldLeaderboardFormats.put(world.toLowerCase(), tabFmt);
                }
            }
        }
        
        // Also check old format
        ConfigurationSection worldChatSection = config.getConfigurationSection("world-formats.chat");
        if (worldChatSection != null) {
            for (String world : worldChatSection.getKeys(false)) {
                worldChatFormats.put(world.toLowerCase(), worldChatSection.getString(world));
            }
        }
        ConfigurationSection worldLbSection = config.getConfigurationSection("world-formats.leaderboard");
        if (worldLbSection != null) {
            for (String world : worldLbSection.getKeys(false)) {
                worldLeaderboardFormats.put(world.toLowerCase(), worldLbSection.getString(world));
            }
        }

        // Load server-specific chat formats
        serverChatFormats.clear();
        ConfigurationSection serverChatSection = config.getConfigurationSection("server-formats.chat");
        if (serverChatSection != null) {
            for (String server : serverChatSection.getKeys(false)) {
                serverChatFormats.put(server.toLowerCase(), serverChatSection.getString(server));
            }
        }

        // Load server-specific leaderboard formats
        serverLeaderboardFormats.clear();
        ConfigurationSection serverLbSection = config.getConfigurationSection("server-formats.leaderboard");
        if (serverLbSection != null) {
            for (String server : serverLbSection.getKeys(false)) {
                serverLeaderboardFormats.put(server.toLowerCase(), serverLbSection.getString(server));
            }
        }
    }

    public String getDefaultChatFormat() {
        return defaultChatFormat;
    }

    public String getDefaultLeaderboardFormat() {
        return defaultLeaderboardFormat;
    }

    public Map<String, GroupFormat> getGroupFormats() {
        return groupFormats;
    }

    public GroupFormat getGroupFormat(String groupName) {
        return groupFormats.get(groupName.toLowerCase());
    }

    public String getWorldChatFormat(String world) {
        return worldChatFormats.get(world.toLowerCase());
    }

    public String getWorldLeaderboardFormat(String world) {
        return worldLeaderboardFormats.get(world.toLowerCase());
    }

    public String getServerChatFormat(String server) {
        return serverChatFormats.get(server.toLowerCase());
    }

    public String getServerLeaderboardFormat(String server) {
        return serverLeaderboardFormats.get(server.toLowerCase());
    }

    public String getPrefixSeparator() {
        return prefixSeparator;
    }

    public String getSuffixSeparator() {
        return suffixSeparator;
    }

    public String getDefaultUsernameColor() {
        return defaultUsernameColor;
    }

    public String getDefaultMessageColor() {
        return defaultMessageColor;
    }

    public String getLeaderboardPositionFormat(int position) {
        return leaderboardPositionFormats.get(position);
    }

    public Map<Integer, String> getLeaderboardPositionFormats() {
        return leaderboardPositionFormats;
    }

    /**
     * Represents a group-specific format configuration.
     */
    public static class GroupFormat {
        private String chatFormat;
        private String leaderboardFormat;
        private String prefix;
        private String suffix;
        private String usernameColor;
        private String messageColor;
        private int priority;

        public String getChatFormat() {
            return chatFormat;
        }

        public void setChatFormat(String chatFormat) {
            this.chatFormat = chatFormat;
        }

        public String getLeaderboardFormat() {
            return leaderboardFormat;
        }

        public void setLeaderboardFormat(String leaderboardFormat) {
            this.leaderboardFormat = leaderboardFormat;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public String getUsernameColor() {
            return usernameColor;
        }

        public void setUsernameColor(String usernameColor) {
            this.usernameColor = usernameColor;
        }

        public String getMessageColor() {
            return messageColor;
        }

        public void setMessageColor(String messageColor) {
            this.messageColor = messageColor;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }
}
