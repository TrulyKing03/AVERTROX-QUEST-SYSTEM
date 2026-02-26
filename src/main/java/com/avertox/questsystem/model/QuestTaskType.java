package com.avertox.questsystem.model;

import java.util.Locale;

public enum QuestTaskType {
    COLLECT_BLOCKS,
    KILL_MOBS,
    MINE_ORES,
    CRAFT_ITEMS,
    VISIT_COORDINATES,
    CUSTOM;

    public static QuestTaskType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CUSTOM;
        }
        try {
            return QuestTaskType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CUSTOM;
        }
    }
}
