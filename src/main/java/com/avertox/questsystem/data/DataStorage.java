package com.avertox.questsystem.data;

import com.avertox.questsystem.model.EventRuntimeState;
import com.avertox.questsystem.model.PlayerQuestProfile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DataStorage {
    CompletableFuture<Void> initialize();

    CompletableFuture<PlayerQuestProfile> loadPlayerProfile(UUID uuid);

    CompletableFuture<Void> savePlayerProfile(PlayerQuestProfile profile);

    CompletableFuture<EventRuntimeState> loadEventRuntime();

    CompletableFuture<Void> saveEventRuntime(EventRuntimeState state);

    CompletableFuture<Map<String, Map<String, Object>>> loadQuestDefinitions();

    CompletableFuture<Void> saveQuestDefinitions(Map<String, Map<String, Object>> definitions);

    CompletableFuture<Map<String, Map<String, Object>>> loadEventDefinitions();

    CompletableFuture<Void> saveEventDefinitions(Map<String, Map<String, Object>> definitions);

    CompletableFuture<Void> close();
}
