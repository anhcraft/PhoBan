package dev.anhcraft.phoban.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.gui.GuiRegistry;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
        GuiRegistry.openRoomSelector(player);
    }

    @Subcommand("quit")
    public void quit(Player player) {
        plugin.gameManager.attemptLeaveRoom(player);
    }

    @Subcommand("reload")
    @CommandPermission("phoban.reload")
    public void reload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "Reloaded the plugin!");
    }

    @Subcommand("list")
    @CommandPermission("phoban.list")
    public void list(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "All: " + String.join(",", plugin.gameManager.getRoomIds()));
        sender.sendMessage(ChatColor.GREEN + "Active: " + String.join(",", plugin.gameManager.getActiveRoomIds()));
    }

    @Subcommand("terminate")
    @CommandPermission("phoban.terminate")
    @CommandCompletion("@activeRoom")
    public void terminate(CommandSender sender, String room) {
        plugin.gameManager.tryTerminate(room);
        sender.sendMessage(ChatColor.GREEN + "Terminated " + room);
    }

    @Subcommand("tp")
    @CommandPermission("phoban.tp")
    @CommandCompletion("@room")
    public void tp(Player sender, String room) {
        RoomConfig rc = plugin.gameManager.getRoomConfig(room);
        if (rc == null) {
            sender.sendMessage(ChatColor.RED + "Room not found: " + room);
            return;
        }
        sender.teleport(rc.getSpawnLocation());
    }

    @Subcommand("sound")
    @CommandPermission("phoban.sound")
    public void sound(Player player) {
        GuiRegistry.openSoundExplorer(player);
    }

    @Subcommand("ticket add")
    @CommandPermission("phoban.ticket.add")
    public void addTicket(CommandSender sender, OfflinePlayer player, int amount) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(ChatColor.YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + throwable.getMessage());
                return;
            }
            playerData.addTicket(amount);
            sender.sendMessage(ChatColor.GREEN + "Added " + amount + " tickets for " + player.getName());
        });
    }

    @Subcommand("ticket set")
    @CommandPermission("phoban.ticket.set")
    public void setTicket(CommandSender sender, OfflinePlayer player, int amount) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(ChatColor.YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + throwable.getMessage());
                return;
            }
            playerData.setTicket(amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + amount + " tickets for " + player.getName());
        });
    }

    @Subcommand("getpos")
    @CommandPermission("phoban.getpos")
    public void getpos(Player p) {
        var loc = p.getLocation();
        var locString = String.join(" ",
                loc.getWorld().getName(),
                Integer.toString(loc.getBlockX()),
                Integer.toString(loc.getBlockY()),
                Integer.toString(loc.getBlockZ()),
                Integer.toString(Math.round(loc.getYaw())),
                Integer.toString(Math.round(loc.getPitch())));

        var text = new ComponentBuilder("Your location is ").color(net.md_5.bungee.api.ChatColor.WHITE)
                .append(locString).color(net.md_5.bungee.api.ChatColor.BLUE).underlined(true)
                .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, locString))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Yes click here")))
                .append(" (click the underlined text to copy)").reset().color(net.md_5.bungee.api.ChatColor.GRAY).italic(true)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("NO NOT THIS TEXT!!1!")))
                .create();
        p.spigot().sendMessage(text);
    }
}
