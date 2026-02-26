package com.avertox.questsystem.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class QuestAction {
    private final QuestActionType type;
    private final Material material;
    private final EntityType entityType;
    private final Location location;
    private final int amount;
    private final String externalKey;

    private QuestAction(
            QuestActionType type,
            Material material,
            EntityType entityType,
            Location location,
            int amount,
            String externalKey
    ) {
        this.type = type;
        this.material = material;
        this.entityType = entityType;
        this.location = location;
        this.amount = Math.max(1, amount);
        this.externalKey = externalKey;
    }

    public static QuestAction blockBreak(Material material, int amount, Location location) {
        return new QuestAction(QuestActionType.BLOCK_BREAK, material, null, location, amount, null);
    }

    public static QuestAction collect(Material material, int amount, Location location) {
        return new QuestAction(QuestActionType.ITEM_COLLECT, material, null, location, amount, null);
    }

    public static QuestAction mobKill(EntityType entityType, Location location) {
        return new QuestAction(QuestActionType.MOB_KILL, null, entityType, location, 1, null);
    }

    public static QuestAction craft(Material material, int amount) {
        return new QuestAction(QuestActionType.ITEM_CRAFT, material, null, null, amount, null);
    }

    public static QuestAction move(Location location) {
        return new QuestAction(QuestActionType.PLAYER_MOVE, null, null, location, 1, null);
    }

    public static QuestAction external(String key, int amount) {
        return new QuestAction(QuestActionType.EXTERNAL, null, null, null, amount, key);
    }

    public QuestActionType type() {
        return type;
    }

    public Material material() {
        return material;
    }

    public EntityType entityType() {
        return entityType;
    }

    public Location location() {
        return location;
    }

    public int amount() {
        return amount;
    }

    public String externalKey() {
        return externalKey;
    }
}
