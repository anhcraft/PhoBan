package dev.anhcraft.phoban.util;

import org.bukkit.attribute.Attribute;

import java.util.HashMap;
import java.util.Map;

public class MobOptions {
    private Map<Attribute, Double> attributes = new HashMap<>();
    private boolean silent;
    private boolean glowing;

    public static MobOptions parse(Map<String, String> options) {
        MobOptions mobOptions = new MobOptions();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "health", "maxHealth" ->
                        mobOptions.attributes.put(Attribute.GENERIC_MAX_HEALTH, Double.parseDouble(entry.getValue()));
                case "followRange" ->
                        mobOptions.attributes.put(Attribute.GENERIC_FOLLOW_RANGE, Double.parseDouble(entry.getValue()));
                case "knockbackResistance" ->
                        mobOptions.attributes.put(Attribute.GENERIC_KNOCKBACK_RESISTANCE, Double.parseDouble(entry.getValue()));
                case "movementSpeed" ->
                        mobOptions.attributes.put(Attribute.GENERIC_MOVEMENT_SPEED, Double.parseDouble(entry.getValue()));
                case "flyingSpeed" ->
                        mobOptions.attributes.put(Attribute.GENERIC_FLYING_SPEED, Double.parseDouble(entry.getValue()));
                case "attackDamage" ->
                        mobOptions.attributes.put(Attribute.GENERIC_ATTACK_DAMAGE, Double.parseDouble(entry.getValue()));
                case "attackKnockback" ->
                        mobOptions.attributes.put(Attribute.GENERIC_ATTACK_KNOCKBACK, Double.parseDouble(entry.getValue()));
                case "attackSpeed" ->
                        mobOptions.attributes.put(Attribute.GENERIC_ATTACK_SPEED, Double.parseDouble(entry.getValue()));
                case "armor" ->
                        mobOptions.attributes.put(Attribute.GENERIC_ARMOR, Double.parseDouble(entry.getValue()));
                case "armorToughness" ->
                        mobOptions.attributes.put(Attribute.GENERIC_ARMOR_TOUGHNESS, Double.parseDouble(entry.getValue()));
                case "silent" -> mobOptions.silent = true;
                case "glowing" -> mobOptions.glowing = true;
            }
        }
        return mobOptions;
    }

    public Map<Attribute, Double> getAttributes() {
        return attributes;
    }

    public boolean isSilent() {
        return silent;
    }

    public boolean isGlowing() {
        return glowing;
    }
}
