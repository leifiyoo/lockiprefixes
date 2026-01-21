package de.locki.lockiprefixes.lp;

import de.locki.lockiprefixes.placeholder.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.util.*;
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
