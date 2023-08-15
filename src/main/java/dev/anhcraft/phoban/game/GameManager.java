package dev.anhcraft.phoban.game;

import dev.anhcraft.jvmkit.utils.FileUtil;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.gui.GuiRegistry;
import dev.anhcraft.phoban.util.ConfigHelper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private PhoBan plugin;
    private Map<UUID, String> player2room = new HashMap<>();
    private Map<String, Room> roomMap = new HashMap<>();
    private Map<String, RoomConfig> roomConfigMap = new HashMap<>();

    public GameManager(PhoBan plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // TODO handle room end here

        roomConfigMap.clear();

        FileUtil.streamFiles(new File(plugin.getDataFolder(), "rooms")).forEach(file -> {
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
            RoomConfig roomConfig = ConfigHelper.load(RoomConfig.class, conf);
            roomConfigMap.put(file.getName().split("\\.")[0], roomConfig);
        });
    }

    public Collection<String> getRoomIds() {
        return roomConfigMap.keySet();
    }

    public Collection<String> getActiveRoomIds() {
        return roomMap.keySet();
    }

    public Collection<RoomConfig> getRoomConfigs() {
        return roomConfigMap.values();
    }

    public Collection<Room> getActiveRooms() {
        return roomMap.values();
    }

    @Nullable
    public RoomConfig getRoomConfig(String room) {
        return roomConfigMap.get(room);
    }

    @Nullable
    public Room getRoom(String room) {
        return roomMap.get(room);
    }

    @Nullable
    public String getRoomId(UUID player) {
        return player2room.get(player);
    }

    @Nullable
    public Room getRoom(UUID player) {
        return roomMap.get(player2room.get(player));
    }

    public void attemptCreateRoom(Player player, String roomId, Difficulty difficulty) {
        if (player2room.containsKey(player.getUniqueId())) {
            PhoBan.instance.msg(player, PhoBan.instance.messageConfig.alreadyJoined);
            return;
        }

        Room room = roomMap.get(roomId);
        if (room != null) {
            GuiRegistry.openRoomSelector(player);
            return;
        }

        // create here
        room = new Room(plugin, roomConfigMap.get(roomId), difficulty);
        roomMap.put(roomId, room);
        room.initialize();

        if(room.handleJoinRoom(player)) {
            player2room.put(player.getUniqueId(), roomId);
        }
    }

    public void attemptJoinRoom(Player player, String roomId) {
        if (player2room.containsKey(player.getUniqueId())) {
            PhoBan.instance.msg(player, PhoBan.instance.messageConfig.alreadyJoined);
            return;
        }

        Room room = roomMap.get(roomId);
        if (room == null) {
            GuiRegistry.openDifficultySelector(player, roomId);
            return;
        }

        // join here
        if(room.handleJoinRoom(player)) {
            player2room.put(player.getUniqueId(), roomId);
        }
    }

    public void attemptLeaveRoom(Player player) {
        Room room = getRoom(player.getUniqueId());
        if (room == null) {
            PhoBan.instance.msg(player, PhoBan.instance.messageConfig.notJoined);
            return;
        }
        if (room.handleLeaveRoom(player)) {
            player2room.remove(player.getUniqueId());
        }
    }
}
