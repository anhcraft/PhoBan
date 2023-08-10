package dev.anhcraft.phoban.storage.player;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;

import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
public class PlayerData {
    @Exclude
    public AtomicBoolean dirty = new AtomicBoolean(false);

    public void markDirty() {
        dirty.set(true);
    }

    @PostHandler
    private void handle() {
        dirty = new AtomicBoolean(false); // sometimes this disappears
    }
}
