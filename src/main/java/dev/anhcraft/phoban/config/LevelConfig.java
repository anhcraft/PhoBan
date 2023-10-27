package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.*;
import dev.anhcraft.phoban.util.MobSpawnRule;
import dev.anhcraft.phoban.util.SoundPlayRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class LevelConfig {
    private int ticketCost;
    private int minPlayers;
    private int maxPlayers;
    private int playingTime;
    private int respawnTime;
    private int respawnChances;

    @Nullable
    private String bossId;

    @Optional
    private Map<String, Integer> objectives = new HashMap<>();

    private boolean allowTimeout;

    private boolean allowOverachieve;

    private boolean allowOverfull;

    private boolean allowLateJoin;

    private String[] joinMessages;

    @Optional
    private List<String> mobs = Collections.emptyList();

    @Optional
    private List<String> sounds = Collections.emptyList();

    @Optional
    private List<String> winRewards = Collections.emptyList();

    @Optional
    private List<String> firstWinRewards = Collections.emptyList();

    @Optional
    private List<String> bossKillRewards = Collections.emptyList();

    @Exclude
    private List<MobSpawnRule> mobSpawnRules;

    @Exclude
    private List<SoundPlayRule> soundPlayRules;

    @PostHandler
    private void postHandler() {
        this.mobSpawnRules = this.mobs.stream().map(MobSpawnRule::parse).toList();
        this.soundPlayRules = this.sounds.stream().map(SoundPlayRule::parse).toList();
        if (this.bossId != null) {
            this.objectives.put(this.bossId, 1);
        }
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
    public Map<String, Integer> getObjectives() {
        return objectives;
    }

    @NotNull
    public List<MobSpawnRule> getMobSpawnRules() {
        return mobSpawnRules;
    }

    @NotNull
    public List<SoundPlayRule> getSoundPlayRules() {
        return soundPlayRules;
    }

    @NotNull
    public List<String> getWinRewards() {
        return winRewards;
    }

    @NotNull
    public List<String> getFirstWinRewards() {
        return firstWinRewards;
    }

    @NotNull
    public List<String> getBossKillRewards() {
        return bossKillRewards;
    }

    public boolean isAllowTimeout() {
        return allowTimeout;
    }

    public boolean isAllowOverachieve() {
        return allowOverachieve;
    }

    public boolean isAllowOverfull() {
        return allowOverfull;
    }

    public boolean isAllowLateJoin() {
        return allowLateJoin;
    }

    @Nullable
    public String[] getJoinMessages() {
        return joinMessages;
    }
}
