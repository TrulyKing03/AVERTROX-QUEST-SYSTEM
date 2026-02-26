package com.avertox.questsystem.quest;

import java.util.Map;

public interface QuestTaskFactory {
    QuestTask create(Map<String, Object> section);
}
