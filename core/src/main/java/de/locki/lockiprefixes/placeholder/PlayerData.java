package de.locki.lockiprefixes.placeholder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data holder for player information used in placeholder replacement.
 * This class is version-agnostic and populated by platform-specific code.
 */
public class PlayerData {

    private UUID uuid;
    private String name;
    private String displayName;
    private String world;
    private String server;

    // LuckPerms data
    private String primaryGroup;
    private String prefix;
    private String suffix;
    private List<String> prefixes;
    private List<String> suffixes;
    private Map<String, String> meta;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPrimaryGroup() {
        return primaryGroup;
    }

    public void setPrimaryGroup(String primaryGroup) {
        this.primaryGroup = primaryGroup;
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

    public List<String> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    public List<String> getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(List<String> suffixes) {
        this.suffixes = suffixes;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public String getMetaValue(String key) {
        if (meta == null) {
            return null;
        }
        return meta.get(key);
    }
}
