package com.avertox.questsystem.api;

import com.avertox.questsystem.model.GlobalEvent;
import com.avertox.questsystem.integration.QuestEligibilityProvider;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestTaskFactory;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public interface QuestSystemApi {
    void registerQuest(Quest quest);

    void unregisterQuest(String questId);

    void registerEvent(GlobalEvent event);

    boolean triggerEvent(String eventId);

    void registerQuestTaskType(String taskTypeKey, QuestTaskFactory factory);

    void registerEligibilityProvider(String providerId, QuestEligibilityProvider provider);

    void unregisterEligibilityProvider(String providerId);

    void registerQuestEvent(String eventKey, Consumer<QuestStoryContext> handler);

    void triggerQuestEvent(String eventKey, Player player, Quest quest, Map<String, Object> data);

    boolean acceptQuest(Player player, String questId);

    double checkProgress(Player player, String questId);

    void recordExternalProgress(Player player, String sourceKey, int amount);

    String getCurrentOrUpcomingEventDisplay();
}
