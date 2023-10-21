package dev.anhcraft.phoban.integration;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.util.TimeUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderBridge extends PlaceholderExpansion {
    private static final String ROOM_NAME = "room_name";
    private static final String ROOM_REGION = "room_region";
    private static final String ROOM_STAGE = "room_stage";
    private static final String ROOM_DIFFICULTY = "room_difficulty";
    private static final String ROOM_CHALLENGE_LEVEL = "room_challenge_level";
    private static final String ROOM_PLAYERS = "room_players";
    private static final String ROOM_MAX_PLAYERS = "room_max_players";
    private static final String ROOM_MIN_PLAYERS = "room_min_players";
    private static final String ROOM_SEPARATORS = "room_separators";
    private static final String ROOM_RESPAWNS = "room_respawns";
    private static final String ROOM_MAX_RESPAWNS = "room_max_respawns";
    private static final String ROOM_OBJECTIVE_LEFT = "room_objective_left";
    private static final String ROOM_TIME = "room_time";
    private static final String ROOM_TIME_LEFT = "room_time_left";
    private static final String TICKETS = "tickets";
    private static final String TOTAL_WINS = "total_wins";
    private static final String TOTAL_LOSSES = "total_losses";
    private static final String TOTAL_MATCHES = "total_matches";
    private final PhoBan plugin;

    public PlaceholderBridge(PhoBan plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "phoban";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (!(player instanceof Player))
            return null;

        switch (params) {
            case ROOM_NAME -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : r.getConfig().getName();
            }
            case ROOM_REGION -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : r.getConfig().getRegion();
            }
            case ROOM_STAGE -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : plugin.messageConfig.stage.get(r.getStage());
            }
            case ROOM_DIFFICULTY -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : plugin.messageConfig.difficulty.get(r.getDifficulty());
            }
            case ROOM_CHALLENGE_LEVEL -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getChallengeLevel());
            }
            case ROOM_PLAYERS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getPlayers().size());
            }
            case ROOM_MAX_PLAYERS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : (r.getLevel().isAllowOverfull() ? "∞" : String.valueOf(r.getLevel().getMaxPlayers()));
            }
            case ROOM_MIN_PLAYERS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getLevel().getMinPlayers());
            }
            case ROOM_SEPARATORS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getSeparators().size());
            }
            case ROOM_RESPAWNS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getRespawnChances().getOrDefault(player.getUniqueId(), 0));
            }
            case ROOM_MAX_RESPAWNS -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : String.valueOf(r.getLevel().getRespawnChances());
            }
            case ROOM_TIME -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : TimeUtils.format(r.getTimeCounter());
            }
            case ROOM_TIME_LEFT -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "" : TimeUtils.format(r.getTimeLeft());
            }
            case ROOM_OBJECTIVE_LEFT -> {
                Room r = plugin.gameManager.getRoom(player.getUniqueId());
                return r == null ? "0" : String.valueOf(r.getObjectiveRequirements().values().stream().mapToInt(Integer::intValue).sum());
            }
            case TICKETS -> {
                return String.valueOf(plugin.playerDataManager.getData((Player) player).getTicket());
            }
            case TOTAL_MATCHES -> {
                return String.valueOf(plugin.playerDataManager.getData((Player) player).streamGameHistory()
                        .map(GameHistory::getTotalPlayTimes)
                        .reduce(0, Integer::sum));
            }
            case TOTAL_WINS -> {
                return String.valueOf(plugin.playerDataManager.getData((Player) player).streamGameHistory()
                        .map(GameHistory::getTotalWinTimes)
                        .reduce(0, Integer::sum));
            }
            case TOTAL_LOSSES -> {
                return String.valueOf(plugin.playerDataManager.getData((Player) player).streamGameHistory()
                        .map(GameHistory::getTotalLossTimes)
                        .reduce(0, Integer::sum));
            }
        }

        return null;
    }
}
