package com.avertox.questsystem.quest;

import com.avertox.questsystem.model.QuestType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestRegistry {
    private final Map<String, Quest> quests = new ConcurrentHashMap<>();

    public void registerQuest(Quest quest) {
        if (quest == null) {
            return;
        }
        quests.put(quest.id().toLowerCase(), quest);
    }

    public void unregisterQuest(String questId) {
        if (questId == null) {
            return;
        }
        quests.remove(questId.toLowerCase());
    }

    public Quest getQuest(String questId) {
        if (questId == null) {
            return null;
        }
        return quests.get(questId.toLowerCase());
    }

    public Collection<Quest> getAll() {
        return Collections.unmodifiableCollection(quests.values());
    }

    public List<Quest> getByType(QuestType type) {
        List<Quest> list = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.type() == type) {
                list.add(quest);
            }
        }
        return list;
    }

    public void clear() {
        quests.clear();
    }
}
