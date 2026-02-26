package com.avertox.questsystem.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventRuntimeState {
    private final Map<String, Long> lastTriggerTimes = new ConcurrentHashMap<>();
    private volatile String activeEventId;
    private volatile long activeUntil;
    private volatile long lastGlobalTrigger;

    public Map<String, Long> lastTriggerTimes() {
        return lastTriggerTimes;
    }

    public String activeEventId() {
        return activeEventId;
    }

    public void setActiveEventId(String activeEventId) {
        this.activeEventId = activeEventId;
    }

    public long activeUntil() {
        return activeUntil;
    }

    public void setActiveUntil(long activeUntil) {
        this.activeUntil = activeUntil;
    }

    public long lastGlobalTrigger() {
        return lastGlobalTrigger;
    }

    public void setLastGlobalTrigger(long lastGlobalTrigger) {
        this.lastGlobalTrigger = lastGlobalTrigger;
    }
}
