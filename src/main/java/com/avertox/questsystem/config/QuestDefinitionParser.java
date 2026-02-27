package com.avertox.questsystem.config;

import com.avertox.questsystem.model.QuestReward;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.model.QuestType;
import com.avertox.questsystem.quest.ConfigQuest;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestTask;
import com.avertox.questsystem.quest.QuestTaskRegistry;
import com.avertox.questsystem.util.ItemCodec;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class QuestDefinitionParser {
    private final QuestTaskRegistry taskRegistry;
    private final Logger logger;
    private final MainConfig mainConfig;

    public QuestDefinitionParser(QuestTaskRegistry taskRegistry, MainConfig mainConfig, Logger logger) {
        this.taskRegistry = taskRegistry;
        this.mainConfig = mainConfig;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    public Quest parse(Map<String, Object> section) {
        if (section == null || section.isEmpty()) {
            return null;
        }

        String id = asString(section.get("id"));
        String typeRaw = asString(section.get("type"));
        String title = asString(section.get("title"));
        String description = asString(section.get("description"));

        if (id.isBlank() || typeRaw.isBlank() || title.isBlank()) {
            logger.warning("Quest definition missing required fields: id/type/title");
            return null;
        }

        QuestType questType;
        try {
            questType = QuestType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("Quest " + id + " has invalid type: " + typeRaw);
            return null;
        }

        Object taskObj = section.get("task");
        if (!(taskObj instanceof Map<?, ?> taskMapRaw)) {
            logger.warning("Quest " + id + " is missing task section.");
            return null;
        }

        Map<String, Object> taskSection = (Map<String, Object>) taskMapRaw;
        String taskTypeRaw = asString(taskSection.get("task_type"));
        if (taskTypeRaw.isBlank() || !taskRegistry.hasTaskType(taskTypeRaw)) {
            logger.warning("Quest " + id + " uses unknown task type: " + taskTypeRaw);
            return null;
        }

        QuestTaskType taskType = QuestTaskType.from(taskTypeRaw);
        int target = (int) asDouble(taskSection.getOrDefault("target", section.getOrDefault("target", 1)), 1);
        if (target <= 0) {
            logger.warning("Quest " + id + " has non-positive target, forcing to 1.");
            target = 1;
        }

        if (taskType == QuestTaskType.VISIT_COORDINATES) {
            sanitizeVisitCoordinates(questType, id, taskSection);
        }

        QuestTask task = taskRegistry.createTask(taskSection);
        if (task == null) {
            logger.warning("Quest " + id + " failed task initialization.");
            return null;
        }

        QuestReward reward = parseReward(section.get("rewards"));
        boolean repeatable = asBoolean(section.get("repeatable"), true);
        String permission = asString(section.getOrDefault("permission", mainConfig.defaultQuestPermission()));

        return new ConfigQuest(
                id,
                questType,
                title,
                description,
                taskType,
                target,
                reward,
                repeatable,
                permission,
                task
        );
    }

    @SuppressWarnings("unchecked")
    private QuestReward parseReward(Object rawRewards) {
        if (!(rawRewards instanceof Map<?, ?> mapRaw)) {
            return QuestReward.empty();
        }
        Map<String, Object> rewards = (Map<String, Object>) mapRaw;
        double xp = asDouble(rewards.get("xp"), 0D);
        double money = asDouble(rewards.get("money"), 0D);

        List<String> itemsRaw = new ArrayList<>();
        Object itemsObj = rewards.get("items");
        if (itemsObj instanceof List<?> list) {
            for (Object object : list) {
                if (object != null) {
                    itemsRaw.add(String.valueOf(object));
                }
            }
        }
        return new QuestReward(xp, money, ItemCodec.parseItems(itemsRaw));
    }

    private String asString(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private double asDouble(Object raw, double fallback) {
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

    private boolean asBoolean(Object raw, boolean fallback) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private void sanitizeVisitCoordinates(QuestType questType, String questId, Map<String, Object> taskSection) {
        String requestedWorld = asString(taskSection.getOrDefault("world", "world"));
        World world = Bukkit.getWorld(requestedWorld);
        boolean adjusted = false;
        StringBuilder reasons = new StringBuilder();

        if (world == null) {
            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) {
                logger.warning("Quest " + questId + " has visit location in unknown world '" + requestedWorld
                        + "' and no worlds are loaded. Using raw coordinates.");
                return;
            }
            world = worlds.get(0);
            adjusted = true;
            reasons.append("world fallback");
        }

        double x = asDouble(taskSection.get("x"), 0D);
        double y = asDouble(taskSection.get("y"), 64D);
        double z = asDouble(taskSection.get("z"), 0D);

        WorldBorder border = world.getWorldBorder();
        double half = Math.max(16D, border.getSize() / 2D - 2D);
        double minX = border.getCenter().getX() - half;
        double maxX = border.getCenter().getX() + half;
        double minZ = border.getCenter().getZ() - half;
        double maxZ = border.getCenter().getZ() + half;

        double clampedX = clamp(x, minX, maxX);
        double clampedZ = clamp(z, minZ, maxZ);
        if (clampedX != x || clampedZ != z) {
            adjusted = true;
            appendReason(reasons, "outside world border");
        }
        x = clampedX;
        z = clampedZ;

        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        if (minY > maxY) {
            minY = world.getMinHeight();
            maxY = world.getMaxHeight() - 1;
        }

        boolean invalidY = y < minY || y > maxY;
        if (invalidY) {
            int surfaceY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
            y = clamp(surfaceY, minY, maxY);
            adjusted = true;
            appendReason(reasons, "invalid Y-level");
        } else {
            y = clamp(y, minY, maxY);
        }

        taskSection.put("world", world.getName());
        taskSection.put("x", x);
        taskSection.put("y", y);
        taskSection.put("z", z);

        if (adjusted) {
            logger.info("Quest " + questId + " (" + questType.name()
                    + ") visit location adjusted to valid coordinates in world "
                    + world.getName() + " at (" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z)
                    + ") due to: " + reasons);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void appendReason(StringBuilder sb, String reason) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(reason);
    }
}
