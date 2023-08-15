package dev.anhcraft.phoban.storage;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.phoban.game.Difficulty;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
public class GameHistory {
    @Exclude
    AtomicBoolean dirty;

    private Map<Difficulty, Integer> playTimes = new HashMap<>();
    private Map<Difficulty, Long> bestCompleteTime = new HashMap<>();

    public int getPlayTimes(Difficulty difficulty) {
        return playTimes.getOrDefault(difficulty, 0);
    }

    public int getTotalPlayTimes() {
        return playTimes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public long getBestCompleteTime(Difficulty difficulty) {
        return bestCompleteTime.getOrDefault(difficulty, 0L);
    }

    public long getBestCompleteOfAllTime() {
        return bestCompleteTime.values().stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    public void increasePlayTime(Difficulty difficulty) {
        playTimes.put(difficulty, getPlayTimes(difficulty) + 1);
        dirty.set(true);
    }

    public void addCompleteTime(Difficulty difficulty, long time) {
        bestCompleteTime.put(difficulty, Math.max(getBestCompleteTime(difficulty), time));
        dirty.set(true);
    }
}
