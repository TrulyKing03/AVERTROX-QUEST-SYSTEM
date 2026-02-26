package com.avertox.questsystem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerQuestProfile {
    private final UUID uuid;
    private final Map<String, PlayerQuestState> questStates;
    private final List<String> history;
    private volatile long lastDailyReset;
    private volatile long lastWeeklyReset;
    private volatile long lastMonthlyReset;

    public PlayerQuestProfile(UUID uuid) {
        this.uuid = uuid;
        this.questStates = new ConcurrentHashMap<>();
        this.history = Collections.synchronizedList(new ArrayList<>());
    }

    public UUID uuid() {
        return uuid;
    }

    public Map<String, PlayerQuestState> questStates() {
        return questStates;
    }

    public List<String> history() {
        return history;
    }

    public void addHistory(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        history.add(0, entry);
        if (history.size() > 60) {
            history.remove(history.size() - 1);
        }
    }

    public long lastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public long lastWeeklyReset() {
        return lastWeeklyReset;
    }

    public void setLastWeeklyReset(long lastWeeklyReset) {
        this.lastWeeklyReset = lastWeeklyReset;
    }

    public long lastMonthlyReset() {
        return lastMonthlyReset;
    }

    public void setLastMonthlyReset(long lastMonthlyReset) {
        this.lastMonthlyReset = lastMonthlyReset;
    }
}
