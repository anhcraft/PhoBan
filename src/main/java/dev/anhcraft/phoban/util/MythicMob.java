package dev.anhcraft.phoban.util;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;

public record MythicMob(String id, int level) {
    public static MythicMob parse(String boss) {
        String[] parts = boss.split(":");
        if (parts.length == 1) {
            return new MythicMob(parts[0], 1);
        }
        return new MythicMob(parts[0], Integer.parseInt(parts[1]));
    }

    public MythicMob raiseLevel(int delta) {
        return new MythicMob(id, level + delta);
    }

    public void spawn(Location location) throws InvalidMobTypeException {
        MythicBukkit.inst().getAPIHelper().spawnMythicMob(id, location, level);
    }

    @Override
    public String toString() {
        return id + ":" + level;
    }
}
