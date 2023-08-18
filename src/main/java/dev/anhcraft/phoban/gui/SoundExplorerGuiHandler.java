package dev.anhcraft.phoban.gui;

import dev.anhcraft.palette.event.ClickEvent;
import dev.anhcraft.palette.ui.GuiHandler;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.util.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SoundExplorerGuiHandler extends GuiHandler {
    private int itemPerPage;
    private int maxPage;
    private int page;
    private float volume;
    private float pitch;

    @Override
    public void onPreOpen(@NotNull Player player) {
        volume = 1.0f;
        pitch = 1.0f;
        itemPerPage = locateComponent("sound").size();
        maxPage = Sound.values().length / itemPerPage;

        listen("previous", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                page = Math.max(0, page - 1);
                refresh();
            }
        });

        listen("next", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                page = Math.min(maxPage, page + 1);
                refresh();
            }
        });

        listen("volume", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                if (clickEvent.isLeftClick()) {
                    volume = Math.min(1.0f, volume + 0.1f);
                } else if (clickEvent.isRightClick()) {
                    volume = Math.max(0f, volume - 0.1f);
                }
                refresh();
            }
        });

        listen("pitch", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                if (clickEvent.isLeftClick()) {
                    pitch = Math.min(1.0f, pitch + 0.1f);
                } else if (clickEvent.isRightClick()) {
                    pitch = Math.max(0f, pitch - 0.1f);
                }
                refresh();
            }
        });

        refresh();
    }

    public void refresh() {
        resetBulk("sound");

        replaceItem("previous", (index, itemBuilder) -> Placeholder.create().add("page", page).add("maxPage", maxPage).replace(itemBuilder));
        replaceItem("next", (index, itemBuilder) -> Placeholder.create().add("page", page).add("maxPage", maxPage).replace(itemBuilder));
        replaceItem("volume", (index, itemBuilder) -> Placeholder.create().add("volume", volume).replace(itemBuilder));
        replaceItem("pitch", (index, itemBuilder) -> Placeholder.create().add("pitch", pitch).replace(itemBuilder));

        List<Integer> slots = new ArrayList<>(locateComponent("sound"));
        Collections.sort(slots);
        List<Sound> sounds = Arrays.stream(Arrays.copyOfRange(Sound.values(), page * itemPerPage, page * itemPerPage + itemPerPage)).filter(Objects::nonNull).toList();

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);

            if (i >= sounds.size()) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            Sound sound = sounds.get(i);

            replaceItem(slot, (index, itemBuilder) -> {
                return Placeholder.create().add("sound", sound.name()).add("volume", volume).add("pitch", pitch).replace(itemBuilder);
            });

            getSlot(slot).setEvents((ClickEvent) (clickEvent, player1, slot1) -> {
                if (clickEvent.isLeftClick()) {
                    if (clickEvent.isShiftClick()) {
                        new BukkitRunnable() {
                            private int times = 0;

                            @Override
                            public void run() {
                                player1.playSound(player1.getLocation(), sound, 1, 1);

                                if (++times >= 10) {
                                    cancel();
                                }
                            }
                        }.runTaskTimer(PhoBan.instance, 0, 20);
                    } else {
                        player1.playSound(player1.getLocation(), sound, 1, 1);
                    }
                } else if (clickEvent.isRightClick()) {
                    player1.stopSound(sound);
                }
            });
        }
    }
}