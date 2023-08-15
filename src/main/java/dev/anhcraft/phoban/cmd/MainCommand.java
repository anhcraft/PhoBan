package dev.anhcraft.phoban.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import dev.anhcraft.phoban.PhoBan;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("pb|phoban")
public class MainCommand extends BaseCommand {
    private final PhoBan plugin;

    public MainCommand(PhoBan plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    @CatchUnknown
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Default
    public void openMenu(Player player) {

    }

    @Subcommand("list")
    @CommandPermission("phoban.list")
    public void list(CommandSender sender) {

    }

    @Subcommand("quit")
    public void quit(Player player) {
        plugin.gameManager.attemptLeaveRoom(player);
    }
}
