package dev.anhcraft.phoban.listener;

import dev.anhcraft.phoban.PhoBan;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.GameMode;
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
        player.setGameMode(GameMode.SURVIVAL);
        plugin.gameManager.rejoinRoom(player);
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
