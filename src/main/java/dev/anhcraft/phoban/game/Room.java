package dev.anhcraft.phoban.game;

import dev.anhcraft.jvmkit.utils.RandomUtil;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.LevelConfig;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.*;
import io.lumine.mythic.bukkit.MythicBukkit;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class Room {
    private static final Predicate<Entity> ENTITY_FILTER = e -> e instanceof Creature || e instanceof Item || e instanceof Projectile;
    private final PhoBan plugin;
    private final String id;
    private final Difficulty difficulty;
    private Stage stage;
    private final Set<UUID> players;
    private final Map<UUID, Integer> separators;
    private final Map<UUID, Integer> respawnChances;
    private final MobSpawner mobSpawner;
    private final SoundPlayer soundPlayer;
    private BoundingBox region;
    private int challengeLevel;
    private int timeCounter;
    private boolean forceStart;
    private boolean starting;
    private boolean terminating;
    private boolean won;
    private int completeTime;
    private List<MobSpawnRule> mobSpawnRules;
    private Map<String, Integer> objectiveRequirements;

    public Room(PhoBan plugin, String id, Difficulty difficulty) {
        this.plugin = plugin;
        this.id = id;
        this.difficulty = difficulty;
        this.stage = Stage.AVAILABLE;
        this.players = new HashSet<>();
        this.separators = new HashMap<>();
        this.respawnChances = new HashMap<>();
        this.mobSpawner = new MobSpawner();
        this.soundPlayer = new SoundPlayer();
        this.mobSpawnRules = new ArrayList<>();
        this.objectiveRequirements = new HashMap<>();
    }

    public void initialize(int challenge) {
        if (stage != Stage.AVAILABLE) return;
        stage = Stage.WAITING;
        timeCounter = -1;
        completeTime = 0;
        challengeLevel = difficulty == Difficulty.CHALLENGE ? challenge : 0;
        region = WorldGuardUtils.getBoundingBox(getConfig().getRegion(), getConfig().getWorld());
        if (region != null)
            plugin.debug(2, "Room %s has region %s", id, region.toString());
        plugin.sync(this::cleanMobs);

        for (Map.Entry<String, Integer> e : getLevel().getObjectives().entrySet()) {
            if (e.getKey().contains(":")) {
                // OBJECTIVES MUST NOT HAVE LEVEL
                // Some bosses have special ability to level up and that can break the objectives system
                plugin.getLogger().warning(String.format("Room %s should not have objective %s with level specified", id, e.getKey()));
                objectiveRequirements.put(e.getKey().split(":")[0], e.getValue());
            } else {
                objectiveRequirements.put(e.getKey(), e.getValue());
            }
        }
        for (String s : objectiveRequirements.keySet())
            plugin.debug(2, "Room %s requires %s", id, s);

        if (difficulty == Difficulty.CHALLENGE) {
            for (MobSpawnRule mobSpawnRule : getLevel().getMobSpawnRules()) {
                mobSpawnRules.add(mobSpawnRule.raiseLevel(challenge));
            }
        } else mobSpawnRules.addAll(getLevel().getMobSpawnRules());
    }

    public void asyncTickPerSec() {
        timeCounter++;

        if (getTimeCounter() > 10 && players.isEmpty()) {
            stage = Stage.ENDING;
            plugin.sync(this::syncTerminate);
            return;
        }

        if (stage == Stage.WAITING) {
            if (starting) {
                if (players.size() >= getLevel().getMaxPlayers()) {
                    stage = Stage.PLAYING;
                    starting = false;
                    asyncStartGame();
                    return;
                }

                int remain = getTimeLeft();
                Placeholder placeholder = placeholder().add("cooldown", remain);

                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    placeholder.actionBar(p, plugin.messageConfig.waitingCooldown);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                }

                if (forceStart || remain == 0) {
                    if (forceStart || players.size() >= getLevel().getMinPlayers()) {
                        stage = Stage.PLAYING;
                        starting = false;
                        asyncStartGame();
                    } else {
                        stage = Stage.WAITING;
                    }
                    timeCounter = 0;
                    return;
                }
            } else if (players.size() >= getLevel().getMinPlayers()) {
                starting = true;
            }
        } else if (stage == Stage.PLAYING) {
            plugin.sync(() -> mobSpawner.syncTickPerSec(this));
            soundPlayer.asyncTickPerSec(this);

            if (getTimeLeft() == 0 || (!separators.isEmpty() && separators.values().stream().allMatch(i -> i < 0))) {
                stage = Stage.ENDING;
                completeTime = timeCounter;
                timeCounter = 0;
                won = objectiveRequirements.isEmpty() || getLevel().isAllowTimeout();
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

                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);

                    if (won) {
                        Firework fw = (Firework) getConfig().getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
                        FireworkMeta fwm = fw.getFireworkMeta();
                        fwm.addEffect(FireworkEffect.builder().withColor(Color.RED).withFade(Color.WHITE).build());
                        fwm.addEffect(FireworkEffect.builder().withColor(Color.GREEN).withFade(Color.WHITE).build());
                        fwm.addEffect(FireworkEffect.builder().withColor(Color.BLUE).withFade(Color.WHITE).build());
                        fwm.setPower(RandomUtil.randomInt(1, 3));
                        fw.setFireworkMeta(fwm);
                    }
                }
            });

            if (remain == 0) {
                plugin.sync(this::syncTerminate);
            }
        }

        for (Iterator<Map.Entry<UUID, Integer>> it = separators.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> ent = it.next();
            if (ent.getValue() < 0) continue; // ignore spectators who no longer have respawn chance

            ent.setValue(Math.max(0, ent.getValue() - 1));

            Player p = Bukkit.getPlayer(ent.getKey());
            if (p == null) {
                continue;
            }

            if (ent.getValue() == 0) {
                plugin.sync(() -> {
                    p.setGameMode(GameMode.SURVIVAL);
                    syncUpdatePlayerState(p);
                });
                it.remove();
                continue;
            }

            if (p.getGameMode() != PhoBan.SPECTATOR_GAMEMODE) {
                plugin.sync(() -> p.setGameMode(PhoBan.SPECTATOR_GAMEMODE));
            }

            Placeholder placeholder = placeholder().add("cooldown", ent.getValue());
            placeholder.actionBar(p, plugin.messageConfig.respawnCooldown);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }
    }

    public void forceStart() {
        forceStart = true;
    }

    private void asyncStartGame() {
        plugin.sync(() -> {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                plugin.playerDataManager.getData(p).requireRoomHistory(id).increasePlayTime(difficulty);
                syncUpdatePlayerState(p).thenRun(() -> {
                    if (getLevel().getStartMessages() != null){
                        for (String s : getLevel().getStartMessages()) {
                            plugin.msg(p, s);
                        }
                    }
                    plugin.msg(p, plugin.messageConfig.gameStarted);
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                });
            }
        });

        for (MobSpawnRule mobSpawnRule : mobSpawnRules) {
            mobSpawner.schedule(mobSpawnRule);
        }

        for (SoundPlayRule soundPlayRule : getLevel().getSoundPlayRules()) {
            soundPlayer.schedule(soundPlayRule);
        }
    }

    private CompletableFuture<Void> syncUpdatePlayerState(Player player) {
        Location tp = stage == Stage.WAITING ? getConfig().getQueueLocation() : getConfig().getSpawnLocation();
        return player.teleportAsync(tp).thenRun(() -> {
            if (getConfig().getWeatherLock() != null) {
                player.setPlayerWeather(getConfig().getWeatherLock());
            }
            if (getConfig().getTimeLock() >= 0) {
                player.setPlayerTime(getConfig().getTimeLock(), false);
            }
        });
    }

    private void syncEndGame() {
        for (Iterator<UUID> it = separators.keySet().iterator(); it.hasNext(); ) {
            UUID uuid = it.next();
            it.remove();
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.setGameMode(GameMode.SURVIVAL);
            p.resetPlayerWeather();
            p.resetPlayerTime();
        }

        if (won) {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || completeTime < 1) continue;

                PlayerData data = plugin.playerDataManager.getData(p);
                data.addWonRoom(id, difficulty);
                if (difficulty == Difficulty.CHALLENGE) data.increaseChallengeLevel(id);

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

                for (String msg : plugin.messageConfig.winMessage) {
                    placeholder.messageRaw(p, msg);
                }
            }
        } else {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || completeTime < 1) continue;

                GameHistory gameHistory = plugin.playerDataManager.getData(p).requireRoomHistory(id);
                gameHistory.addCompleteTime(difficulty, completeTime);

                Placeholder placeholder = placeholder().add("player", p).addTime("completeTime", completeTime);

                for (String msg : plugin.messageConfig.lossMessage) {
                    placeholder.messageRaw(p, msg);
                }
            }
        }
    }

    public void syncTerminate() {
        if (terminating) return;
        terminating = true;

        cleanMobs();

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.resetPlayerTime();
            p.resetPlayerWeather();
            p.teleportAsync(plugin.mainConfig.spawnLocation);
        }

        plugin.gameManager.destroyRoom(this.id);
    }

    public void forceEnd() {
        timeCounter += getTimeLeft();
    }

    private void cleanMobs() {
        if (getRegion() != null) {
            Collection<Entity> mobs = getConfig().getWorld().getNearbyEntities(getRegion(), ENTITY_FILTER);
            for (Entity mob : mobs) {
                if (MythicBukkit.inst().getAPIHelper().isMythicMob(mob)) {
                    plugin.debug(2, "Removing MM %s", MythicBukkit.inst().getAPIHelper().getMythicMobInstance(mob).getMobType());
                }
                mob.remove();
            }
            plugin.debug(1, "Removed %d mobs in room %s", mobs.size(), id);
        }
    }

    boolean handleJoinRoom(Player player, boolean force) {
        if (players.contains(player.getUniqueId())) {
            syncUpdatePlayerState(player).thenRun(() -> {
                if (getLevel().getJoinMessages() != null){
                    for (String s : getLevel().getJoinMessages()) {
                        plugin.msg(player, s);
                    }
                }

                if (getStage() == Stage.PLAYING) {
                    if (getLevel().getStartMessages() != null) {
                        for (String s : getLevel().getStartMessages()) {
                            plugin.msg(player, s);
                        }
                    }
                    plugin.msg(player, plugin.messageConfig.gameStarted);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                }
            });
            return true;
        }

        if (stage != Stage.WAITING && !force && !getLevel().isAllowLateJoin()) {
            plugin.msg(player, plugin.messageConfig.notInWaiting);
            return false;
        }
        if (players.size() >= getLevel().getMaxPlayers() && !force && !getLevel().isAllowOverfull()) {
            placeholder().message(player, plugin.messageConfig.maxPlayerReached);
            return false;
        }

        PlayerData playerData = plugin.playerDataManager.getData(player);

        if (playerData.getTicket() < getLevel().getTicketCost() && !force) {
            placeholder().message(player, plugin.messageConfig.insufficientTicket);
            return false;
        }

        if (terminating || !players.add(player.getUniqueId()))
            return false;

        playerData.reduceTicket(getLevel().getTicketCost());

        Placeholder placeholder = placeholder().add("player", player);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            placeholder.message(p, plugin.messageConfig.joinMessage);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.5f);
        }

        CompletableFuture<Void> sync = syncUpdatePlayerState(player);
        if (getStage() == Stage.PLAYING) {
            sync.thenRun(() -> {
                if (getLevel().getStartMessages() != null) {
                    for (String s : getLevel().getStartMessages()) {
                        plugin.msg(player, s);
                    }
                }
                plugin.msg(player, plugin.messageConfig.gameStarted);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            });
        }

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

        player.resetPlayerTime();
        player.resetPlayerWeather();
        player.teleportAsync(plugin.mainConfig.spawnLocation);

        return true;
    }

    public void handleBossDeath(MythicMobDeathEvent event) {
        String mobId = event.getMobType().getInternalName();
        plugin.debug(2, "Room %s has boss %s killed", id, mobId);
        Integer count = objectiveRequirements.get(mobId); // objectives must not have level
        if (count == null || count <= 0) return;
        if (count == 1) {
            objectiveRequirements.remove(mobId);
        } else {
            objectiveRequirements.put(mobId, count - 1);
        }

        if (event.getKiller() instanceof Player) {
            Placeholder placeholder = placeholder()
                    .add("boss", event.getMob().getDisplayName())
                    .add("killer", event.getKiller())
                    .add("player", event.getKiller());
            for (String reward : getLevel().getBossKillRewards()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder.replace(reward));
            }
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                placeholder.message(p, plugin.messageConfig.killMessage);
            }
        }

        if (objectiveRequirements.isEmpty() && !getLevel().isAllowOverachieve()) {
            stage = Stage.ENDING;
            completeTime = timeCounter;
            timeCounter = 0;
            won = true;
            syncEndGame();
        }
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setCancelled(true);
        event.setReviveHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        if (separators.containsKey(player.getUniqueId()) || stage != Stage.PLAYING) {
            return;
        }

        player.setGameMode(PhoBan.SPECTATOR_GAMEMODE);
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
                .add("maxPlayers", getLevel().isAllowOverfull() ? "∞" : getLevel().getMaxPlayers())
                .add("difficulty", difficulty)
                .add("challengeLevel", difficulty == Difficulty.CHALLENGE ? Integer.toString(challengeLevel+1) : "")
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
        return Objects.requireNonNull(getConfig().getLevel(difficulty));
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

    @NotNull
    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
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
            i = getLevel().getPlayingTime() - timeCounter;
        } else if (stage == Stage.WAITING) {
            i = plugin.mainConfig.roomSettings.waitingTime - timeCounter;
        }
        return Math.max(0, i);
    }

    public Map<String, Integer> getObjectiveRequirements() {
        return objectiveRequirements;
    }

    public int getChallengeLevel() {
        return challengeLevel;
    }
}
