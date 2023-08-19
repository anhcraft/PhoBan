package dev.anhcraft.phoban.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.anhcraft.jvmkit.utils.FileUtil;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.gui.GuiRegistry;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.ConfigHelper;
import dev.anhcraft.phoban.util.Placeholder;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class GameManager {
    private final PhoBan plugin;
    private final Map<UUID, String> player2room = new HashMap<>();
    private final Multimap<String, String> boss2room = HashMultimap.create();
    private final Map<String, Room> roomMap = new HashMap<>();
    private final Map<String, RoomConfig> roomConfigMap = new HashMap<>();

    public GameManager(PhoBan plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // TODO handle room end here
        for (Room room : roomMap.values()) {
            room.syncTerminate();
        }

        roomConfigMap.clear();
        boss2room.clear();
        player2room.clear();
        roomMap.clear();

        File dir = new File(plugin.getDataFolder(), "rooms");

        if (!dir.exists()) {
            dir.mkdirs();

            plugin.requestConfig("rooms/room-1.yml");
            plugin.requestConfig("rooms/room-2.yml");
        }

        FileUtil.streamFiles(dir).forEach(file -> {
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
            plugin.msg(player, plugin.messageConfig.alreadyJoined);
            return;
        }

        Room room = roomMap.get(roomId);
        if (room != null) {
            GuiRegistry.openRoomSelector(player);
            return;
        }

        PlayerData pd = plugin.playerDataManager.getData(player);
        long createUnlockTime = pd.getLastCreateRoomTime() + plugin.mainConfig.roomCreateCooldown * 1000L;
        if (createUnlockTime > System.currentTimeMillis()) {
            Placeholder.create().addTime("cooldown", (createUnlockTime - System.currentTimeMillis()) / 1000L)
                    .message(player, plugin.messageConfig.createRoomCooldown);
            return;
        }

        // create here
        room = new Room(plugin, roomId, difficulty);
        room.initialize();

        if(room.handleJoinRoom(player)) {
            player2room.put(player.getUniqueId(), roomId);
            roomMap.put(roomId, room);
            boss2room.put(room.getLevel().getBossId(), roomId);
            pd.setLastCreateRoomTime(System.currentTimeMillis());
        }
    }

    public void attemptJoinRoom(Player player, String roomId) {
        if (player2room.containsKey(player.getUniqueId())) {
            plugin.msg(player, plugin.messageConfig.alreadyJoined);
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

    public void rejoinRoom(Player player) {
        String id = player2room.get(player.getUniqueId());
        if (id == null) {
            return;
        }

        Room room = roomMap.get(id);
        if (room == null) {
            player2room.remove(player.getUniqueId());
            return;
        }

        room.handleJoinRoom(player);
    }

    public void attemptLeaveRoom(Player player) {
        Room room = getRoom(player.getUniqueId());
        if (room == null) {
            plugin.msg(player, plugin.messageConfig.notJoined);
            return;
        }
        if (room.handleLeaveRoom(player)) {
            player2room.remove(player.getUniqueId());
        }
    }

    public void destroyRoom(String room) {
        roomMap.remove(room);
        player2room.values().removeIf(s -> s.equals(room));
        boss2room.values().removeIf(s -> s.equals(room));
    }

    public void handleBossDeath(MythicMobDeathEvent event) {
        Collection<String> rooms = boss2room.get(event.getMobType().getInternalName());

        for (Iterator<String> it = rooms.iterator(); it.hasNext(); ) {
            String room = it.next();
            Room r = getRoom(room);
            if (r == null) {
                it.remove();
                continue;
            }

            if (r.hasLocation(event.getEntity().getLocation()) && r.getLevel().getBossId().equals(event.getMobType().getInternalName())) {
                r.handleBossDeath(event);
                break;
            }
        }
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        Room room = getRoom(event.getEntity().getUniqueId());
        if (room != null) {
            room.handlePlayerDeath(event);
        }
    }

    public boolean shouldBlockDamage(UUID uniqueId) {
        Room room = getRoom(uniqueId);
        if (room != null) {
            return room.getStage() != Stage.PLAYING;
        }
        return false;
    }

    public void tryTerminate(String room) {
        Room r = roomMap.get(room);
        if (r != null) {
            r.syncTerminate();
        }
    }
}
