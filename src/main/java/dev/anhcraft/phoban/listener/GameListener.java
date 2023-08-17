package dev.anhcraft.phoban.listener;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.Placeholder;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class GameListener implements Listener {
    private final PhoBan plugin;

    public GameListener(PhoBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.gameManager.rejoinRoom(player);
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
}
