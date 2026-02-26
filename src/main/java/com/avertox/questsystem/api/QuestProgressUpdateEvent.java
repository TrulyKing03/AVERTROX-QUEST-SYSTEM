package com.avertox.questsystem.api;

import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QuestProgressUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Quest quest;
    private final PlayerQuestState state;

    public QuestProgressUpdateEvent(Player player, Quest quest, PlayerQuestState state) {
        this.player = player;
        this.quest = quest;
        this.state = state;
    }

    public Player getPlayer() {
        return player;
    }

    public Quest getQuest() {
        return quest;
    }

    public PlayerQuestState getState() {
        return state;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
