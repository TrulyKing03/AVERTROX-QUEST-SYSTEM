package com.avertox.questsystem.quest.task;

import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestTaskType;
import com.avertox.questsystem.quest.QuestTask;
import org.bukkit.Location;

public class VisitCoordinatesTask implements QuestTask {
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double radiusSquared;

    public VisitCoordinatesTask(String world, double x, double y, double z, double radius) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radiusSquared = radius * radius;
    }

    @Override
    public QuestTaskType type() {
        return QuestTaskType.VISIT_COORDINATES;
    }

    @Override
    public boolean matches(QuestAction action) {
        if (action.type() != QuestActionType.PLAYER_MOVE) {
            return false;
        }
        Location location = action.location();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(world)) {
            return false;
        }
        double dx = location.getX() - x;
        double dy = location.getY() - y;
        double dz = location.getZ() - z;
        return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
    }

    @Override
    public int progressAmount(QuestAction action) {
        return 1;
    }

    @Override
    public String describeTarget() {
        return "Visit " + world + " (" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ")";
    }
}
