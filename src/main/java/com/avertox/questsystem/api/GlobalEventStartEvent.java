package com.avertox.questsystem.api;

import com.avertox.questsystem.model.GlobalEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GlobalEventStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GlobalEvent event;

    public GlobalEventStartEvent(GlobalEvent event) {
        this.event = event;
    }

    public GlobalEvent getEventData() {
        return event;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
