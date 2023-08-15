package dev.anhcraft.phoban.util;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Stage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder {
    public static final Pattern INFO_PLACEHOLDER_PATTERN = Pattern.compile("<[a-zA-Z0-9:_]+>");

    public static Placeholder create() {
        return new Placeholder();
    }

    private final HashMap<String, String> placeholders = new HashMap<>();

    public Placeholder add(String key, Object value) {
        placeholders.put(key, format(value));
        return this;
    }

    public Placeholder addTime(String key, long timeSec) {
        long hours = timeSec / 3600;
        long minutes = (timeSec % 3600) / 60;
        long seconds = timeSec % 60;
        placeholders.put(key, String.format("%02d:%02d:%02d", hours, minutes, seconds));
        return this;
    }

    public String replace(String str) {
        if (str == null || str.isEmpty()) return str;
        Matcher m = INFO_PLACEHOLDER_PATTERN.matcher(str);
        StringBuilder sb = new StringBuilder(str.length());
        while (m.find()) {
            String p = m.group();
            String s = p.substring(1, p.length() - 1).trim();
            m.appendReplacement(sb, s);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void message(Player player, String str) {
        PhoBan.instance.msg(player, replace(str));
    }

    public void messageRaw(Player player, String str) {
        PhoBan.instance.rawMsg(player, replace(str));
    }

    public ItemBuilder replace(ItemBuilder itemBuilder) {
        itemBuilder.replaceDisplay(this::replace);
        return itemBuilder;
    }

    private String format(Object v) {
        if (v == null) {
            return "(null)";
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
        }
        return "(object)";
    }
}
