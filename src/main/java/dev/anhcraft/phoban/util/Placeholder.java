package dev.anhcraft.phoban.util;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Stage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder {
    public static final Pattern INFO_PLACEHOLDER_PATTERN = Pattern.compile("\\{[a-zA-Z0-9-_]+}");

    public static Placeholder create() {
        return new Placeholder();
    }

    private final HashMap<String, String> placeholders = new HashMap<>();

    public Placeholder add(String key, Object value) {
        placeholders.put(key, format(value));
        return this;
    }

    public Placeholder addTime(String key, long timeSec) {
        placeholders.put(key, TimeUtils.format(timeSec));
        return this;
    }

    public Placeholder addRatio(String key, double a, double b) {
        placeholders.put(key, Math.abs(b) < 0.001 ? "-" : String.format("%.02f%%", a / b * 100));
        return this;
    }

    public Placeholder addUnknown(String key) {
        placeholders.put(key, "-");
        return this;
    }

    public String replace(String str) {
        if (str == null || str.isEmpty()) return str;
        Matcher m = INFO_PLACEHOLDER_PATTERN.matcher(str);
        StringBuilder sb = new StringBuilder(str.length());
        while (m.find()) {
            String p = m.group();
            String s = p.substring(1, p.length() - 1).trim();
            m.appendReplacement(sb, placeholders.getOrDefault(s, "(null)"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void message(CommandSender sender, String str) {
        PhoBan.instance.msg(sender, replace(str));
    }

    public void actionBar(Player player, String str) {
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(replace(str)));
    }

    public void messageRaw(CommandSender sender, String str) {
        PhoBan.instance.rawMsg(sender, replace(str));
    }

    public ItemBuilder replace(ItemBuilder itemBuilder) {
        itemBuilder.replaceDisplay(this::replace);
        return itemBuilder;
    }

    private String format(Object v) {
        if (v == null) {
            return "(null)";
        } else if (v instanceof Double) {
            return String.format("%.02f", v);
        } else if (v instanceof Float) {
            return String.format("%.02f", v);
        } else if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        } else if (v instanceof String) {
            return (String) v;
        } else if (v instanceof Stage) {
            return PhoBan.instance.messageConfig.stage.get(v);
        } else if (v instanceof Difficulty) {
            return PhoBan.instance.messageConfig.difficulty.get(v);
        } else if (v instanceof OfflinePlayer) {
            return ((OfflinePlayer) v).getName();
        } else if (v instanceof Entity) {
            String s = ((Entity) v).getCustomName();
            return s == null ? ((Entity) v).getType().name() : s;
        }
        return "(object)";
    }
}
