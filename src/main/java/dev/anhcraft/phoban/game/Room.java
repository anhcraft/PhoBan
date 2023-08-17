package dev.anhcraft.phoban.game;

import dev.anhcraft.jvmkit.utils.RandomUtil;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.LevelConfig;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.MobSpawnRule;
import dev.anhcraft.phoban.util.Placeholder;
import dev.anhcraft.phoban.util.WorldGuardUtils;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Room {
    private final PhoBan plugin;
    private final String id;
    private final Difficulty difficulty;
    private Stage stage;
    private final Set<UUID> players;
    private final Map<UUID, Integer> separators;
    private final Map<UUID, Integer> respawnChances;
    private final MobSpawner mobSpawner;
    private BoundingBox region;
    private int timeCounter;
    private boolean starting;
    private boolean terminating;
    private int completeTime;

    public Room(PhoBan plugin, String id, Difficulty difficulty) {
        this.plugin = plugin;
        this.id = id;
        this.difficulty = difficulty;
        this.stage = Stage.AVAILABLE;
        this.players = new HashSet<>();
        this.separators = new HashMap<>();
        this.respawnChances = new HashMap<>();
        this.mobSpawner = new MobSpawner();
    }

    public void initialize() {
        if (stage != Stage.AVAILABLE) return;
        stage = Stage.WAITING;
        timeCounter = -1;
        region = WorldGuardUtils.getBoundingBox(getConfig().getRegion(), getConfig().getWorld());
        completeTime = 0;
        if (region != null) {
            plugin.sync(() -> getConfig().getWorld().getNearbyEntities(region, e -> e instanceof Monster).forEach(Entity::remove));
        }
    }

    public void asyncTickPerSec() {
        timeCounter++;

        if (stage == Stage.WAITING) {
            if (starting) {
                int remain = getTimeLeft();
                Placeholder placeholder = placeholder().add("cooldown", remain);

                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    placeholder.actionBar(p, plugin.messageConfig.waitingCooldown);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                }

                if (remain == 0) {
                    if (players.size() >= getLevel().getMinPlayers()) {
                        stage = Stage.PLAYING;
                        starting = false;
                        asyncStartGame();
                    } else {
                        stage = Stage.WAITING;
                    }
                    timeCounter = 0;
                }
            } else if (players.size() >= getLevel().getMinPlayers()) {
                starting = true;
            }
        } else if (stage == Stage.PLAYING) {
            plugin.sync(() -> mobSpawner.syncTickPerSec(this));

            if (players.isEmpty()) {
                stage = Stage.ENDING;
                plugin.sync(this::syncTerminate);
            } else if (getTimeLeft() == 0) {
                stage = Stage.ENDING;
                completeTime = timeCounter;
                timeCounter = 0;
                plugin.sync(this::syncEndGame);
            }
        } else if (stage == Stage.ENDING && !terminating) {
            int remain = getTimeLeft();
            Placeholder placeholder = placeholder().add("cooldown", remain);

            plugin.sync(() -> {
                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    placeholder.actionBar(p, plugin.messageConfig.endingCooldown);

                    Firework fw = (Firework) getConfig().getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
                    FireworkMeta fwm = fw.getFireworkMeta();
                    fwm.addEffect(FireworkEffect.builder().withColor(Color.RED).withFade(Color.WHITE).build());
                    fwm.addEffect(FireworkEffect.builder().withColor(Color.GREEN).withFade(Color.WHITE).build());
                    fwm.addEffect(FireworkEffect.builder().withColor(Color.BLUE).withFade(Color.WHITE).build());
                    fwm.setPower(RandomUtil.randomInt(2, 5));
                    fw.setFireworkMeta(fwm);

                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                }
            });

            if (remain == 0) {
                plugin.sync(this::syncTerminate);
            }
        }

        for (Iterator<Map.Entry<UUID, Integer>> it = separators.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> ent = it.next();
            if (ent.getValue() < 0) continue;
            ent.setValue(ent.getValue() - 1);

            Player p = Bukkit.getPlayer(ent.getKey());
            if (p == null) continue;

            if (ent.getValue() == 0) {
                plugin.sync(() -> {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.teleportAsync(getConfig().getSpawnLocation());
                });
                it.remove();
                continue;
            }

            if (p.getGameMode() != GameMode.SPECTATOR) {
                plugin.sync(() -> p.setGameMode(GameMode.SPECTATOR));
            }

            Placeholder placeholder = placeholder().add("cooldown", ent.getValue());
            placeholder.actionBar(p, plugin.messageConfig.respawnCooldown);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }
    }

    private void asyncStartGame() {
        plugin.sync(() -> {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                p.teleportAsync(getConfig().getSpawnLocation()).thenRun(() -> {
                    plugin.msg(p, plugin.messageConfig.gameStarted);
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                });
            }
        });

        for (MobSpawnRule mobSpawnRule : getLevel().getMobSpawnRules()) {
            mobSpawner.schedule(mobSpawnRule);
        }
    }

    private void syncEndGame() {
        for (Iterator<UUID> it = separators.keySet().iterator(); it.hasNext(); ) {
            UUID uuid = it.next();
            it.remove();
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.setGameMode(GameMode.SURVIVAL);
        }

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || completeTime < 1) continue;

            PlayerData data = plugin.playerDataManager.getData(p);
            data.addWonRoom(id, difficulty);

            GameHistory gameHistory = data.requireRoomHistory(id);
            int newWinTime = gameHistory.increaseWinTime(difficulty);
            gameHistory.addCompleteTime(difficulty, completeTime);

            Placeholder placeholder = placeholder().add("player", p).addTime("completeTime", completeTime);

            for (String reward : getLevel().getWinRewards()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder.replace(reward));
            }

            if (newWinTime == 1) {
                for (String reward : getLevel().getFirstWinRewards()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder.replace(reward));
                }
            }

            for (String msg : plugin.messageConfig.endMessage) {
                placeholder.messageRaw(p, msg);
            }
        }
    }

    public void syncTerminate() {
        if (terminating) return;
        terminating = true;

        if (getRegion() != null) {
            getConfig().getWorld().getNearbyEntities(getRegion(), e -> e instanceof Monster).forEach(Entity::remove);
        }

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.teleportAsync(plugin.mainConfig.spawnLocation);
        }

        plugin.gameManager.destroyRoom(this.id);
    }

    boolean handleJoinRoom(Player player) {
        if (players.contains(player.getUniqueId())) {
            if (stage == Stage.WAITING) {
                plugin.sync(() -> {
                    player.teleportAsync(getConfig().getQueueLocation()).thenRun(() -> {
                        plugin.sync(() -> player.setGameMode(GameMode.SURVIVAL), 30);
                    });
                });
            } else {
                plugin.sync(() -> {
                    player.teleportAsync(getConfig().getSpawnLocation()).thenRun(() -> {
                        if (separators.containsKey(player.getUniqueId())) {
                            plugin.sync(() -> player.setGameMode(GameMode.SPECTATOR), 30);
                        } else {
                            plugin.sync(() -> player.setGameMode(GameMode.SURVIVAL), 30);
                        }
                    });
                });
            }
            return true;
        }

        if (stage != Stage.WAITING) {
            plugin.msg(player, plugin.messageConfig.notInWaiting);
            return false;
        }
        if (players.size() >= getLevel().getMaxPlayers()) {
            placeholder().message(player, plugin.messageConfig.maxPlayerReached);
            return false;
        }

        PlayerData playerData = plugin.playerDataManager.getData(player);

        if (playerData.getTicket() >= getLevel().getTicketCost()) {
            placeholder().message(player, plugin.messageConfig.insufficientTicket);
            return false;
        }

        if (terminating || !players.add(player.getUniqueId()))
            return false;

        playerData.reduceTicket(getLevel().getTicketCost());
        playerData.requireRoomHistory(id).increasePlayTime(difficulty);

        Placeholder placeholder = placeholder().add("player", player);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.joinMessage);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.5f);
        }

        plugin.sync(() -> player.teleportAsync(getConfig().getQueueLocation()));

        return true;
    }

    boolean handleLeaveRoom(Player player) {
        if (!players.remove(player.getUniqueId()))
            return false;

        if (separators.remove(player.getUniqueId()) != null) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        Placeholder placeholder = placeholder().add("player", player);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.leaveMessage);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        }

        plugin.sync(() -> player.teleportAsync(plugin.mainConfig.spawnLocation));

        return true;
    }

    public void handleBossDeath(MythicMobDeathEvent event) {
        Placeholder placeholder = placeholder().add("boss", event.getMob().getDisplayName());

        if (event.getKiller() != null) {
            placeholder.add("killer", event.getKiller());
        }

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.killMessage);
        }

        stage = Stage.ENDING;
        completeTime = timeCounter;
        timeCounter = 0;
        syncEndGame();
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setCancelled(true);
        event.setReviveHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        if (separators.containsKey(player.getUniqueId()) || stage != Stage.PLAYING) {
            return;
        }

        player.setGameMode(GameMode.SPECTATOR);
        int chances = respawnChances.getOrDefault(player.getUniqueId(), 0);

        if (chances >= getLevel().getRespawnChances()) {
            separators.put(player.getUniqueId(), -1);
            placeholder().message(player, plugin.messageConfig.respawnMax);
        } else {
            separators.put(player.getUniqueId(), getLevel().getRespawnTime());
            respawnChances.put(player.getUniqueId(), chances + 1);
        }
    }

    @NotNull
    public Placeholder placeholder() {
        return Placeholder.create()
                .add("dungeon", getConfig().getName())
                .add("currentPlayers", players.size())
                .add("maxPlayers", getLevel().getMaxPlayers())
                .add("difficulty", difficulty)
                .add("stage", stage);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public RoomConfig getConfig() {
        return Objects.requireNonNull(plugin.gameManager.getRoomConfig(id));
    }

    @NotNull
    public LevelConfig getLevel() {
        return getConfig().getLevel(difficulty);
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
        return separators.keySet();
    }

    @NotNull
    public Map<UUID, Integer> getRespawnChances() {
        return respawnChances;
    }

    public boolean isStarting() {
        return starting;
    }

    public boolean isTerminating() {
        return terminating;
    }

    public int getCompleteTime() {
        return completeTime;
    }

    @NotNull
    public MobSpawner getMobSpawner() {
        return mobSpawner;
    }

    public int getTimeCounter() {
        return timeCounter;
    }

    public boolean hasLocation(@NotNull Location location) {
        return region != null && location.getWorld().equals(getConfig().getWorld()) && region.contains(location.getX(), location.getY(), location.getZ());
    }

    @Nullable
    public BoundingBox getRegion() {
        return region;
    }

    public int getTimeLeft() {
        int i = 0;
        if (stage == Stage.ENDING) {
            i = plugin.mainConfig.roomSettings.intermissionTime - timeCounter;
        } else if (stage == Stage.PLAYING) {
            i = getConfig().getLevel(difficulty).getPlayingTime() - timeCounter;
        } else if (stage == Stage.WAITING) {
            i = plugin.mainConfig.roomSettings.waitingTime - timeCounter;
        }
        return Math.max(0, i);
    }
}
