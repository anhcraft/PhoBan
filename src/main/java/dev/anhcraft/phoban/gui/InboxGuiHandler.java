package dev.anhcraft.phoban.gui;

import dev.anhcraft.mailbox.Mailbox;
import dev.anhcraft.mailbox.api.data.PlayerData;
import dev.anhcraft.mailbox.api.query.*;
import dev.anhcraft.palette.ui.GuiHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InboxGuiHandler extends GuiHandler implements AutoRefresh {
    private PlayerData playerData;
    private Query query;

    @Override
    public void onPreOpen(@NotNull Player player) {
        playerData = Mailbox.getApi().getPlayerData(player);
        query = new QueryBuilder()
                .filter(new FilterBuilder()
                        .sendDate(DateRange.untilNow())
                )
                .order(Field.SEND_DATE, Order.DESC)
                .order(Field.EXPIRY_DATE, Order.DESC)
                .build();
        refresh(player);
    }

    @Override
    public void refresh(Player player) {

    }
}
