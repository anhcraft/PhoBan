package dev.anhcraft.phoban.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import dev.anhcraft.mailbox.Mailbox;
import dev.anhcraft.mailbox.gui.GuiRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("m|mail|mailbox")
public class MainCommand extends BaseCommand {
    private final Mailbox plugin;

    public MainCommand(Mailbox plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    @CatchUnknown
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Default
    public void openMenu(Player player) {
        GuiRegistry.openInboxGui(player);
    }
}
