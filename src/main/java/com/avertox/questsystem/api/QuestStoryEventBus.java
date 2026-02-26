package com.avertox.questsystem.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class QuestStoryEventBus {
    private final Map<String, List<Consumer<QuestStoryContext>>> handlers = new ConcurrentHashMap<>();

    public void register(String eventKey, Consumer<QuestStoryContext> consumer) {
        if (eventKey == null || eventKey.isBlank() || consumer == null) {
            return;
        }
        handlers.computeIfAbsent(eventKey.toLowerCase(), ignored -> new ArrayList<>()).add(consumer);
    }

    public void fire(String eventKey, QuestStoryContext context) {
        if (eventKey == null || eventKey.isBlank()) {
            return;
        }
        List<Consumer<QuestStoryContext>> consumers = handlers.get(eventKey.toLowerCase());
        if (consumers == null || consumers.isEmpty()) {
            return;
        }
        for (Consumer<QuestStoryContext> consumer : consumers) {
            try {
                consumer.accept(context);
            } catch (Exception ignored) {
            }
        }
    }
}
