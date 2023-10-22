package dev.anhcraft.phoban.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.gui.GuiRegistry;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.util.TimeUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

import static org.bukkit.ChatColor.*;

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

    @Subcommand("profile")
    @CommandPermission("phoban.profile")
    @CommandCompletion("@players")
    public void profile(CommandSender sender, OfflinePlayer player) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(RED + throwable.getMessage());
                return;
            }
            sender.sendMessage(YELLOW+"This is the profile of "+WHITE+player.getName());
            sender.sendMessage(GREEN+"- Available ticket: "+WHITE+playerData.getTicket());
            playerData.streamPlayedRooms().forEach(room -> {
                GameHistory history = playerData.requireRoomHistory(room);
                sender.sendMessage(AQUA+"* Played room "+room);
                for (Difficulty value : Difficulty.values()) {
                    if (history.getPlayTimes(value) == 0) continue;
                    String msg;
                    if (value == Difficulty.CHALLENGE) {
                         msg = String.format(
                                "&7- %s %d:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)",
                                plugin.messageConfig.difficulty.get(value),
                                playerData.getChallengeLevel(room),
                                history.getWinTimes(value),
                                history.getLossTimes(value),
                                history.getPlayTimes(value),
                                TimeUtils.format(history.getBestCompleteTime(value)),
                                 ((double) history.getWinTimes(value))/history.getPlayTimes(value)*100
                        );
                    } else {
                        msg = String.format(
                                "&7- %s:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)",
                                plugin.messageConfig.difficulty.get(value),
                                history.getWinTimes(value),
                                history.getLossTimes(value),
                                history.getPlayTimes(value),
                                TimeUtils.format(history.getBestCompleteTime(value)),
                                ((double) history.getWinTimes(value))/history.getPlayTimes(value)*100
                        );
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
                String msg = String.format(
                                "&7> Total:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)",
                                history.getTotalWinTimes(),
                                history.getTotalLossTimes(),
                                history.getTotalPlayTimes(),
                                TimeUtils.format(history.getBestCompleteOfAllTime()),
                                ((double) history.getTotalWinTimes())/history.getTotalPlayTimes()*100
                );
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            });
        });
    }

    @Subcommand("quit")
    public void quit(Player player) {
        plugin.gameManager.attemptLeaveRoom(player);
    }

    @Subcommand("reload")
    @CommandPermission("phoban.reload")
    public void reload(CommandSender sender) {
        if (!plugin.gameManager.getActiveRoomIds().isEmpty()) {
            sender.sendMessage(RED + "There are active rooms in playing!");
            return;
        }
        plugin.reload();
        sender.sendMessage(GREEN + "Reloaded the plugin!");
    }

    @Subcommand("enable")
    @CommandPermission("phoban.enable")
    @CommandCompletion("@room")
    public void enable(CommandSender sender, String room) {
        RoomConfig rc = plugin.gameManager.getRoomConfig(room);
        if (rc == null) {
            sender.sendMessage(RED + "Room not found: " + room);
            return;
        }
        rc.setEnabled(true);
        sender.sendMessage(GREEN + "Enabled " + room);
    }

    @Subcommand("disable")
    @CommandPermission("phoban.disable")
    @CommandCompletion("@room")
    public void disable(CommandSender sender, String room) {
        RoomConfig rc = plugin.gameManager.getRoomConfig(room);
        if (rc == null) {
            sender.sendMessage(RED + "Room not found: " + room);
            return;
        }
        rc.setEnabled(false);
        sender.sendMessage(YELLOW + "Disabled " + room);
    }

    @Subcommand("list")
    @CommandPermission("phoban.list")
    public void list(CommandSender sender) {
        sender.sendMessage(GOLD + "All: " + String.join(",", plugin.gameManager.getRoomIds()));
        sender.sendMessage(GREEN + "Active: " + String.join(",", plugin.gameManager.getActiveRoomIds()));
    }

    @Subcommand("join")
    @CommandPermission("phoban.join")
    @CommandCompletion("@activeRoom @players")
    public void join(CommandSender sender, String room, @Optional Player target) {
        if (target == null) {
            if (sender instanceof Player)
                target = (Player) sender;
            else {
                sender.sendMessage(RED + "You must specify a player!");
                return;
            }
        }
        if (plugin.gameManager.getRoom(room) == null) {
            sender.sendMessage(RED + "Room not created: " + room);
            return;
        }
        plugin.gameManager.attemptJoinRoom(target, room, true);
    }

    @Subcommand("start")
    @CommandPermission("phoban.start")
    @CommandCompletion("@activeRoom")
    public void start(CommandSender sender, String room) {
        plugin.gameManager.tryStart(room);
        sender.sendMessage(GREEN + "Started " + room);
    }

    @Subcommand("end")
    @CommandPermission("phoban.end")
    @CommandCompletion("@activeRoom")
    public void end(CommandSender sender, String room) {
        plugin.gameManager.tryEnd(room);
        sender.sendMessage(GREEN + "Ended " + room);
    }

    @Subcommand("terminate")
    @CommandPermission("phoban.terminate")
    @CommandCompletion("@activeRoom")
    public void terminate(CommandSender sender, String room) {
        plugin.gameManager.tryTerminate(room);
        sender.sendMessage(GREEN + "Terminated " + room);
    }

    @Subcommand("reset respawn")
    @CommandPermission("phoban.reset.respawn")
    @CommandCompletion("@activeRoom @players")
    public void resetRespawn(CommandSender sender, String room, @Optional Player target) {
        if (target == null) {
            if (sender instanceof Player)
                target = (Player) sender;
            else {
                sender.sendMessage(RED + "You must specify a player!");
                return;
            }
        }
        Room r = plugin.gameManager.getRoom(room);
        if (r == null) {
            sender.sendMessage(RED + "Room not created: " + room);
            return;
        }
        r.getRespawnChances().remove(target.getUniqueId());
        sender.sendMessage(GREEN + "Reset respawn chances for " + target.getName());
    }

    @Subcommand("tp")
    @CommandPermission("phoban.tp")
    @CommandCompletion("@room @players")
    public void tp(CommandSender sender, String room, @Optional Player target) {
        if (target == null) {
            if (sender instanceof Player)
                target = (Player) sender;
            else {
                sender.sendMessage(RED + "You must specify a player!");
                return;
            }
        }
        RoomConfig rc = plugin.gameManager.getRoomConfig(room);
        if (rc == null) {
            sender.sendMessage(RED + "Room not found: " + room);
            return;
        }
        target.teleport(rc.getSpawnLocation());
    }

    @Subcommand("reset data")
    @CommandPermission("phoban.reset.data")
    @CommandCompletion("@players")
    public void resetData(CommandSender sender, OfflinePlayer player) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(RED + throwable.getMessage());
                return;
            }
            playerData.reset();
            sender.sendMessage(GREEN + "Reset player data: " + player.getName());
        });
    }

    @Subcommand("sound")
    @CommandPermission("phoban.sound")
    public void sound(Player player) {
        GuiRegistry.openSoundExplorer(player);
    }

    @Subcommand("ticket add")
    @CommandPermission("phoban.ticket.add")
    @CommandCompletion("@players")
    public void addTicket(CommandSender sender, OfflinePlayer player, int amount) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(RED + throwable.getMessage());
                return;
            }
            playerData.addTicket(amount);
            sender.sendMessage(GREEN + "Added " + amount + " tickets for " + player.getName());
        });
    }

    @Subcommand("ticket set")
    @CommandPermission("phoban.ticket.set")
    @CommandCompletion("@players")
    public void setTicket(CommandSender sender, OfflinePlayer player, int amount) {
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(RED + "This player has not played before!");
            return;
        }
        if (!player.isOnline())
            sender.sendMessage(YELLOW + "Fetching player data as he is currently offline...");

        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(RED + throwable.getMessage());
                return;
            }
            playerData.setTicket(amount);
            sender.sendMessage(GREEN + "Set " + amount + " tickets for " + player.getName());
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
