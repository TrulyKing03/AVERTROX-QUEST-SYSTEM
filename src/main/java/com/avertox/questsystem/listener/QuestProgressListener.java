package com.avertox.questsystem.listener;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class QuestProgressListener implements Listener {
    private final QuestManager questManager;
    private final EventManager eventManager;

    public QuestProgressListener(QuestManager questManager, EventManager eventManager) {
        this.questManager = questManager;
        this.eventManager = eventManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        questManager.onQuestAction(player, QuestAction.blockBreak(type, 1, block.getLocation()));
        applyDropRateBoost(player, block);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        EntityType type = event.getEntityType();
        questManager.onQuestAction(killer, QuestAction.mobKill(type, event.getEntity().getLocation()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.AIR) {
            return;
        }
        int amount = Math.max(1, result.getAmount());
        questManager.onQuestAction(player, QuestAction.craft(result.getType(), amount));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCollect(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack.getType().isBlock()) {
            questManager.onQuestAction(player, QuestAction.collect(stack.getType(), stack.getAmount(), event.getItem().getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        questManager.onQuestAction(event.getPlayer(), QuestAction.move(event.getTo()));
    }

    private void applyDropRateBoost(Player player, Block block) {
        double multiplier = eventManager.dropRateMultiplier();
        if (multiplier <= 1D) {
            return;
        }

        Collection<ItemStack> baseDrops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        if (baseDrops.isEmpty()) {
            return;
        }

        double bonusChance = Math.min(1D, multiplier - 1D);
        for (ItemStack drop : baseDrops) {
            if (ThreadLocalRandom.current().nextDouble() <= bonusChance) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }
    }
}
