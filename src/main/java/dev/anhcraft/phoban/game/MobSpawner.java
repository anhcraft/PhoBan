package dev.anhcraft.phoban.game;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.util.MobSpawnRule;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;

public class MobSpawner {
    private final PriorityQueue<SpawnTask> tasks = new PriorityQueue<>(Comparator.comparingInt(SpawnTask::getNextSpawnTick));

    public void schedule(MobSpawnRule rule) {
        PhoBan.instance.debug(1, "Scheduling task for %s", rule);
        tasks.add(new SpawnTask(rule));
    }

    public void syncTickPerSec(Room room) {
        for (Iterator<SpawnTask> it = tasks.iterator(); it.hasNext(); ) {
            SpawnTask task = it.next();
            if (room.getTimeCounter() < task.nextSpawnTick) break;
            PhoBan.instance.debug(1, "Executing task for %s", task.rule);
            if (!task.spawnAndSchedule(room)) {
                it.remove();
            }
        }
    }

    public static class SpawnTask {
        private final MobSpawnRule rule;
        private int nextSpawnTick;

        public SpawnTask(MobSpawnRule rule) {
            this.rule = rule;
            this.nextSpawnTick = rule.delay();
        }

        public MobSpawnRule getRule() {
            return rule;
        }

        public int getNextSpawnTick() {
            return nextSpawnTick;
        }

        public boolean spawnAndSchedule(Room room) {
            this.nextSpawnTick = room.getTimeCounter() + rule.every();

            if (rule.location() != null) {
                spawnAt(rule.location());
            } else {
                for (UUID player : room.getPlayers()) {
                    Player p = Bukkit.getPlayer(player);
                    if (p == null) continue;
                    spawnAt(p.getLocation());
                }
            }

            return rule.isRepeatable();
        }

        private void spawnAt(Location location) {
            EntityType entityType = rule.getVanillaType();

            if (entityType == null) {
                try {
                    for (int i = 0; i < rule.amount(); i++) {
                        rule.getMythicBoss().spawn(location);
                    }
                } catch (InvalidMobTypeException ignored) {}
            } else {
                World w = location.getWorld();
                for (int i = 0; i < rule.amount(); i++) {
                    w.spawnEntity(location, entityType, CreatureSpawnEvent.SpawnReason.CUSTOM);
                }
            }
        }
    }
}
