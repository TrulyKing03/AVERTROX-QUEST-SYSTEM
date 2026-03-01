package com.avertox.questsystem.listener;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class QuestProgressListener implements Listener {
    private static final long MINING_HIT_TIMEOUT_MS = 1800L;
    private static final int MAX_MINING_TARGET_DISTANCE = 6;
    private static final int BASE_BREAK_HITS = 5;

    private final QuestManager questManager;
    private final EventManager eventManager;
    private final Map<UUID, MiningHitState> miningHitStates = new ConcurrentHashMap<>();

    public QuestProgressListener(QuestManager questManager, EventManager eventManager) {
        this.questManager = questManager;
        this.eventManager = eventManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        double multiplier = eventManager.miningSpeedMultiplier();
        if (multiplier <= 1D || event.getInstaBreak()) {
            miningHitStates.remove(event.getPlayer().getUniqueId());
            return;
        }

        Material blockType = event.getBlock().getType();
        if (!isSupportedMiningBlock(blockType) || !isMatchingTool(event.getPlayer(), blockType)) {
            miningHitStates.remove(event.getPlayer().getUniqueId());
            return;
        }

        // Deterministic server-side mining assist so actual break speed matches event multiplier.
        UUID uuid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        String key = blockKey(event.getBlock());

        MiningHitState previous = miningHitStates.get(uuid);
        int hits = 1;
        if (previous != null && previous.blockKey().equals(key) && (now - previous.lastHitMs()) <= MINING_HIT_TIMEOUT_MS) {
            hits = previous.hits() + 1;
        }

        int requiredHits = requiredHitsFor(multiplier);
        if (hits >= requiredHits) {
            event.setInstaBreak(true);
            miningHitStates.remove(uuid);
            return;
        }
        miningHitStates.put(uuid, new MiningHitState(key, hits, now));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        double multiplier = eventManager.miningSpeedMultiplier();
        UUID uuid = event.getPlayer().getUniqueId();
        if (multiplier <= 1D) {
            miningHitStates.remove(uuid);
            return;
        }

        MiningHitState state = miningHitStates.get(uuid);
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - state.lastHitMs()) > MINING_HIT_TIMEOUT_MS) {
            miningHitStates.remove(uuid);
            return;
        }

        Player player = event.getPlayer();
        Block target = player.getTargetBlockExact(MAX_MINING_TARGET_DISTANCE);
        if (target == null || target.getType() == Material.AIR) {
            miningHitStates.remove(uuid);
            return;
        }

        Material blockType = target.getType();
        if (!state.blockKey().equals(blockKey(target))
                || !isSupportedMiningBlock(blockType)
                || !isMatchingTool(player, blockType)) {
            miningHitStates.remove(uuid);
            return;
        }

        int hits = state.hits() + 1;
        int requiredHits = requiredHitsFor(multiplier);
        if (hits >= requiredHits) {
            miningHitStates.remove(uuid);
            player.breakBlock(target);
            return;
        }

        miningHitStates.put(uuid, new MiningHitState(state.blockKey(), hits, now));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        miningHitStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        miningHitStates.remove(event.getPlayer().getUniqueId());

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

    private boolean isSupportedMiningBlock(Material blockType) {
        return Tag.MINEABLE_PICKAXE.isTagged(blockType)
                || Tag.MINEABLE_SHOVEL.isTagged(blockType)
                || Tag.MINEABLE_AXE.isTagged(blockType);
    }

    private boolean isMatchingTool(Player player, Material blockType) {
        Material held = player.getInventory().getItemInMainHand().getType();
        if (held == Material.AIR) {
            return false;
        }
        String heldName = held.name();
        if (Tag.MINEABLE_PICKAXE.isTagged(blockType)) {
            return heldName.endsWith("_PICKAXE");
        }
        if (Tag.MINEABLE_SHOVEL.isTagged(blockType)) {
            return heldName.endsWith("_SHOVEL");
        }
        if (Tag.MINEABLE_AXE.isTagged(blockType)) {
            return heldName.endsWith("_AXE");
        }
        return true;
    }

    private int requiredHitsFor(double multiplier) {
        if (multiplier <= 1D) {
            return BASE_BREAK_HITS;
        }
        return Math.max(1, (int) Math.ceil(BASE_BREAK_HITS / multiplier));
    }

    private String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private record MiningHitState(String blockKey, int hits, long lastHitMs) {
    }
}
