package com.avertox.questsystem.integration;

import com.avertox.questsystem.quest.Quest;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface QuestEligibilityProvider {
    boolean canAccept(Player player, Quest quest);
}
