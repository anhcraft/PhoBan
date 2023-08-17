package dev.anhcraft.phoban.game;

public record RoomRequirement(String name, Difficulty minimumDifficulty) {
    public static RoomRequirement parse(String requirement) {
        String[] split = requirement.split(":");
        if (split.length == 1) {
            return new RoomRequirement(split[0], Difficulty.EASY);
        }
        return new RoomRequirement(split[0], Difficulty.valueOf(split[1]));
    }
}
