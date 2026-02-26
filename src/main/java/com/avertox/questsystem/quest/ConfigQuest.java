package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.QuestReward;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.model.QuestType;

public class ConfigQuest extends Quest {
    public ConfigQuest(
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
        super(id, type, title, description, taskType, targetValue, rewards, repeatable, permission, task);
    }
}
