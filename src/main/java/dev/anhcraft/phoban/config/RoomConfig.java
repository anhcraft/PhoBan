package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.game.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
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

    @Validation(notNull = true)
    private List<String> description;

    @Validation(notNull = true)
    private Location spawnLocation;

    private String region;

    @Validation(notNull = true)
    private Map<Difficulty, LevelConfig> levels;

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

    @NotNull
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Nullable
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
