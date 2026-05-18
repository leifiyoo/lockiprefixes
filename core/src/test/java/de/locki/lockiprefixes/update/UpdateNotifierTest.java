package de.locki.lockiprefixes.update;

import org.bukkit.ChatColor;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class UpdateNotifierTest {

    @Test
    public void parsesLatestVersionAndFirstChangesFromChangelog() {
        String changelog = "{\n"
            + "  \"latestVersion\": \"3.0.2\",\n"
            + "  \"entries\": [\n"
            + "    {\n"
            + "      \"version\": \"3.0.2\",\n"
            + "      \"date\": \"2026-05-18\",\n"
            + "      \"changes\": [\n"
            + "        \"Added a cleaner update message.\",\n"
            + "        \"Only checks for updates on server startup.\",\n"
            + "        \"Kept the join notification cached.\",\n"
            + "        \"This fourth item is not shown.\"\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        UpdateNotifier.UpdateInfo info = UpdateNotifier.parseUpdateInfo(changelog);

        assertEquals("3.0.2", info.getVersion());
        assertEquals(Arrays.asList(
            "Added a cleaner update message.",
            "Only checks for updates on server startup.",
            "Kept the join notification cached."
        ), info.getHighlights());
    }

    @Test
    public void buildsCompactUpdateMessageWithHighlights() {
        UpdateNotifier.UpdateInfo info = new UpdateNotifier.UpdateInfo(
            "3.0.2",
            Arrays.asList("Cleaner update notice.", "Startup-only update checks.")
        );

        assertEquals(Arrays.asList(
            ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------------",
            ChatColor.GOLD.toString() + ChatColor.BOLD + "LockiPrefixes "
                + ChatColor.DARK_GRAY + "\u00BB "
                + ChatColor.YELLOW + "Update " + ChatColor.WHITE + "3.0.2 "
                + ChatColor.GRAY + "ist verfuegbar",
            ChatColor.GRAY + "Installiert: " + ChatColor.WHITE + "3.0.1"
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Neu: " + ChatColor.GREEN + "3.0.2",
            ChatColor.GRAY + "Neu:",
            ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + "Cleaner update notice.",
            ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + "Startup-only update checks.",
            ChatColor.GRAY + "Download: " + ChatColor.AQUA + "https://modrinth.com/plugin/lockiprefixes",
            ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------------"
        ), UpdateNotifier.buildUpdateMessage("3.0.1", info));
    }
}
