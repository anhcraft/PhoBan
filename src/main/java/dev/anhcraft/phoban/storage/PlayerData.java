package dev.anhcraft.phoban.storage;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
public class PlayerData {
    @Exclude
    public AtomicBoolean dirty = new AtomicBoolean(false);

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

    public void markDirty() {
        dirty.set(true);
    }

    @PostHandler
    private void handle() {
        dirty = new AtomicBoolean(false); // sometimes this disappears
    }
}
