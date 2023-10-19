package dev.anhcraft.phoban.gui;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import dev.anhcraft.palette.event.ClickEvent;
import dev.anhcraft.palette.ui.GuiHandler;
import dev.anhcraft.palette.util.ItemReplacer;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.LevelConfig;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.game.RoomRequirement;
import dev.anhcraft.phoban.game.Stage;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.storage.PlayerData;
import dev.anhcraft.phoban.util.Placeholder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomSelectorGuiHandler extends GuiHandler implements AutoRefresh {
    private PlayerData playerData;

    @Override
    public void onPreOpen(@NotNull Player player) {
        replaceItem("info", new ItemReplacer() {
            @Override
            public @NotNull ItemBuilder apply(int i, @NotNull ItemBuilder itemBuilder) {
                return itemBuilder.replaceDisplay(s -> PlaceholderAPI.setPlaceholders(player, s));
            }
        });

        if (PhoBan.instance.mainConfig.infoItemCmd != null) {
            listen("info", new ClickEvent() {
                @Override
                public void onClick(@NotNull InventoryClickEvent inventoryClickEvent, @NotNull Player player, int i) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Placeholder.create().add("player", player.getName()).replace(PhoBan.instance.mainConfig.infoItemCmd));
                }
            });
        }

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

            if (roomConfig == null || (!roomConfig.isEnabled() && !player.hasPermission("phoban.admin"))) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            RoomRequirement failedRequirement = null;

            if (roomConfig.getRoomRequirement() != null && !playerData.hasWonRoom(roomConfig.getRoomRequirement())) {
                failedRequirement = roomConfig.getRoomRequirement();
            }

            Room room = PhoBan.instance.gameManager.getRoom(roomId);
            Stage stage = room == null ? Stage.AVAILABLE : room.getStage();
            GameHistory history = playerData.getGameHistory(roomId);
            Placeholder placeholder = Placeholder.create()
                    .add("stage", stage)
                    .addUnknown("playTimes")
                    .addUnknown("wins")
                    .addUnknown("losses")
                    .addUnknown("winRatio")
                    .addUnknown("bestCompleteTime");

            if (room != null) {
                Difficulty difficulty = room.getDifficulty();

                placeholder.add("currentPlayers", room.getPlayers().size())
                        .add("maxPlayers", room.getLevel().getMaxPlayers())
                        .add("ticketCost", room.getLevel().getTicketCost())
                        .add("difficulty", room.getDifficulty())
                        .addTime("timeLeft", room.getTimeLeft());

                if (history != null) {
                    placeholder.add("playTimes", history.getPlayTimes(difficulty))
                            .add("wins", history.getWinTimes(difficulty))
                            .add("losses", history.getLossTimes(difficulty))
                            .addRatio("winRatio", history.getWinTimes(difficulty), history.getPlayTimes(difficulty))
                            .addTime("bestCompleteTime", history.getBestCompleteTime(difficulty));
                }

                if (failedRequirement == null && difficulty.ordinal() > 0 &&
                        !playerData.hasWonRoom(roomId, Difficulty.values()[difficulty.ordinal()-1])) {
                    failedRequirement = new RoomRequirement(roomId, Difficulty.values()[difficulty.ordinal()-1]);
                }
            } else if (history != null) {
                placeholder.add("playTimes", history.getTotalPlayTimes())
                        .add("wins", history.getTotalWinTimes())
                        .add("losses", history.getTotalLossTimes())
                        .addRatio("winRatio", history.getTotalWinTimes(), history.getTotalPlayTimes())
                        .addTime("bestCompleteTime", history.getBestCompleteOfAllTime());
            }

            if (failedRequirement != null) {
                RoomConfig required = PhoBan.instance.gameManager.getRoomConfig(failedRequirement.name());
                if (required == null) {
                    resetItem(slot);
                } else {
                    placeholder.add("requiredRoom", required.getName())
                            .add("requiredDifficulty", failedRequirement.minimumDifficulty());

                    replaceItem(slot, (index, itemBuilder) -> {
                        itemBuilder.material(roomConfig.getIcon());
                        itemBuilder.name(roomConfig.getName());
                        itemBuilder.lore(roomConfig.getDescription());
                        itemBuilder.lore().addAll(GuiRegistry.ROOM_SELECTOR.roomLockedTrailer.get(stage));
                        return placeholder.replace(itemBuilder);
                    });
                }
                getSlot(slot).clearEvents();
                continue;
            }

            replaceItem(slot, (index, itemBuilder) -> {
                itemBuilder.material(roomConfig.getIcon());
                itemBuilder.name(roomConfig.getName());
                itemBuilder.lore(roomConfig.getDescription());
                itemBuilder.lore().addAll(GuiRegistry.ROOM_SELECTOR.roomLoreTrailer.get(stage));
                return placeholder.replace(itemBuilder);
            });

            getSlot(slot).setEvents((ClickEvent) (clickEvent, player1, slot1) -> {
                PhoBan.instance.gameManager.attemptJoinRoom(player, roomId, false);
            });
        }
    }
}
