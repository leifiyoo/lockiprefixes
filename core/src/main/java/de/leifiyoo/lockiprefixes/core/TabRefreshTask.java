package de.leifiyoo.lockiprefixes.core;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Unified TAB list refresh task with change detection.
 * Only updates TAB entries when actual player data changes.
 * This significantly reduces CPU load compared to unconditional updates every tick.
 */
public abstract class TabRefreshTask implements Runnable {
    
    protected final Plugin plugin;
    protected final TabCache cache;
    protected final int refreshInterval;
    protected BukkitTask task;
    
    /**
     * Creates a new TabRefreshTask.
     * 
     * @param plugin The plugin instance
     * @param refreshInterval The refresh interval in ticks (default: 20)
     */
    public TabRefreshTask(Plugin plugin, int refreshInterval) {
        this.plugin = plugin;
        this.cache = new TabCache();
        this.refreshInterval = Math.max(1, refreshInterval); // Minimum 1 tick
    }
    
    /**
     * Starts the refresh task.
     */
    public void start() {
        if (task != null) {
            return; // Already running
        }
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, refreshInterval);
    }
    
    /**
     * Stops the refresh task.
     */
    public void stop() {
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task.getTaskId());
            task = null;
        }
    }
    
    /**
     * Checks if the task is running.
     */
    public boolean isRunning() {
        return task != null;
    }
    
    @Override
    public final void run() {
        List<? extends Player> players = (List<? extends Player>) Bukkit.getOnlinePlayers();
        for (Player player : players) {
            updatePlayerTab(player);
        }
    }
    
    /**
     * Updates a single player's TAB entry if changes are detected.
     * Subclasses should implement the actual TAB update logic here.
     */
    protected abstract void updatePlayerTab(Player player);
    
    /**
     * Gets the player prefix to use. Subclasses can override this.
     */
    protected String getPlayerPrefix(Player player) {
        return "";
    }
    
    /**
     * Gets the player display name to use. Subclasses can override this.
     */
    protected String getPlayerDisplayName(Player player) {
        return player.getName();
    }
    
    /**
     * Gets the player custom name. Subclasses can override this.
     */
    protected String getPlayerCustomName(Player player) {
        return null;
    }
    
    /**
     * Clears the cache (should be called on disable or reload).
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Invalidates a specific player's cache entry (call on player quit).
     */
    public void invalidatePlayer(java.util.UUID uuid) {
        cache.invalidate(uuid);
    }
}
