package de.locki.lockiprefixes.lp;

import de.locki.lockiprefixes.placeholder.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade for LuckPerms API interactions.
 * Provides caching and efficient data retrieval.
 */
public class LuckPermsFacade {

    private final LuckPerms luckPerms;
    private final Map<UUID, CachedPlayerData> cache = new ConcurrentHashMap<>();

    public LuckPermsFacade(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    /**
     * Populates player data with LuckPerms information.
     *
     * @param playerData The player data to populate
     */
    public void populatePlayerData(PlayerData playerData) {
        if (luckPerms == null || playerData.getUuid() == null) {
            return;
        }

        User user = luckPerms.getUserManager().getUser(playerData.getUuid());
        if (user == null) {
            return;
        }

        ContextManager contextManager = luckPerms.getContextManager();
        ImmutableContextSet contextSet = contextManager.getContext(user).orElse(contextManager.getStaticContext());
        QueryOptions queryOptions = contextManager.getQueryOptions(user).orElse(QueryOptions.defaultContextualOptions());

        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);

        // Primary group
        playerData.setPrimaryGroup(user.getPrimaryGroup());

        // Prefix and suffix
        playerData.setPrefix(metaData.getPrefix());
        playerData.setSuffix(metaData.getSuffix());

        // All prefixes sorted by priority (highest first)
        SortedMap<Integer, String> prefixMap = metaData.getPrefixes();
        List<String> prefixes = new ArrayList<>();
        if (prefixMap != null) {
            List<Integer> keys = new ArrayList<>(prefixMap.keySet());
            Collections.sort(keys, Collections.reverseOrder());
            for (Integer key : keys) {
                prefixes.add(prefixMap.get(key));
            }
        }
        playerData.setPrefixes(prefixes);

        // All suffixes sorted by priority (highest first)
        SortedMap<Integer, String> suffixMap = metaData.getSuffixes();
        List<String> suffixes = new ArrayList<>();
        if (suffixMap != null) {
            List<Integer> keys = new ArrayList<>(suffixMap.keySet());
            Collections.sort(keys, Collections.reverseOrder());
            for (Integer key : keys) {
                suffixes.add(suffixMap.get(key));
            }
        }
        playerData.setSuffixes(suffixes);

        // Meta values
        Map<String, String> meta = new HashMap<>();
        Map<String, List<String>> metaMulti = metaData.getMeta();
        if (metaMulti != null) {
            for (Map.Entry<String, List<String>> entry : metaMulti.entrySet()) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    meta.put(entry.getKey(), values.get(0));
                }
            }
        }
        playerData.setMeta(meta);

        // Server context
        for (net.luckperms.api.context.Context ctx : contextSet) {
            if ("server".equalsIgnoreCase(ctx.getKey())) {
                playerData.setServer(ctx.getValue());
                break;
            }
        }
    }

    /**
     * Invalidates the cache for a player.
     *
     * @param uuid The player's UUID
     */
    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Sets a custom prefix for a player at the given priority (player node).
     *
     * @param uuid     The player's UUID
     * @param prefix   The new prefix string (use null or empty to clear)
     * @param priority The priority for the prefix node (default 100)
     * @return A CompletableFuture that completes when the change is saved
     */
    public CompletableFuture<Void> setPlayerPrefix(UUID uuid, String prefix, int priority) {
        return luckPerms.getUserManager().modifyUser(uuid, user -> {
            // Remove all existing player-level prefix nodes
            user.data().clear(NodeType.PREFIX.predicate(n -> true));
            // Set new prefix if non-empty
            if (prefix != null && !prefix.isEmpty()) {
                user.data().add(PrefixNode.builder(prefix, priority).build());
            }
        });
    }

    /**
     * Sets a custom suffix for a player at the given priority (player node).
     *
     * @param uuid     The player's UUID
     * @param suffix   The new suffix string (use null or empty to clear)
     * @param priority The priority for the suffix node (default 100)
     * @return A CompletableFuture that completes when the change is saved
     */
    public CompletableFuture<Void> setPlayerSuffix(UUID uuid, String suffix, int priority) {
        return luckPerms.getUserManager().modifyUser(uuid, user -> {
            // Remove all existing player-level suffix nodes
            user.data().clear(NodeType.SUFFIX.predicate(n -> true));
            // Set new suffix if non-empty
            if (suffix != null && !suffix.isEmpty()) {
                user.data().add(SuffixNode.builder(suffix, priority).build());
            }
        });
    }

    /**
     * Sets (or clears) a player meta key.
     * This is used for plugin-level per-player overrides like "chat-format" or "prefix".
     *
     * @param uuid  The player's UUID
     * @param key   Meta key
     * @param value Meta value (null/empty clears)
     * @return A CompletableFuture that completes when the change is saved
     */
    public CompletableFuture<Void> setPlayerMeta(UUID uuid, String key, String value) {
        return luckPerms.getUserManager().modifyUser(uuid, user -> {
            user.data().clear(NodeType.META.predicate(node -> node.getMetaKey().equalsIgnoreCase(key)));
            if (value != null && !value.trim().isEmpty()) {
                user.data().add(MetaNode.builder(key, value).build());
            }
        });
    }

    /**
     * Creates a LuckPerms group and saves it to storage (persists across restarts).
     *
     * @param groupName Group name (automatically lowercased)
     * @return CompletableFuture that completes with the Group once created and saved
     */
    public CompletableFuture<Group> createGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String normalized = groupName.toLowerCase(Locale.ROOT);
        return luckPerms.getGroupManager().createAndLoadGroup(normalized)
            .thenCompose(group -> luckPerms.getGroupManager().saveGroup(group)
                .thenApply(v -> group));
    }

    /**
     * Checks if a LuckPerms group exists.
     *
     * @param groupName Group name
     * @return Future with true if group exists
     */
    public CompletableFuture<Boolean> groupExists(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String normalized = groupName.toLowerCase(Locale.ROOT);

        if (luckPerms.getGroupManager().getGroup(normalized) != null) {
            return CompletableFuture.completedFuture(true);
        }

        return luckPerms.getGroupManager().loadGroup(normalized)
            .thenApply(opt -> opt != null && opt.isPresent())
            .exceptionally(ex -> false);
    }

    /**
     * Retrieves all unique prefixes from all loaded groups.
     * Useful for listing available server prefixes.
     *
     * @return List of unique prefixes
     */
    public List<String> getAllGroupPrefixes() {
        Set<String> uniquePrefixes = new HashSet<>();
        // Iterate all loaded groups
        for (Group group : luckPerms.getGroupManager().getLoadedGroups()) { 
            // Get group's own data (not inherited context)
            CachedMetaData meta = group.getCachedData().getMetaData(QueryOptions.nonContextual());
            if (meta.getPrefix() != null) {
                uniquePrefixes.add(meta.getPrefix());
            }
            // Also check map if multiple prefixes exist on same group
            if (meta.getPrefixes() != null) {
                uniquePrefixes.addAll(meta.getPrefixes().values());
            }
        }
        // Also check if any users have prefixes? No, that's too much. Just groups.
        List<String> result = new ArrayList<>(uniquePrefixes);
        // Sort by length or content? Let's sort alphabetically for now.
        Collections.sort(result);
        return result;
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        cache.clear();
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    /**
     * Internal cached data holder.
     */
    private static class CachedPlayerData {
        String primaryGroup;
        String prefix;
        String suffix;
        List<String> prefixes;
        List<String> suffixes;
        Map<String, String> meta;
        long timestamp;
    }
}
