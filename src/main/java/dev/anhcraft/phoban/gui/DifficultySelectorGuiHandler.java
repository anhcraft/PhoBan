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

public class DifficultySelectorGuiHandler extends GuiHandler implements AutoRefresh {
    private final String roomId;
    private PlayerData playerData;

    public DifficultySelectorGuiHandler(String roomId) {
        this.roomId = roomId;
    }

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

        listen("quit", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                GuiRegistry.openRoomSelector(player);
            }
        });

        refresh(player);
    }

    @Override
    public void refresh(Player player) {
        resetBulk("difficulty");

        List<Integer> slots = new ArrayList<>(locateComponent("difficulty"));
        Collections.sort(slots);
        List<Difficulty> difficulties = List.of(Difficulty.values());

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);

            if (i >= difficulties.size()) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            RoomConfig roomConfig = PhoBan.instance.gameManager.getRoomConfig(roomId);
            if (roomConfig == null || (!roomConfig.isEnabled() && !player.hasPermission("phoban.admin"))) {
                GuiRegistry.openRoomSelector(player);
                return;
            }

            Difficulty difficulty = difficulties.get(i);
            Room room = PhoBan.instance.gameManager.getRoom(roomId);

            if (room != null) {
                GuiRegistry.openRoomSelector(player);
                return;
            }

            if (difficulty.ordinal() > 0 && !playerData.hasWonRoom(roomId, Difficulty.values()[difficulty.ordinal()-1])) {
                Placeholder placeholder = Placeholder.create()
                        .add("dungeon", roomConfig.getName())
                        .add("difficulty", difficulty)
                        .add("requiredRoom", roomConfig.getName())
                        .add("requiredDifficulty", Difficulty.values()[difficulty.ordinal()-1]);
                replaceItem(slot, (index, itemBuilder) -> {
                    itemBuilder.material(roomConfig.getIcon());
                    itemBuilder.lore(roomConfig.getDescription());
                    itemBuilder.lore().addAll(GuiRegistry.DIFFICULTY_SELECTOR.roomLockedTrailer);
                    return placeholder.replace(itemBuilder);
                });
                getSlot(slot).clearEvents();
                continue;
            }

            LevelConfig levelConfig = roomConfig.getLevel(difficulty);
            GameHistory history = playerData.getGameHistory(roomId);
            Placeholder placeholder = Placeholder.create()
                    .add("dungeon", roomConfig.getName())
                    .addUnknown("playTimes")
                    .addUnknown("wins")
                    .addUnknown("losses")
                    .addUnknown("winRatio")
                    .addUnknown("bestCompleteTime")
                    .add("difficulty", difficulty)
                    .add("currentPlayers", 0)
                    .add("maxPlayers", levelConfig.getMaxPlayers())
                    .add("ticketCost", levelConfig.getTicketCost())
                    .add("difficulty", difficulty)
                    .add("stage", Stage.AVAILABLE);

            if (history != null) {
                placeholder.addRatio("winRatio", history.getWinTimes(difficulty), history.getPlayTimes(difficulty))
                        .add("playTimes", history.getPlayTimes(difficulty))
                        .addTime("bestCompleteTime", history.getBestCompleteTime(difficulty))
                        .add("wins", history.getWinTimes(difficulty))
                        .add("losses", history.getLossTimes(difficulty));
            }

            replaceItem(slot, (index, itemBuilder) -> {
                itemBuilder.material(roomConfig.getIcon());
                itemBuilder.lore(roomConfig.getDescription());
                itemBuilder.lore().addAll(GuiRegistry.DIFFICULTY_SELECTOR.roomLoreTrailer);
                return placeholder.replace(itemBuilder);
            });

            getSlot(slot).setEvents((ClickEvent) (clickEvent, player1, slot1) -> {
                PhoBan.instance.gameManager.attemptCreateRoom(player, roomId, difficulty);
            });
        }
    }
}