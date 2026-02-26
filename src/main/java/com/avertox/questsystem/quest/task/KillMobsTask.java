package com.avertox.questsystem.quest.task;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.QuestTask;
import org.bukkit.entity.EntityType;

public class KillMobsTask implements QuestTask {
    private final EntityType entityType;

    public KillMobsTask(EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public QuestTaskType type() {
        return QuestTaskType.KILL_MOBS;
    }

    @Override
    public boolean matches(QuestAction action) {
        if (action.type() != QuestActionType.MOB_KILL) {
            return false;
        }
        return entityType == null || entityType == action.entityType();
    }

    @Override
    public String describeTarget() {
        return entityType == null ? "Kill mobs" : "Kill " + entityType.name();
    }
}
