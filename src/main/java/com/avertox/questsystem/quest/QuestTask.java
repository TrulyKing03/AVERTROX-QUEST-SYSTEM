package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestTaskType;

public interface QuestTask {
    QuestTaskType type();

    boolean matches(QuestAction action);

    default int progressAmount(QuestAction action) {
        return Math.max(1, action.amount());
    }

    String describeTarget();
}
