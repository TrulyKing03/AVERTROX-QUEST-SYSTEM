package com.avertox.questsystem.event;

import com.avertox.questsystem.model.GlobalEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventRegistry {
    private final Map<String, GlobalEvent> events = new ConcurrentHashMap<>();

    public void register(GlobalEvent event) {
        if (event == null) {
            return;
        }
        events.put(event.id().toLowerCase(), event);
    }

    public void unregister(String eventId) {
        if (eventId == null) {
            return;
        }
        events.remove(eventId.toLowerCase());
    }

    public GlobalEvent get(String eventId) {
        if (eventId == null) {
            return null;
        }
        return events.get(eventId.toLowerCase());
    }

    public Collection<GlobalEvent> all() {
        return Collections.unmodifiableCollection(events.values());
    }

    public List<GlobalEvent> enabled() {
        List<GlobalEvent> list = new ArrayList<>();
        for (GlobalEvent event : events.values()) {
            if (event.enabled()) {
                list.add(event);
            }
        }
        return list;
    }

    public void clear() {
        events.clear();
    }
}
