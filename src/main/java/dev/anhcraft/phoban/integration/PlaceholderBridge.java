package dev.anhcraft.phoban.integration;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.util.TimeUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderBridge extends PlaceholderExpansion {
    private static final String ROOM_NAME = "room_name";
    private static final String ROOM_REGION = "room_region";
    private static final String ROOM_STAGE = "room_stage";
    private static final String ROOM_DIFFICULTY = "room_difficulty";
    private static final String ROOM_PLAYERS = "room_players";
    private static final String ROOM_MAX_PLAYERS = "room_max_players";
    private static final String ROOM_MIN_PLAYERS = "room_min_players";
    private static final String ROOM_SEPARATORS = "room_separators";
    private static final String ROOM_RESPAWNS = "room_respawns";
    private static final String ROOM_TIME = "room_time";
    private static final String ROOM_TIME_LEFT = "room_time_left";
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
        if (player == null)
            return null;

        if(params.equals(ROOM_NAME)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : r.getConfig().getName();
        } else if(params.equals(ROOM_REGION)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : r.getConfig().getRegion();
        } else if(params.equals(ROOM_STAGE)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : plugin.messageConfig.stage.get(r.getStage());
        } else if(params.equals(ROOM_DIFFICULTY)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : plugin.messageConfig.difficulty.get(r.getDifficulty());
        } else if(params.equals(ROOM_PLAYERS)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : String.valueOf(r.getPlayers().size());
        } else if(params.equals(ROOM_MAX_PLAYERS)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : String.valueOf(r.getLevel().getMaxPlayers());
        } else if(params.equals(ROOM_MIN_PLAYERS)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : String.valueOf(r.getLevel().getMinPlayers());
        } else if(params.equals(ROOM_SEPARATORS)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : String.valueOf(r.getSeparators().size());
        } else if(params.equals(ROOM_RESPAWNS)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : String.valueOf(r.getRespawnChances().getOrDefault(player.getUniqueId(), 0));
        } else if(params.equals(ROOM_TIME)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : TimeUtils.format(r.getTimeCounter());
        } else if(params.equals(ROOM_TIME_LEFT)){
            Room r = plugin.gameManager.getRoom(player.getUniqueId());
            return r == null ? "" : TimeUtils.format(r.getTimeLeft());
        }

        return null;
    }
}
