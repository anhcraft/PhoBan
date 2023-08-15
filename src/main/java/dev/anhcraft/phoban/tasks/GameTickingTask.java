package dev.anhcraft.phoban.tasks;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Room;
import org.bukkit.scheduler.BukkitRunnable;

public class GameTickingTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Room room : PhoBan.instance.gameManager.getActiveRooms()) {
            room.asyncTickPerSec();
        }
    }
}
