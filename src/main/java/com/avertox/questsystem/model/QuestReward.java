package com.avertox.questsystem.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestReward {
    private final double xp;
    private final double money;
    private final List<ItemStack> items;

    public QuestReward(double xp, double money, List<ItemStack> items) {
        this.xp = Math.max(0D, xp);
        this.money = Math.max(0D, money);
        if (items == null || items.isEmpty()) {
            this.items = Collections.emptyList();
        } else {
            List<ItemStack> copy = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                if (item != null) {
                    copy.add(item.clone());
                }
            }
            this.items = Collections.unmodifiableList(copy);
        }
    }

    public static QuestReward empty() {
        return new QuestReward(0D, 0D, Collections.emptyList());
    }

    public double xp() {
        return xp;
    }

    public double money() {
        return money;
    }

    public List<ItemStack> items() {
        return items;
    }
}
