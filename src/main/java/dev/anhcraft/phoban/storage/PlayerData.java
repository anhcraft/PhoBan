package dev.anhcraft.phoban.storage;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.jvmkit.utils.Condition;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.RoomRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Configurable
public class PlayerData {
    @Exclude
    public AtomicBoolean dirty = new AtomicBoolean(false);

    @Validation(notNull = true, silent = true)
    private Map<String, Difficulty> roomWins = new HashMap<>();

    @Validation(notNull = true, silent = true)
    private Map<String, GameHistory> roomHistory = new HashMap<>();

    private int ticket;
    private long lastFreeTicketTime;
    private long lastCreateRoomTime;

    public Stream<GameHistory> streamGameHistory() {
        return roomHistory.values().stream();
    }

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

    public boolean hasWonRoom(@NotNull String room, @NotNull Difficulty minimumDifficulty) {
        Difficulty diff = roomWins.get(room);
        return diff != null && diff.ordinal() >= minimumDifficulty.ordinal();
    }

    public boolean hasWonRoom(RoomRequirement roomRequirement) {
        return hasWonRoom(roomRequirement.name(), roomRequirement.minimumDifficulty());
    }

    public void addWonRoom(@NotNull String room, @NotNull Difficulty difficulty) {
        Difficulty diff = roomWins.get(room);
        if (diff == null || diff.ordinal() < difficulty.ordinal()) {
            roomWins.put(room, difficulty);
            markDirty();
        }
    }


    public int getTicket() {
        return ticket;
    }

    public void addTicket(int amount) {
        Condition.check(amount >= 0, "amount must be >= 0");
        this.ticket += amount;
        markDirty();
    }

    public void reduceTicket(int amount) {
        Condition.check(amount >= 0, "amount must be >= 0");
        this.ticket = Math.max(0, this.ticket - amount);
        markDirty();
    }

    public void setTicket(int ticket) {
        this.ticket = ticket;
        markDirty();
    }

    public long getLastFreeTicketTime() {
        return lastFreeTicketTime;
    }

    public void setLastFreeTicketTime(long lastFreeTicketTime) {
        this.lastFreeTicketTime = lastFreeTicketTime;
        markDirty();
    }

    public long getLastCreateRoomTime() {
        return lastCreateRoomTime;
    }

    public void setLastCreateRoomTime(long lastCreateRoomTime) {
        this.lastCreateRoomTime = lastCreateRoomTime;
        markDirty();
    }

    public void reset() {
        roomWins.clear();
        roomHistory.clear();
        ticket = 0;
        lastFreeTicketTime = 0;
        lastCreateRoomTime = 0;
        markDirty();
    }

    public void markDirty() {
        dirty.set(true);
    }

    @PostHandler
    private void handle() {
        dirty = new AtomicBoolean(false); // sometimes this disappears
    }
}
