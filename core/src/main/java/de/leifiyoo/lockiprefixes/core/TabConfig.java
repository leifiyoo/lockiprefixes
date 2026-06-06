package de.leifiyoo.lockiprefixes.core;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration holder for TAB list refresh settings.
 * Provides a unified configuration interface for all modules.
 */
public class TabConfig {
    
    private int refreshInterval;
    
    /**
     * Creates a new TabConfig with default values.
     */
    public TabConfig() {
        this.refreshInterval = 20; // Default 20 ticks = 1 second
    }
    
    /**
     * Loads configuration from a ConfigurationSection.
     */
    public void load(ConfigurationSection section) {
        if (section != null) {
            this.refreshInterval = section.getInt("tablist-refresh-interval", 20);
        }
    }
    
    /**
     * Gets the TAB list refresh interval in ticks.
     * Default is 20 ticks (1 second).
     */
    public int getRefreshInterval() {
        return Math.max(1, refreshInterval);
    }
    
    /**
     * Sets the TAB list refresh interval in ticks.
     */
    public void setRefreshInterval(int ticks) {
        this.refreshInterval = Math.max(1, ticks);
    }
    
    /**
     * Returns a human-readable description of refresh rate.
     */
    public String getRefreshRateDescription() {
        double seconds = refreshInterval / 20.0;
        return String.format("%.2f seconds (%d ticks)", seconds, refreshInterval);
    }
}
