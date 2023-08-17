package dev.anhcraft.phoban.gui;

import dev.anhcraft.palette.event.ClickEvent;
import dev.anhcraft.palette.ui.GuiHandler;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.LevelConfig;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.game.Stage;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomSelectorGuiHandler extends GuiHandler implements AutoRefresh {
    private PlayerData playerData;

    @Override
    public void onPreOpen(@NotNull Player player) {
        playerData = PhoBan.instance.playerDataManager.getData(player);
        refresh(player);
    }

    @Override
    public void refresh(Player player) {
        resetBulk("room");

        List<Integer> slots = new ArrayList<>(locateComponent("room"));
        Collections.sort(slots);
        List<String> roomIds = new ArrayList<>(PhoBan.instance.gameManager.getRoomIds());

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);

            if (i >= roomIds.size()) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            String roomId = roomIds.get(i);
            RoomConfig roomConfig = PhoBan.instance.gameManager.getRoomConfig(roomId);

            if (roomConfig == null) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            if (roomConfig.getRoomRequirement() != null && !playerData.hasPlayedRoom(roomConfig.getRoomRequirement())) {
                RoomConfig required = PhoBan.instance.gameManager.getRoomConfig(roomConfig.getRoomRequirement().name());
                if (required == null) {
                    resetItem(slot);
                } else {
                    replaceItem(slot, (index, itemBuilder) -> {
                        itemBuilder.material(roomConfig.getIcon());
                        itemBuilder.name(roomConfig.getName());
                        itemBuilder.lore(roomConfig.getDescription());
                        itemBuilder.lore().addAll(GuiRegistry.ROOM_SELECTOR.roomLockedTrailer);
                        return Placeholder.create()
                                .add("requiredRoom", required.getName())
                                .add("requiredDifficulty", roomConfig.getRoomRequirement().minimumDifficulty())
                                .replace(itemBuilder);
                    });
                }
                getSlot(slot).clearEvents();
                continue;
            }

            Room room = PhoBan.instance.gameManager.getRoom(roomId);
            Stage stage = room == null ? Stage.AVAILABLE : room.getStage();
            GameHistory history = playerData.getGameHistory(roomId);
            Placeholder placeholder = Placeholder.create().add("stage", stage);

            if (room != null) {
                Difficulty difficulty = room.getDifficulty();
                LevelConfig levelConfig = roomConfig.getLevel(room.getDifficulty());

                placeholder.add("playTimes", history == null ? 0 : history.getPlayTimes(difficulty))
                        .addTime("bestCompleteTime", history == null ? 0 : history.getBestCompleteTime(difficulty))
                        .add("currentPlayers", room.getPlayers().size())
                        .add("maxPlayers", levelConfig.getMaxPlayers())
                        .add("difficulty", room.getDifficulty())
                        .addTime("timeLeft", room.getTimeLeft());

                if (difficulty.ordinal() > 0 && !playerData.hasPlayedRoom(roomId, Difficulty.values()[difficulty.ordinal()-1])) {
                    placeholder.add("requiredRoom", roomConfig.getName())
                            .add("requiredDifficulty", Difficulty.values()[difficulty.ordinal()-1]);
                    replaceItem(slot, (index, itemBuilder) -> {
                        itemBuilder.material(roomConfig.getIcon());
                        itemBuilder.name(roomConfig.getName());
                        itemBuilder.lore(roomConfig.getDescription());
                        itemBuilder.lore().addAll(GuiRegistry.ROOM_SELECTOR.roomLockedTrailer);
                        return placeholder.replace(itemBuilder);
                    });
                    getSlot(slot).clearEvents();
                    continue;
                }
            } else {
                placeholder.add("playTimes", history == null ? 0 : history.getTotalPlayTimes())
                        .addTime("bestCompleteTime", history == null ? 0 : history.getBestCompleteOfAllTime());
            }

            replaceItem(slot, (index, itemBuilder) -> {
                itemBuilder.material(roomConfig.getIcon());
                itemBuilder.name(roomConfig.getName());
                itemBuilder.lore(roomConfig.getDescription());
                itemBuilder.lore().addAll(GuiRegistry.ROOM_SELECTOR.roomLoreTrailer.get(stage));
                return placeholder.replace(itemBuilder);
            });

            getSlot(slot).setEvents((ClickEvent) (clickEvent, player1, slot1) -> {
                PhoBan.instance.gameManager.attemptJoinRoom(player, roomId);
            });
        }
    }
}
