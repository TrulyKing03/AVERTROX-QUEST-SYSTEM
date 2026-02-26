package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.PlayerQuestProfile;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressTracker {
    private final Map<UUID, PlayerQuestProfile> profiles = new ConcurrentHashMap<>();

    public PlayerQuestProfile getOrCreate(UUID uuid) {
        return profiles.computeIfAbsent(uuid, PlayerQuestProfile::new);
    }

    public void set(PlayerQuestProfile profile) {
        if (profile == null) {
            return;
        }
        profiles.put(profile.uuid(), profile);
    }

    public PlayerQuestProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    public PlayerQuestProfile remove(UUID uuid) {
        return profiles.remove(uuid);
    }

    public Collection<PlayerQuestProfile> all() {
        return profiles.values();
    }
}
