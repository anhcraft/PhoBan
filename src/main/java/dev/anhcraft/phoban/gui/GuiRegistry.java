package dev.anhcraft.phoban.gui;

import org.bukkit.entity.Player;

public class GuiRegistry {
    public static RoomSelectorGui ROOM_SELECTOR;
    public static DifficultySelectorGui DIFFICULTY_SELECTOR;

    public static void openRoomSelector(Player player) {
        ROOM_SELECTOR.open(player, new RoomSelectorGuiHandler());
    }

    public static void openDifficultySelector(Player player, String roomId) {
        DIFFICULTY_SELECTOR.open(player, new DifficultySelectorGuiHandler(roomId));
    }
}
