package dev.anhcraft.phoban.listener;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.Placeholder;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class GameListener implements Listener {
    private final PhoBan plugin;

    public GameListener(PhoBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.resetPlayerTime();
        player.resetPlayerWeather();
        plugin.gameManager.rejoinRoom(player);

        if (!plugin.mainConfig.freeTicketEnabled) return;

        plugin.sync(() -> {
            PlayerData pd = plugin.playerDataManager.getData(player);
            if (pd.getLastFreeTicketTime() + plugin.mainConfig.freeTicketEvery * 1000L < System.currentTimeMillis()) {
                pd.addTicket(plugin.mainConfig.freeTicketAmount);
                pd.setLastFreeTicketTime(System.currentTimeMillis() + plugin.mainConfig.freeTicketEvery * 1000L);
                Placeholder.create().add("amount", plugin.mainConfig.freeTicketAmount).message(player, plugin.messageConfig.freeTicketReceived);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
            }
        }, 40);
    }

    @EventHandler
    private void bossDeath(MythicMobDeathEvent event) {
        plugin.gameManager.handleBossDeath(event);
    }

    @EventHandler
    private void death(PlayerDeathEvent event) {
        plugin.gameManager.handlePlayerDeath(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void move(PlayerMoveEvent event) {
        if (!event.hasExplicitlyChangedBlock()) return;
        Room room = plugin.gameManager.getRoom(event.getPlayer().getUniqueId());
        if (room == null || !room.getSeparators().contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void teleport(PlayerTeleportEvent event) {
        if (!event.hasExplicitlyChangedBlock() || event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        Room room = plugin.gameManager.getRoom(event.getPlayer().getUniqueId());
        if (room != null)
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void damage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && plugin.gameManager.shouldBlockDamage(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        } else if (event.getEntity() instanceof Player && plugin.gameManager.shouldBlockDamage(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void damage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && plugin.gameManager.shouldBlockDamage(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void cmd(PlayerCommandPreprocessEvent event) {
        Room room = plugin.gameManager.getRoom(event.getPlayer().getUniqueId());
        if (room != null && event.getMessage().length() > 1) {
            String msg = event.getMessage().substring(1);
            for (String command : plugin.mainConfig.allowedCommands) {
                if (msg.startsWith(command)) {
                    return;
                }
            }
            plugin.msg(event.getPlayer(), plugin.messageConfig.commandBlocked);
            event.setCancelled(true);
        }
    }
}
