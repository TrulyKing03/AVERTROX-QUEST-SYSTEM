package com.avertox.questsystem.model;

import org.bukkit.potion.PotionEffectType;

public class EventEffect {
    private final EventEffectType type;
    private final double value;
    private final PotionEffectType potionEffectType;
    private final int amplifier;

    public EventEffect(EventEffectType type, double value, PotionEffectType potionEffectType, int amplifier) {
        this.type = type;
        this.value = value;
        this.potionEffectType = potionEffectType;
        this.amplifier = Math.max(0, amplifier);
    }

    public EventEffectType type() {
        return type;
    }

    public double value() {
        return value;
    }

    public PotionEffectType potionEffectType() {
        return potionEffectType;
    }

    public int amplifier() {
        return amplifier;
    }
}
