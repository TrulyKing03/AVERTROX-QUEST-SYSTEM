package com.avertox.questsystem.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {
    }

    public static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }
}
