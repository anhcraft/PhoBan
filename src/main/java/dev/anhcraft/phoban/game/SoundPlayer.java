package dev.anhcraft.phoban.game;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.util.SoundPlayRule;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;

public class SoundPlayer {
    private final PriorityQueue<PlayTask> tasks = new PriorityQueue<>(Comparator.comparingInt(PlayTask::getNextPlayTick));

    public void schedule(SoundPlayRule rule) {
        PhoBan.instance.debug(1, "Scheduling task for %s", rule);
        tasks.add(new PlayTask(rule));
    }

    public void asyncTickPerSec(Room room) {
        for (Iterator<PlayTask> it = tasks.iterator(); it.hasNext(); ) {
            PlayTask task = it.next();
            if (room.getTimeCounter() < task.nextPlayTick) break;
            PhoBan.instance.debug(1, "Executing task for %s", task.rule);
            if (!task.spawnAndSchedule(room)) {
                it.remove();
            }
        }
    }

    public static class PlayTask {
        private final SoundPlayRule rule;
        private int nextPlayTick;

        public PlayTask(SoundPlayRule rule) {
            this.rule = rule;
            this.nextPlayTick = rule.delay();
        }

        public SoundPlayRule getRule() {
            return rule;
        }

        public int getNextPlayTick() {
            return nextPlayTick;
        }

        public boolean spawnAndSchedule(Room room) {
            this.nextPlayTick = room.getTimeCounter() + rule.every();

            if (rule.location() != null) {
                Sound sound = rule.getVanillaType();

                if (sound == null) {
                    rule.location().getWorld().playSound(rule.location(), rule.type(), rule.volume(), rule.pitch());
                } else {
                    rule.location().getWorld().playSound(rule.location(), sound, rule.volume(), rule.pitch());
                }
            } else {
                Sound sound = rule.getVanillaType();

                if (sound == null) {
                    for (UUID player : room.getPlayers()) {
                        Player p = Bukkit.getPlayer(player);
                        if (p != null) p.playSound(p.getLocation(), rule.type(), rule.volume(), rule.pitch());
                    }
                } else {
                    for (UUID player : room.getPlayers()) {
                        Player p = Bukkit.getPlayer(player);
                        if (p != null) p.playSound(p.getLocation(), sound, rule.volume(), rule.pitch());
                    }
                }
            }

            return rule.isRepeatable();
        }
    }
}
