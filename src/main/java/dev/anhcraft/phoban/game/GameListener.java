package dev.anhcraft.phoban.game;

import dev.anhcraft.phoban.PhoBan;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

        Room room = plugin.gameManager.getRoom(player.getUniqueId());
        if (room == null) {
            return;
        }
        room.handleJoinRoom(player);
    }
}
