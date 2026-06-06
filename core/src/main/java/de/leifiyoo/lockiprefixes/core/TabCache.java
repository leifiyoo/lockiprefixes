package de.leifiyoo.lockiprefixes.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Tracks player prefix and display name state for change detection.
 * Eliminates redundant TAB list updates by caching player information.
 */
public class TabCache {
    
    private static final class PlayerState {
        String prefix;
        String displayName;
        String customName;
        
        PlayerState(String prefix, String displayName, String customName) {
            this.prefix = prefix;
            this.displayName = displayName;
            this.customName = customName;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PlayerState)) {
                return false;
            }
            PlayerState other = (PlayerState) obj;
            return safeEquals(this.prefix, other.prefix) &&
                   safeEquals(this.displayName, other.displayName) &&
                   safeEquals(this.customName, other.customName);
        }
        
        @Override
        public int hashCode() {
            return (prefix != null ? prefix.hashCode() : 0) ^
                   (displayName != null ? displayName.hashCode() : 0) ^
                   (customName != null ? customName.hashCode() : 0);
        }
    }
    
    private final Map<UUID, PlayerState> cache = new HashMap<>();
    
    /**
     * Checks if player state has changed and updates cache.
     * Returns true only if actual changes were detected.
     */
    public boolean hasChanged(Player player, String prefix, String displayName, String customName) {
        UUID uuid = player.getUniqueId();
        PlayerState newState = new PlayerState(prefix, displayName, customName);
        PlayerState oldState = cache.get(uuid);
        
        if (oldState == null || !oldState.equals(newState)) {
            cache.put(uuid, newState);
            return true;
        }
        return false;
    }
    
    /**
     * Removes player from cache (e.g., when player quits)
     */
    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
    
    /**
     * Clears all cached player states
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Gets the size of the cache
     */
    public int size() {
        return cache.size();
    }
    
    private static boolean safeEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}
