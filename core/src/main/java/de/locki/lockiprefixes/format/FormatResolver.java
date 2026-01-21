package de.locki.lockiprefixes.format;

import de.locki.lockiprefixes.config.LockiConfig;

/**
 * Resolves the correct format string based on context (group, world, server).
 * Priority: group+context > group > context > default
 */
public class FormatResolver {

    private final LockiConfig config;

    public FormatResolver(LockiConfig config) {
        this.config = config;
    }

    /**
     * Resolves the chat format for a player based on their primary group, world, and server.
     *
     * @param primaryGroup The player's primary LuckPerms group
     * @param world        The player's current world name
     * @param server       The server context (from LuckPerms, may be null)
     * @return The resolved chat format string
     */
    public String resolveChatFormat(String primaryGroup, String world, String server) {
        // Priority 1: Group-specific format
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = config.getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getChatFormat() != null) {
                return groupFormat.getChatFormat();
            }
        }

        // Priority 2: Server-specific format
        if (server != null) {
            String serverFormat = config.getServerChatFormat(server);
            if (serverFormat != null) {
                return serverFormat;
            }
        }

        // Priority 3: World-specific format
        if (world != null) {
            String worldFormat = config.getWorldChatFormat(world);
            if (worldFormat != null) {
                return worldFormat;
            }
        }

        // Priority 4: Default format
        return config.getDefaultChatFormat();
    }

    /**
     * Resolves the leaderboard format for a player based on their primary group, world, and server.
     *
     * @param primaryGroup The player's primary LuckPerms group
     * @param world        The player's current world name
     * @param server       The server context (from LuckPerms, may be null)
     * @return The resolved leaderboard format string
     */
    public String resolveLeaderboardFormat(String primaryGroup, String world, String server) {
        // Priority 1: Group-specific format
        if (primaryGroup != null) {
            LockiConfig.GroupFormat groupFormat = config.getGroupFormat(primaryGroup);
            if (groupFormat != null && groupFormat.getLeaderboardFormat() != null) {
                return groupFormat.getLeaderboardFormat();
            }
        }

        // Priority 2: Server-specific format
        if (server != null) {
            String serverFormat = config.getServerLeaderboardFormat(server);
            if (serverFormat != null) {
                return serverFormat;
            }
        }

        // Priority 3: World-specific format
        if (world != null) {
            String worldFormat = config.getWorldLeaderboardFormat(world);
            if (worldFormat != null) {
                return worldFormat;
            }
        }

        // Priority 4: Default format
        return config.getDefaultLeaderboardFormat();
    }

    public LockiConfig getConfig() {
        return config;
    }
}
