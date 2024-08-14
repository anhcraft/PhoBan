package dev.anhcraft.phoban.tasks;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FreeTicketTask extends BukkitRunnable {
    private final PhoBan plugin;

    public FreeTicketTask(PhoBan plugin) {
        this.plugin = plugin;
    }

    public void checkTicket(Player player) {
        synchronized (this) {
            check(player);
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                check(player);
            }
        }
    }

    private void check(Player player) {
        PlayerData pd = plugin.playerDataManager.getData(player);
        if (pd.getLastFreeTicketTime() + plugin.mainConfig.freeTicketEvery * 1000L < System.currentTimeMillis()) {
            pd.addTicket(plugin.mainConfig.freeTicketAmount);
            pd.setLastFreeTicketTime(System.currentTimeMillis() + plugin.mainConfig.freeTicketEvery * 1000L);
            Placeholder.create().add("amount", plugin.mainConfig.freeTicketAmount).message(player, plugin.messageConfig.freeTicketReceived);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        }
    }
}
