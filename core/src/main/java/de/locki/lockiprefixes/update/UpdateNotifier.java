package de.locki.lockiprefixes.update;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks remote changelog metadata for newer releases and notifies OPs.
 * Runs once on startup and then once every 24 hours.
 */
public class UpdateNotifier implements Listener {

    private static final long DAILY_INTERVAL_TICKS = 20L * 60L * 60L * 24L;

    private final JavaPlugin plugin;
    private final String remoteChangelogRawUrl;
    private final String changelogPageUrl;

    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private BukkitTask dailyTask;

    public UpdateNotifier(JavaPlugin plugin, String remoteChangelogRawUrl, String changelogPageUrl) {
        this.plugin = plugin;
        this.remoteChangelogRawUrl = remoteChangelogRawUrl;
        this.changelogPageUrl = changelogPageUrl;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        runAsyncCheck(true);

        dailyTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            new Runnable() {
                @Override
                public void run() {
                    checkAndNotify(false);
                }
            },
            DAILY_INTERVAL_TICKS,
            DAILY_INTERVAL_TICKS
        );
    }

    public void stop() {
        if (dailyTask != null) {
            dailyTask.cancel();
            dailyTask = null;
        }
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
        String remoteVersion = fetchLatestVersion();
        if (remoteVersion == null || remoteVersion.isEmpty()) {
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();
        boolean isNewer = compareVersions(remoteVersion, currentVersion) > 0;

        updateAvailable = isNewer;
        latestVersion = remoteVersion;

        if (!isNewer) {
            return;
        }

        plugin.getLogger().warning("Update available: " + currentVersion + " -> " + remoteVersion);

        if (startup) {
            plugin.getLogger().warning("Changelog: " + changelogPageUrl);
        }

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
        String currentVersion = plugin.getDescription().getVersion();
        player.sendMessage(ChatColor.GOLD + "[LockiPrefixes] " + ChatColor.YELLOW + "Update available: "
            + ChatColor.AQUA + latestVersion + ChatColor.GRAY + " (current: " + currentVersion + ")");
        player.sendMessage(ChatColor.GOLD + "[LockiPrefixes] " + ChatColor.GRAY + "Changelog: "
            + ChatColor.WHITE + changelogPageUrl);
    }

    private String fetchLatestVersion() {
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
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            reader.close();

            return extractVersion(content.toString());
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractVersion(String changelogContent) {
        Pattern latestPattern = Pattern.compile("\"latestVersion\"\\s*:\\s*\"(\\d+\\.\\d+\\.\\d+(?:[-+][\\w.-]+)?)\"");
        Matcher latestMatcher = latestPattern.matcher(changelogContent);
        if (latestMatcher.find()) {
            return latestMatcher.group(1);
        }

        Pattern entriesPattern = Pattern.compile("\"version\"\\s*:\\s*\"(\\d+\\.\\d+\\.\\d+(?:[-+][\\w.-]+)?)\"");
        Matcher entriesMatcher = entriesPattern.matcher(changelogContent);
        if (entriesMatcher.find()) {
            return entriesMatcher.group(1);
        }

        return null;
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

        String[] dotParts = normalized.split("\\\\.");
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
}
