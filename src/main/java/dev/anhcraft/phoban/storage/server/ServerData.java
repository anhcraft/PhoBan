package dev.anhcraft.phoban.storage.server;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.vanhen.games.GameSession;
import dev.anhcraft.vanhen.games.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
public class ServerData {
    @Exclude
    public AtomicBoolean dirty = new AtomicBoolean(false);

    public void markDirty() {
        dirty.set(true);
    }

    @PostHandler
    private void handle() {
        dirty = new AtomicBoolean(false); // sometimes this disappears
    }

    public Map<GameType, GameSession> sessions = new HashMap<>();
}
