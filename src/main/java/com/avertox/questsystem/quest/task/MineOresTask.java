package com.avertox.questsystem.quest.task;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.QuestTask;
import org.bukkit.Material;

public class MineOresTask implements QuestTask {
    private final Material material;

    public MineOresTask(Material material) {
        this.material = material;
    }

    @Override
    public QuestTaskType type() {
        return QuestTaskType.MINE_ORES;
    }

    @Override
    public boolean matches(QuestAction action) {
        if (action.type() != QuestActionType.BLOCK_BREAK) {
            return false;
        }
        Material target = action.material();
        if (target == null) {
            return false;
        }
        if (material != null) {
            return material == target;
        }
        return isOre(target);
    }

    @Override
    public String describeTarget() {
        return material == null ? "Mine ores" : "Mine " + material.name();
    }

    private boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS || material == Material.STONE;
    }
}
