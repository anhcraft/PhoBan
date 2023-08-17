package dev.anhcraft.phoban.storage;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.RoomRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
public class PlayerData {
    @Exclude
    public AtomicBoolean dirty = new AtomicBoolean(false);

    @Validation(notNull = true, silent = true)
    private Map<String, Difficulty> roomPlayed = new HashMap<>();

    @Validation(notNull = true, silent = true)
    private Map<String, GameHistory> roomHistory = new HashMap<>();

    @Nullable
    public GameHistory getGameHistory(String roomId) {
        GameHistory history = roomHistory.get(roomId);
        if (history != null && history.dirty == null) {
            history.dirty = dirty;
        }
        return history;
    }

    @NotNull
    public GameHistory requireRoomHistory(String roomId) {
        GameHistory history = roomHistory.get(roomId);
        if (history == null) {
            roomHistory.put(roomId, history = new GameHistory());
            history.dirty = dirty;
        } else if (history.dirty == null) {
            history.dirty = dirty;
        }
        return history;
    }

    public boolean hasPlayedRoom(@NotNull String room, @NotNull Difficulty minimumDifficulty) {
        Difficulty diff = roomPlayed.get(room);
        return diff != null && diff.ordinal() >= minimumDifficulty.ordinal();
    }

    public boolean hasPlayedRoom(RoomRequirement roomRequirement) {
        return hasPlayedRoom(roomRequirement.name(), roomRequirement.minimumDifficulty());
    }

    public void addPlayedRoom(@NotNull String room, @NotNull Difficulty difficulty) {
        Difficulty diff = roomPlayed.get(room);
        if (diff == null || diff.ordinal() < difficulty.ordinal()) {
            roomPlayed.put(room, difficulty);
        }
    }

    public void markDirty() {
        dirty.set(true);
    }

    @PostHandler
    private void handle() {
        dirty = new AtomicBoolean(false); // sometimes this disappears
    }
}
