package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.RoomRequirement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class RoomConfig {
    @Validation(notNull = true)
    private String name;

    @Validation(notNull = true)
    private Material icon;

    private String requirement;

    private WeatherType weatherLock;

    private int timeLock = -1;

    @Validation(notNull = true)
    private List<String> description;

    @Validation(notNull = true)
    private Location spawnLocation;

    @Validation(notNull = true)
    private Location queueLocation;

    @Validation(notNull = true)
    private String region;

    @Validation(notNull = true)
    private Map<Difficulty, LevelConfig> levels;

    @Exclude
    private RoomRequirement roomRequirement;

    @PostHandler
    private void postHandler() {
        roomRequirement = requirement == null ? null : RoomRequirement.parse(requirement);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }

    @NotNull
    public List<String> getDescription() {
        return description;
    }

    @Nullable
    public RoomRequirement getRoomRequirement() {
        return roomRequirement;
    }

    @Nullable
    public WeatherType getWeatherLock() {
        return weatherLock;
    }

    public int getTimeLock() {
        return timeLock;
    }

    @NotNull
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @NotNull
    public Location getQueueLocation() {
        return queueLocation;
    }

    @NotNull
    public World getWorld() {
        return spawnLocation.getWorld();
    }

    @NotNull
    public String getRegion() {
        return region;
    }

    @NotNull
    public Map<Difficulty, LevelConfig> getLevels() {
        return levels;
    }

    public LevelConfig getLevel(Difficulty difficulty) {
        return levels.get(difficulty);
    }
}
