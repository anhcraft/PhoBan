package dev.anhcraft.phoban.gui;

import dev.anhcraft.palette.ui.Gui;
import org.bukkit.entity.Player;

public class GuiRegistry {
    public static RoomSelectorGui ROOM_SELECTOR;
    public static DifficultySelectorGui DIFFICULTY_SELECTOR;
    public static Gui SOUND_EXPLORER;

    public static void openRoomSelector(Player player) {
        ROOM_SELECTOR.open(player, new RoomSelectorGuiHandler());
    }

    public static void openDifficultySelector(Player player, String roomId) {
        DIFFICULTY_SELECTOR.open(player, new DifficultySelectorGuiHandler(roomId));
    }

    public static void openSoundExplorer(Player player) {
        SOUND_EXPLORER.open(player, new SoundExplorerGuiHandler());
    }
}
