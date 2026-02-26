package de.locki.lockiprefixes.gui;

import java.util.UUID;

/**
 * Represents an active chat input session for editing a prefix or suffix.
 * This is a version-agnostic state holder used across all plugin modules.
 */
public class ChatEditSession {

    public enum EditType {
        PREFIX, SUFFIX
    }

    private final UUID playerUuid;
    private final EditType editType;
    private String draft;
    private final long startTime;

    public ChatEditSession(UUID playerUuid, EditType editType, String initialValue) {
        this.playerUuid = playerUuid;
        this.editType = editType;
        this.draft = initialValue != null ? initialValue : "";
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public EditType getEditType() {
        return editType;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public long getStartTime() {
        return startTime;
    }

    /** Returns how long (ms) this session has been active. */
    public long getAge() {
        return System.currentTimeMillis() - startTime;
    }

    /** Sessions expire after 5 minutes of inactivity. */
    public boolean isExpired() {
        return getAge() > 5 * 60 * 1000;
    }
}
