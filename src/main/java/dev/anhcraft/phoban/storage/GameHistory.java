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
    private Map<Difficulty, Integer> winTimes = new HashMap<>();
    private Map<Difficulty, Long> bestCompleteTime = new HashMap<>();

    public int getPlayTimes(Difficulty difficulty) {
        return playTimes.getOrDefault(difficulty, 0);
    }

    public int getTotalPlayTimes() {
        return playTimes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getWinTimes(Difficulty difficulty) {
        return winTimes.getOrDefault(difficulty, 0);
    }

    public int getTotalWinTimes() {
        return winTimes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getLossTimes(Difficulty difficulty) {
        return getPlayTimes(difficulty) - getWinTimes(difficulty);
    }

    public int getTotalLossTimes() {
        return getTotalPlayTimes() - getTotalWinTimes();
    }

    public long getBestCompleteTime(Difficulty difficulty) {
        return bestCompleteTime.getOrDefault(difficulty, 0L);
    }

    public long getBestCompleteOfAllTime() {
        return bestCompleteTime.values().stream().mapToLong(Long::longValue).min().orElse(0L);
    }

    public int increasePlayTime(Difficulty difficulty) {
        int newPlayTime = getPlayTimes(difficulty) + 1;
        playTimes.put(difficulty, newPlayTime);
        dirty.set(true);
        return newPlayTime;
    }

    public int increaseWinTime(Difficulty difficulty) {
        int newWinTime = getWinTimes(difficulty) + 1;
        winTimes.put(difficulty, newWinTime);
        dirty.set(true);
        return newWinTime;
    }

    public void addCompleteTime(Difficulty difficulty, long time) {
        bestCompleteTime.put(difficulty, Math.min(getBestCompleteTime(difficulty), time));
        dirty.set(true);
    }
}
