package dev.anhcraft.phoban.gui;

import dev.anhcraft.palette.ui.Gui;
import org.bukkit.entity.Player;

public class GuiRegistry {
    public static Gui INBOX;

    public static void openInboxGui(Player player) {
        INBOX.open(player, new InboxGuiHandler());
    }
}
