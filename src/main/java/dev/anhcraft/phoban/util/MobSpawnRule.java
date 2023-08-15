package dev.anhcraft.phoban.util;

import dev.anhcraft.jvmkit.utils.EnumUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record MobSpawnRule(@NotNull String type, int amount, @Nullable Location location, int delay, int every) {
    public static MobSpawnRule parse(String rule) {
        String[] parts = rule.split("\\s*@\\s*");

        Map<String, String> options = new HashMap<>();

        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String[] args = parts[i].trim().split("=");
                options.put(args[0], args[1]);
            }
        }

        String[] mainArgs = parts[0].trim().split("\\s+");
        Location location = null;

        if (mainArgs.length == 5) {
            location = new Location(
                    Bukkit.getWorld(mainArgs[1]),
                    Double.parseDouble(mainArgs[2]),
                    Double.parseDouble(mainArgs[3]),
                    Double.parseDouble(mainArgs[4])
            );
        } else if (mainArgs.length != 1) {
            throw new IllegalArgumentException("Invalid main arguments");
        }

        return new MobSpawnRule(
                mainArgs[0],
                Integer.parseInt(options.getOrDefault("amount", "1")),
                location,
                Integer.parseInt(options.getOrDefault("delay", "0")),
                Integer.parseInt(options.getOrDefault("every", "0"))
        );
    }

    @Nullable
    public EntityType getVanillaType() {
        return (EntityType) EnumUtil.findEnum(EntityType.class, type);
    }

    @NotNull
    public MythicMob getMythicBoss() {
        return MythicMob.parse(type);
    }

    public boolean isRepeatable() {
        return every > 0;
    }

    @Override
    public String toString() {
        if (location == null) {
            return String.format(
                    "%s @amount=%d @delay=%d @every=%d",
                    type, amount, delay, every
            );
        }

        return String.format(
                "%s %s %.2f %.2f %.2f @amount=%d @delay=%d @every=%d",
                type, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), amount, delay, every
        );
    }
}
