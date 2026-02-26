package com.avertox.questsystem.model;

import java.util.Locale;

public enum EventEffectType {
    WALK_SPEED_MULTIPLIER,
    MINING_SPEED_MODIFIER,
    MONEY_BOOST,
    XP_BOOST,
    DROP_RATE_MULTIPLIER,
    POTION_EFFECT;

    public static EventEffectType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EventEffectType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
