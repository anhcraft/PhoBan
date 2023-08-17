package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.util.MobSpawnRule;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class LevelConfig {
    private int ticketCost;
    private int minPlayers;
    private int maxPlayers;
    private int playingTime;
    private int respawnTime;
    private int respawnChances;

    @Validation(notNull = true)
    private String bossId;

    @Validation(notNull = true, silent = true)
    private List<String> mobs = Collections.emptyList();

    @Validation(notNull = true, silent = true)
    private List<String> winRewards = Collections.emptyList();

    @Validation(notNull = true, silent = true)
    private List<String> firstWinRewards = Collections.emptyList();

    @Exclude
    private List<MobSpawnRule> mobSpawnRules;

    @PostHandler
    private void postHandler() {
        this.mobSpawnRules = this.mobs.stream().map(MobSpawnRule::parse).toList();
    }

    public int getTicketCost() {
        return ticketCost;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayingTime() {
        return playingTime;
    }

    public int getRespawnChances() {
        return respawnChances;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    @NotNull
    public String getBossId() {
        return bossId;
    }

    @NotNull
    public List<MobSpawnRule> getMobSpawnRules() {
        return mobSpawnRules;
    }

    @NotNull
    public List<String> getWinRewards() {
        return winRewards;
    }

    @NotNull
    public List<String> getFirstWinRewards() {
        return firstWinRewards;
    }
}
