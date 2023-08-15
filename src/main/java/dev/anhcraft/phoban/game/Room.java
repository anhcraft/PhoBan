package dev.anhcraft.phoban.game;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.LevelConfig;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.util.MobSpawnRule;
import dev.anhcraft.phoban.util.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Room {
    private PhoBan plugin;
    private RoomConfig roomConfig;
    private Difficulty difficulty;
    private Stage stage;
    private Set<UUID> players;
    private Set<UUID> separators;
    private Queue<MobSpawnRule> mobSpawnQueue;
    private int timeCounter;

    public Room(PhoBan plugin, RoomConfig roomConfig, Difficulty difficulty) {
        this.plugin = plugin;
        this.roomConfig = roomConfig;
        this.difficulty = difficulty;
        this.stage = Stage.AVAILABLE;
        this.players = new HashSet<>();
        this.separators = new HashSet<>();
        this.mobSpawnQueue = new LinkedList<>();
    }

    public void initialize() {
        if (stage != Stage.AVAILABLE) return;
        stage = Stage.WAITING;
        timeCounter = 0;
    }

    public void asyncTickPerSec() {
        if (stage == Stage.WAITING) {
            int remain = getTimeLeft();
            Placeholder placeholder = placeholder().add("cooldown", remain);

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                placeholder.message(p, plugin.messageConfig.waitingCooldown);
                p.playSound(p.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1.0f, 0.5f);

                if (remain == 0) {
                    placeholder.message(p, plugin.messageConfig.gameStarted);
                }
            }

            if (remain == 0) {
                stage = Stage.PLAYING;
                timeCounter = 0;
            }
        } else if (stage == Stage.PLAYING) {
            if (players.isEmpty()) {
                terminate();
                return;
            }

            if (getTimeLeft() == 0) {
                endGame();
            }
        } else if (stage == Stage.ENDING) {
            int remain = getTimeLeft();
            Placeholder placeholder = placeholder().add("cooldown", remain);

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                placeholder.message(p, plugin.messageConfig.endingCooldown);
                p.playSound(p.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1.0f, 0.5f);
            }

            if (remain == 0) {
                terminate();
            }
        }

        timeCounter++;
    }

    private void endGame() {
        if (stage == Stage.ENDING) return;
        stage = Stage.ENDING;

    }

    private void terminate() {
        if (stage != Stage.ENDING) return;

    }

    boolean handleJoinRoom(Player player) {
        if (!players.add(player.getUniqueId()))
            return false;

        Placeholder placeholder = placeholder().add("player", player);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.joinMessage);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        }

        player.teleport(roomConfig.getSpawnLocation());

        return true;
    }

    boolean handleLeaveRoom(Player player) {
        if (!players.remove(player.getUniqueId()))
            return false;

        if (separators.remove(player.getUniqueId())) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        Placeholder placeholder = placeholder().add("player", player);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.leaveMessage);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        }

        player.teleport(plugin.mainConfig.spawnLocation);

        return true;
    }

    @NotNull
    public Placeholder placeholder() {
        return Placeholder.create()
                .add("currentPlayers", players.size())
                .add("maxPlayers", getLevel().getMaxPlayers())
                .add("difficulty", difficulty)
                .add("stage", stage);
    }

    @NotNull
    public RoomConfig getConfig() {
        return roomConfig;
    }

    @NotNull
    public LevelConfig getLevel() {
        return roomConfig.getLevel(difficulty);
    }

    @NotNull
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @NotNull
    public Stage getStage() {
        return stage;
    }

    @NotNull
    public Set<UUID> getPlayers() {
        return players;
    }

    @NotNull
    public Set<UUID> getSeparators() {
        return separators;
    }

    @NotNull
    public Queue<MobSpawnRule> getMobSpawnQueue() {
        return mobSpawnQueue;
    }

    public int getTimeCounter() {
        return timeCounter;
    }

    public int getTimeLeft() {
        int i = 0;
        if (stage == Stage.ENDING) {
            i = plugin.mainConfig.roomSettings.intermissionTime - timeCounter;
        } else if (stage == Stage.PLAYING) {
            i = roomConfig.getLevel(difficulty).getPlayingTime() - timeCounter;
        } else if (stage == Stage.WAITING) {
            i = plugin.mainConfig.roomSettings.waitingTime - timeCounter;
        }
        return Math.max(0, i);
    }
}
