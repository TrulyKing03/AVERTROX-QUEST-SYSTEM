package com.avertox.questsystem.quest.task;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.QuestTask;
import org.bukkit.Material;

public class CraftItemsTask implements QuestTask {
    private final Material material;

    public CraftItemsTask(Material material) {
        this.material = material;
    }

    @Override
    public QuestTaskType type() {
        return QuestTaskType.CRAFT_ITEMS;
    }

    @Override
    public boolean matches(QuestAction action) {
        if (action.type() != QuestActionType.ITEM_CRAFT) {
            return false;
        }
        return material == null || material == action.material();
    }

    @Override
    public String describeTarget() {
        return material == null ? "Craft items" : "Craft " + material.name();
    }
}
