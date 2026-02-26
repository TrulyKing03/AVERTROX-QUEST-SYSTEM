package com.avertox.questsystem.quest;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface QuestUpdateNotifier {
    void onUpdate(Player player);
}
