package de.locki.lockiprefixes.tablist;

import de.locki.lockiprefixes.format.ChatFormatter;
import de.locki.lockiprefixes.lp.LuckPermsFacade;
import de.locki.lockiprefixes.placeholder.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lightweight tablist name updater for legacy, mid, and modern modules.
 */
public class SimpleTablistManager implements Listener {

    private final JavaPlugin plugin;
    private ChatFormatter chatFormatter;
    private final LuckPermsFacade luckPermsFacade;

    public SimpleTablistManager(JavaPlugin plugin, ChatFormatter chatFormatter, LuckPermsFacade luckPermsFacade) {
        this.plugin = plugin;
        this.chatFormatter = chatFormatter;
        this.luckPermsFacade = luckPermsFacade;
        registerLuckPermsListener();
    }

    public void setChatFormatter(ChatFormatter chatFormatter) {
        this.chatFormatter = chatFormatter;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                updatePlayer(event.getPlayer());
            }
        }, 20L);
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private void registerLuckPermsListener() {
        try {
            LuckPerms luckPerms = luckPermsFacade.getLuckPerms();
            if (luckPerms == null) {
                return;
            }

            EventBus eventBus = luckPerms.getEventBus();
            eventBus.subscribe(plugin, UserDataRecalculateEvent.class, event -> {
                final Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
                if (player == null) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        updatePlayer(player);
                    }
                });
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register LuckPerms tablist listener: " + e.getMessage());
        }
    }

    private void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (chatFormatter == null) {
            return;
        }

        PlayerData data = new PlayerData();
        data.setUuid(player.getUniqueId());
        data.setName(player.getName());
        data.setDisplayName(player.getDisplayName());
        data.setWorld(player.getWorld().getName());
        luckPermsFacade.populatePlayerData(data);

        String formatted = chatFormatter.formatLeaderboard(data);
        try {
            player.setPlayerListName(formatted);
        } catch (IllegalArgumentException ignored) {
            player.setPlayerListName(trimVisibleName(ChatColor.stripColor(formatted), player.getName()));
        }
    }

    private String trimVisibleName(String value, String fallback) {
        String name = value == null || value.trim().isEmpty() ? fallback : value.trim();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
