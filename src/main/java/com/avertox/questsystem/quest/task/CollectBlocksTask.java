package com.avertox.questsystem.quest.task;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.QuestTask;
import org.bukkit.Material;

public class CollectBlocksTask implements QuestTask {
    private final Material material;

    public CollectBlocksTask(Material material) {
        this.material = material;
    }

    @Override
    public QuestTaskType type() {
        return QuestTaskType.COLLECT_BLOCKS;
    }

    @Override
    public boolean matches(QuestAction action) {
        if (action.type() != QuestActionType.ITEM_COLLECT) {
            return false;
        }
        return material == null || material == action.material();
    }

    @Override
    public String describeTarget() {
        return material == null ? "Collect block items" : "Collect " + material.name();
    }
}
