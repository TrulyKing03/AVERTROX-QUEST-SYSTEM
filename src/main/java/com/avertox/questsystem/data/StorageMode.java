package com.avertox.questsystem.data;

public enum StorageMode {
    YAML,
    MYSQL;

    public static StorageMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return YAML;
        }
        if (raw.equalsIgnoreCase("mysql")) {
            return MYSQL;
        }
        return YAML;
    }
}
