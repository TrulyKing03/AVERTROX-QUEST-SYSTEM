package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.QuestReward;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.model.QuestType;
import org.bukkit.entity.Player;

public abstract class Quest {
    private final String id;
    private final QuestType type;
    private final String title;
    private final String description;
    private final QuestTaskType taskType;
    private final int targetValue;
    private final QuestReward rewards;
    private final boolean repeatable;
    private final String permission;
    private final QuestTask task;

    protected Quest(
            String id,
            QuestType type,
            String title,
            String description,
            QuestTaskType taskType,
            int targetValue,
            QuestReward rewards,
            boolean repeatable,
            String permission,
            QuestTask task
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.targetValue = Math.max(1, targetValue);
        this.rewards = rewards;
        this.repeatable = repeatable;
        this.permission = permission == null ? "" : permission;
        this.task = task;
    }

    public String id() {
        return id;
    }

    public QuestType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public QuestTaskType taskType() {
        return taskType;
    }

    public int targetValue() {
        return targetValue;
    }

    public QuestReward rewards() {
        return rewards;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public String permission() {
        return permission;
    }

    public QuestTask task() {
        return task;
    }

    public boolean canAccept(Player player) {
        return permission.isBlank() || player.hasPermission(permission);
    }
}
