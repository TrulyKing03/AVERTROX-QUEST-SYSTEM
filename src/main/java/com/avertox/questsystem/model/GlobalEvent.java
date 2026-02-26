package com.avertox.questsystem.model;

import java.util.Collections;
import java.util.List;

public class GlobalEvent {
    private final String id;
    private final String name;
    private final String description;
    private final int durationMinutes;
    private final int scheduleMinutes;
    private final List<EventEffect> effects;
    private boolean enabled;
    private boolean active;

    public GlobalEvent(
            String id,
            String name,
            String description,
            int durationMinutes,
            int scheduleMinutes,
            boolean enabled,
            List<EventEffect> effects
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationMinutes = Math.max(1, durationMinutes);
        this.scheduleMinutes = Math.max(0, scheduleMinutes);
        this.enabled = enabled;
        this.effects = effects == null ? Collections.emptyList() : Collections.unmodifiableList(effects);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public int scheduleMinutes() {
        return scheduleMinutes;
    }

    public List<EventEffect> effects() {
        return effects;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
