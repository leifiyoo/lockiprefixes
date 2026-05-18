package de.locki.lockiprefixes.update;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks remote changelog metadata for newer releases and notifies OPs.
 * Runs once on startup and reuses the cached result for join notifications.
 */
public class UpdateNotifier implements Listener {

    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/lockiprefixes";
    private static final int MAX_HIGHLIGHTS = 3;

    /** Pre-compiled patterns — never changes at runtime. */
    private static final Pattern LATEST_VERSION_PATTERN =
        Pattern.compile("\"latestVersion\"\\s*:\\s*\"(\\d+\\.\\d+\\.\\d+(?:[-+][\\w.-]+)?)\"");
    private static final Pattern ENTRIES_VERSION_PATTERN =
        Pattern.compile("\"version\"\\s*:\\s*\"(\\d+\\.\\d+\\.\\d+(?:[-+][\\w.-]+)?)\"");
    private static final Pattern CHANGES_BLOCK_PATTERN =
        Pattern.compile("\"changes\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern JSON_STRING_PATTERN =
        Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private final JavaPlugin plugin;
    private final String remoteChangelogRawUrl;
    private final String changelogPageUrl;

    private volatile boolean updateAvailable;
    private volatile UpdateInfo latestUpdateInfo;

    public UpdateNotifier(JavaPlugin plugin, String remoteChangelogRawUrl, String changelogPageUrl) {
        this.plugin = plugin;
        this.remoteChangelogRawUrl = remoteChangelogRawUrl;
        this.changelogPageUrl = changelogPageUrl;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        runAsyncCheck(true);
    }

    public void stop() {
        // No repeating update task is scheduled. The method is kept for plugin shutdown.
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateAvailable || !event.getPlayer().isOp()) {
            return;
        }

        final Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    sendUpdateMessage(player);
                }
            }
        }, 40L);
    }

    private void runAsyncCheck(final boolean startup) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                checkAndNotify(startup);
            }
        });
    }

    private void checkAndNotify(boolean startup) {
        UpdateInfo remoteInfo = fetchUpdateInfo();
        if (remoteInfo == null || remoteInfo.getVersion().isEmpty()) {
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();
        String remoteVersion = remoteInfo.getVersion();
        boolean isNewer = compareVersions(remoteVersion, currentVersion) > 0;

        // Write details BEFORE updateAvailable so join handlers never see
        // updateAvailable=true with stale update text.
        latestUpdateInfo = remoteInfo;
        updateAvailable = isNewer;

        if (!isNewer) {
            return;
        }

        plugin.getLogger().warning("Update available: " + currentVersion + " -> " + remoteVersion);

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                notifyOnlineOps();
            }
        });
    }

    private void notifyOnlineOps() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.isOp()) {
                sendUpdateMessage(online);
            }
        }
    }

    private void sendUpdateMessage(Player player) {
        UpdateInfo info = latestUpdateInfo;
        if (info == null) {
            return;
        }

        for (String line : buildUpdateMessage(plugin.getDescription().getVersion(), info)) {
            player.sendMessage(line);
        }
    }

    private UpdateInfo fetchUpdateInfo() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(remoteChangelogRawUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "LockiPrefixes-UpdateChecker");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }

            return parseUpdateInfo(content.toString());
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static UpdateInfo parseUpdateInfo(String changelogContent) {
        String version = extractVersion(changelogContent);
        if (version == null || version.isEmpty()) {
            return null;
        }

        return new UpdateInfo(version, extractHighlights(changelogContent));
    }

    static List<String> buildUpdateMessage(String currentVersion, UpdateInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "LockiPrefixes "
            + ChatColor.DARK_GRAY + "\u00BB "
            + ChatColor.YELLOW + "Update " + ChatColor.WHITE + info.getVersion()
            + " " + ChatColor.GRAY + "ist verfuegbar");
        lines.add(ChatColor.GRAY + "Installiert: " + ChatColor.WHITE + currentVersion
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Neu: " + ChatColor.GREEN + info.getVersion());

        if (!info.getHighlights().isEmpty()) {
            lines.add(ChatColor.GRAY + "Neu:");
            for (String highlight : info.getHighlights()) {
                lines.add(ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + highlight);
            }
        } else {
            lines.add(ChatColor.GRAY + "Neu: " + ChatColor.WHITE + "Details stehen im Changelog.");
        }

        lines.add(ChatColor.GRAY + "Download: " + ChatColor.AQUA + DOWNLOAD_URL);
        lines.add(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
        return lines;
    }

    private static String extractVersion(String changelogContent) {
        Matcher latestMatcher = LATEST_VERSION_PATTERN.matcher(changelogContent);
        if (latestMatcher.find()) {
            return latestMatcher.group(1);
        }

        Matcher entriesMatcher = ENTRIES_VERSION_PATTERN.matcher(changelogContent);
        if (entriesMatcher.find()) {
            return entriesMatcher.group(1);
        }

        return null;
    }

    private static List<String> extractHighlights(String changelogContent) {
        Matcher blockMatcher = CHANGES_BLOCK_PATTERN.matcher(changelogContent);
        if (!blockMatcher.find()) {
            return Collections.emptyList();
        }

        List<String> highlights = new ArrayList<>();
        Matcher itemMatcher = JSON_STRING_PATTERN.matcher(blockMatcher.group(1));
        while (itemMatcher.find() && highlights.size() < MAX_HIGHLIGHTS) {
            String value = unescapeJsonString(itemMatcher.group(1)).trim();
            if (!value.isEmpty()) {
                highlights.add(value);
            }
        }

        return highlights;
    }

    private static String unescapeJsonString(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append(c);
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    default:
                        result.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private int compareVersions(String left, String right) {
        int[] a = toNumericVersion(left);
        int[] b = toNumericVersion(right);

        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int leftPart = i < a.length ? a[i] : 0;
            int rightPart = i < b.length ? b[i] : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private int[] toNumericVersion(String version) {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        String[] dotParts = normalized.split("\\.");
        int max = Math.min(dotParts.length, 4);
        int[] numbers = new int[max];

        for (int i = 0; i < max; i++) {
            numbers[i] = parseLeadingNumber(dotParts[i]);
        }

        return numbers;
    }

    private int parseLeadingNumber(String token) {
        if (token == null || token.isEmpty()) {
            return 0;
        }

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }

        if (digits.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static final class UpdateInfo {
        private final String version;
        private final List<String> highlights;

        UpdateInfo(String version, List<String> highlights) {
            this.version = version == null ? "" : version;
            this.highlights = highlights == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(highlights));
        }

        String getVersion() {
            return version;
        }

        List<String> getHighlights() {
            return highlights;
        }
    }
}
