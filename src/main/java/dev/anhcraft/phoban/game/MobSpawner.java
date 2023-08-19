package dev.anhcraft.phoban.game;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.util.MobOptions;
import dev.anhcraft.phoban.util.MobSpawnRule;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;

public class MobSpawner {
    private final PriorityQueue<SpawnTask> tasks = new PriorityQueue<>(Comparator.comparingInt(SpawnTask::getNextSpawnTick));

    public void schedule(MobSpawnRule rule) {
        PhoBan.instance.debug(1, "Scheduling task for %s", rule);
        tasks.add(new SpawnTask(rule));
    }

    public void syncTickPerSec(Room room) {
        List<SpawnTask> tasksToUpdate = new ArrayList<>();

        for (Iterator<SpawnTask> it = tasks.iterator(); it.hasNext(); ) {
            SpawnTask task = it.next();
            if (room.getTimeCounter() < task.nextSpawnTick) break;

            PhoBan.instance.debug(1, "Executing task for %s", task.rule);

            if (task.spawnAndSchedule(room)) {
                tasksToUpdate.add(task);
            } else {
                it.remove();
            }
        }

        // re-order the queue if the internal field changes
        if (!tasksToUpdate.isEmpty()) {
            tasks.removeAll(tasksToUpdate);
            tasks.addAll(tasksToUpdate);
        }
    }

    public static class SpawnTask {
        private final MobSpawnRule rule;
        private int nextSpawnTick;
        private int times;

        public SpawnTask(MobSpawnRule rule) {
            this.rule = rule;
            this.nextSpawnTick = rule.delay();
            this.times = 0;
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

            return rule.isRepeatable() && (rule.times() < 1 || ++times < rule.times());
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
                    decorate(w.spawnEntity(location, entityType, CreatureSpawnEvent.SpawnReason.CUSTOM), rule.getMobOptions());
                }
            }
        }

        private void decorate(Entity entity, MobOptions options) {
            if (entity instanceof Attributable a) {
                for (Map.Entry<Attribute, Double> entry : options.getAttributes().entrySet()) {
                    AttributeInstance attr = a.getAttribute(entry.getKey());
                    if (attr != null) {
                        attr.setBaseValue(entry.getValue());
                        if (entry.getKey() == Attribute.GENERIC_MAX_HEALTH && entity instanceof Damageable) {
                            ((LivingEntity) entity).setHealth(entry.getValue());
                        }
                    }
                }
            }
            entity.setSilent(options.isSilent());
            entity.setGlowing(options.isGlowing());
        }
    }
}
