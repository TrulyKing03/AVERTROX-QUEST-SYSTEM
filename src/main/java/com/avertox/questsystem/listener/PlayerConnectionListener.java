package com.avertox.questsystem.listener;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.quest.QuestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final QuestManager questManager;
    private final EventManager eventManager;

    public PlayerConnectionListener(QuestManager questManager, EventManager eventManager) {
        this.questManager = questManager;
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        questManager.loadPlayer(event.getPlayer());
        eventManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        questManager.unloadPlayer(event.getPlayer());
    }
}
