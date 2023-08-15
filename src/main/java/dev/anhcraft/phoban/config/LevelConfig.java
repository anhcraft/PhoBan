package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.util.MobSpawnRule;
import dev.anhcraft.phoban.util.MythicMob;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class LevelConfig {
    private int minPlayers;
    private int maxPlayers;
    private int preparingTme;
    private int playingTime;
    private int respawnChances;

    @Validation(notNull = true)
    private String boss;

    @Validation(notNull = true)
    private List<String> mobs;

    @Validation(notNull = true)
    private List<String> winRewards;

    @Exclude
    private MythicMob mythicBoss;
    @Exclude
    private List<MobSpawnRule> mobSpawnRules;

    @PostHandler
    private void postHandler() {
        this.mythicBoss = MythicMob.parse(this.boss);
        this.mobSpawnRules = this.mobs.stream().map(MobSpawnRule::parse).toList();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPreparingTme() {
        return preparingTme;
    }

    public int getPlayingTime() {
        return playingTime;
    }

    public int getRespawnChances() {
        return respawnChances;
    }

    @NotNull
    public MythicMob getMythicBoss() {
        return mythicBoss;
    }

    @NotNull
    public List<MobSpawnRule> getMobSpawnRules() {
        return mobSpawnRules;
    }

    @NotNull
    public List<String> getWinRewards() {
        return winRewards;
    }
}
