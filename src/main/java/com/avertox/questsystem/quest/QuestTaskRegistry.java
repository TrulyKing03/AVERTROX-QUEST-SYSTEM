package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.task.CollectBlocksTask;
import com.avertox.questsystem.quest.task.CraftItemsTask;
import com.avertox.questsystem.quest.task.KillMobsTask;
import com.avertox.questsystem.quest.task.MineOresTask;
import com.avertox.questsystem.quest.task.VisitCoordinatesTask;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestTaskRegistry {
    private final Map<String, QuestTaskFactory> factories = new ConcurrentHashMap<>();

    public QuestTaskRegistry() {
        registerBuiltIns();
    }

    public void registerTaskType(String key, QuestTaskFactory factory) {
        if (key == null || key.isBlank() || factory == null) {
            return;
        }
        factories.put(key.trim().toUpperCase(Locale.ROOT), factory);
    }

    public QuestTask createTask(Map<String, Object> taskSection) {
        if (taskSection == null || taskSection.isEmpty()) {
            return null;
        }
        String taskType = String.valueOf(taskSection.getOrDefault("task_type", "CUSTOM"));
        QuestTaskFactory factory = factories.get(taskType.toUpperCase(Locale.ROOT));
        return factory == null ? null : factory.create(taskSection);
    }

    public boolean hasTaskType(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return factories.containsKey(key.toUpperCase(Locale.ROOT));
    }

    private void registerBuiltIns() {
        registerTaskType(QuestTaskType.COLLECT_BLOCKS.name(), section -> {
            Material material = parseMaterial(section.get("material"));
            return new CollectBlocksTask(material);
        });
        registerTaskType(QuestTaskType.KILL_MOBS.name(), section -> {
            EntityType type = parseEntity(section.get("entity"));
            return new KillMobsTask(type);
        });
        registerTaskType(QuestTaskType.MINE_ORES.name(), section -> {
            Material material = parseMaterial(section.get("material"));
            return new MineOresTask(material);
        });
        registerTaskType(QuestTaskType.CRAFT_ITEMS.name(), section -> {
            Material material = parseMaterial(section.get("material"));
            return new CraftItemsTask(material);
        });
        registerTaskType(QuestTaskType.VISIT_COORDINATES.name(), section -> {
            String world = String.valueOf(section.getOrDefault("world", "world"));
            double x = parseDouble(section.get("x"), 0D);
            double y = parseDouble(section.get("y"), 64D);
            double z = parseDouble(section.get("z"), 0D);
            double radius = Math.max(1D, parseDouble(section.get("radius"), 5D));
            return new VisitCoordinatesTask(world, x, y, z, radius);
        });
    }

    private static Material parseMaterial(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Material.valueOf(String.valueOf(raw).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static EntityType parseEntity(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EntityType.valueOf(String.valueOf(raw).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static double parseDouble(Object raw, double fallback) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
