package dev.anhcraft.phoban.util;

public record MythicMob(String id, int level) {
    public static MythicMob parse(String boss) {
        String[] parts = boss.split(":");
        if (parts.length == 1) {
            return new MythicMob(parts[0], 1);
        }
        return new MythicMob(parts[0], Integer.parseInt(parts[1]));
    }
}
