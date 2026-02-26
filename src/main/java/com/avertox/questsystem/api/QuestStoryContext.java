package com.avertox.questsystem.api;

import com.avertox.questsystem.quest.Quest;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuestStoryContext {
    private final Player player;
    private final Quest quest;
    private final Map<String, Object> data;

    public QuestStoryContext(Player player, Quest quest, Map<String, Object> data) {
        this.player = player;
        this.quest = quest;
        this.data = data == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(data));
    }

    public Player player() {
        return player;
    }

    public Quest quest() {
        return quest;
    }

    public Map<String, Object> data() {
        return data;
    }
}
