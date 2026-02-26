package com.avertox.questsystem.api;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.event.EventRegistry;
import com.avertox.questsystem.integration.QuestEligibilityProvider;
import com.avertox.questsystem.model.GlobalEvent;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestManager;
import com.avertox.questsystem.quest.QuestRegistry;
import com.avertox.questsystem.quest.QuestTaskFactory;
import com.avertox.questsystem.quest.QuestTaskRegistry;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public class QuestSystemApiImpl implements QuestSystemApi {
    private final QuestRegistry questRegistry;
    private final EventRegistry eventRegistry;
    private final EventManager eventManager;
    private final QuestTaskRegistry questTaskRegistry;
    private final QuestManager questManager;
    private final QuestStoryEventBus storyEventBus;

    public QuestSystemApiImpl(
            QuestRegistry questRegistry,
            EventRegistry eventRegistry,
            EventManager eventManager,
            QuestTaskRegistry questTaskRegistry,
            QuestManager questManager,
            QuestStoryEventBus storyEventBus
    ) {
        this.questRegistry = questRegistry;
        this.eventRegistry = eventRegistry;
        this.eventManager = eventManager;
        this.questTaskRegistry = questTaskRegistry;
        this.questManager = questManager;
        this.storyEventBus = storyEventBus;
    }

    @Override
    public void registerQuest(Quest quest) {
        questRegistry.registerQuest(quest);
    }

    @Override
    public void unregisterQuest(String questId) {
        questRegistry.unregisterQuest(questId);
    }

    @Override
    public void registerEvent(GlobalEvent event) {
        eventRegistry.register(event);
    }

    @Override
    public boolean triggerEvent(String eventId) {
        return eventManager.startEvent(eventId, true);
    }

    @Override
    public void registerQuestTaskType(String taskTypeKey, QuestTaskFactory factory) {
        questTaskRegistry.registerTaskType(taskTypeKey, factory);
    }

    @Override
    public void registerEligibilityProvider(String providerId, QuestEligibilityProvider provider) {
        questManager.registerEligibilityProvider(providerId, provider);
    }

    @Override
    public void unregisterEligibilityProvider(String providerId) {
        questManager.unregisterEligibilityProvider(providerId);
    }

    @Override
    public void registerQuestEvent(String eventKey, Consumer<QuestStoryContext> handler) {
        storyEventBus.register(eventKey, handler);
    }

    @Override
    public void triggerQuestEvent(String eventKey, Player player, Quest quest, Map<String, Object> data) {
        storyEventBus.fire(eventKey, new QuestStoryContext(player, quest, data));
    }

    @Override
    public boolean acceptQuest(Player player, String questId) {
        return questManager.acceptQuest(player, questId);
    }

    @Override
    public double checkProgress(Player player, String questId) {
        return questManager.checkProgress(player, questId);
    }

    @Override
    public void recordExternalProgress(Player player, String sourceKey, int amount) {
        questManager.onExternalProgress(player, sourceKey, amount);
    }

    @Override
    public String getCurrentOrUpcomingEventDisplay() {
        return eventManager.getCurrentOrUpcomingDisplay();
    }
}
