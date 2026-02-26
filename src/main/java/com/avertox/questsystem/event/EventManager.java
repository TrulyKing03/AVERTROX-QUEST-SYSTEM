package com.avertox.questsystem.event;

import com.avertox.questsystem.api.GlobalEventEndEvent;
import com.avertox.questsystem.api.GlobalEventStartEvent;
import com.avertox.questsystem.config.MainConfig;
import com.avertox.questsystem.data.StorageManager;
import com.avertox.questsystem.model.EventEffect;
import com.avertox.questsystem.model.EventEffectType;
import com.avertox.questsystem.model.EventRuntimeState;
import com.avertox.questsystem.model.GlobalEvent;
import com.avertox.questsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class EventManager {
    private final JavaPlugin plugin;
    private final MainConfig config;
    private final StorageManager storageManager;
    private final EventRegistry registry;

    private final EventRuntimeState runtimeState = new EventRuntimeState();
    private final Map<UUID, Float> originalWalkSpeeds = new ConcurrentHashMap<>();
    private final Set<PotionEffectType> appliedPotionTypes = ConcurrentHashMap.newKeySet();

    private BossBar bossBar;
    private BukkitTask tickerTask;
    private GlobalEvent activeEvent;

    private volatile double moneyBoostMultiplier = 1D;
    private volatile double xpBoostMultiplier = 1D;
    private volatile double dropRateMultiplier;
    private volatile double miningSpeedMultiplier;

    public EventManager(JavaPlugin plugin, MainConfig config, StorageManager storageManager, EventRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.storageManager = storageManager;
        this.registry = registry;
        this.dropRateMultiplier = config.baseDropRateMultiplier();
        this.miningSpeedMultiplier = config.baseMiningMultiplier();
    }

    public void hydrateRuntime(EventRuntimeState state) {
        if (state == null) {
            return;
        }
        runtimeState.lastTriggerTimes().clear();
        runtimeState.lastTriggerTimes().putAll(state.lastTriggerTimes());
        runtimeState.setLastGlobalTrigger(state.lastGlobalTrigger());
        runtimeState.setActiveEventId(state.activeEventId());
        runtimeState.setActiveUntil(state.activeUntil());

        if (state.activeEventId() != null && !state.activeEventId().isBlank()) {
            GlobalEvent event = registry.get(state.activeEventId());
            if (event != null && state.activeUntil() > System.currentTimeMillis()) {
                startEvent(event.id(), false, true);
            } else {
                runtimeState.setActiveEventId("");
                runtimeState.setActiveUntil(0L);
                saveRuntime();
            }
        }
    }

    public void registerEvent(GlobalEvent event) {
        registry.register(event);
    }

    public void clearRegistry() {
        registry.clear();
    }

    public boolean startEvent(String eventId, boolean broadcast, boolean restoring) {
        GlobalEvent event = registry.get(eventId);
        if (event == null || !event.enabled()) {
            return false;
        }
        if (activeEvent != null) {
            stopActiveEvent(false);
        }

        long now = System.currentTimeMillis();
        long end = now + TimeUnit.MINUTES.toMillis(event.durationMinutes());
        activeEvent = event;
        event.setActive(true);

        runtimeState.setActiveEventId(event.id());
        runtimeState.setActiveUntil(end);
        runtimeState.setLastGlobalTrigger(now);
        runtimeState.lastTriggerTimes().put(event.id().toLowerCase(), now);

        applyEffects(event);
        if (config.bossBarEnabled()) {
            createBossBar(event);
        }
        startTicker();

        if (broadcast) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "⚡ Event Started: " + ChatColor.GOLD + event.name()
                    + ChatColor.GRAY + " - " + event.description());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(ChatColor.GOLD + "⚡ " + event.name(), ChatColor.YELLOW + event.description(), 10, 60, 10);
            }
        }
        if (!restoring) {
            Bukkit.getPluginManager().callEvent(new GlobalEventStartEvent(event));
        }
        saveRuntime();
        return true;
    }

    public boolean startEvent(String eventId, boolean broadcast) {
        return startEvent(eventId, broadcast, false);
    }

    public boolean triggerRandomEvent(boolean broadcast) {
        List<GlobalEvent> enabled = registry.enabled();
        if (enabled.isEmpty()) {
            return false;
        }
        GlobalEvent target = enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()));
        return startEvent(target.id(), broadcast, false);
    }

    public void stopActiveEvent(boolean broadcast) {
        if (activeEvent == null) {
            return;
        }
        GlobalEvent closing = activeEvent;

        removeEffects(closing);
        closing.setActive(false);
        activeEvent = null;

        runtimeState.setActiveEventId("");
        runtimeState.setActiveUntil(0L);
        saveRuntime();

        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }

        if (broadcast) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "⏰ Event Ended: " + ChatColor.GOLD + closing.name());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(ChatColor.GRAY + "⏰ Event Ended", ChatColor.GOLD + closing.name(), 10, 50, 10);
            }
        }

        Bukkit.getPluginManager().callEvent(new GlobalEventEndEvent(closing));
    }

    public void schedulerTick() {
        long now = System.currentTimeMillis();
        if (activeEvent != null) {
            if (now >= runtimeState.activeUntil()) {
                stopActiveEvent(true);
            }
            return;
        }

        if (!config.eventsSchedulerEnabled()) {
            return;
        }

        long intervalMs = TimeUnit.MINUTES.toMillis(config.eventIntervalMinutes());
        long last = runtimeState.lastGlobalTrigger();
        if (last <= 0L || (now - last) >= intervalMs) {
            triggerRandomEvent(true);
        }
    }

    public void shutdown() {
        stopActiveEvent(false);
    }

    public void handleJoin(Player player) {
        if (activeEvent == null) {
            return;
        }
        applyEffectsToPlayer(player, activeEvent);
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public GlobalEvent getActiveEvent() {
        return activeEvent;
    }

    public EventRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public long getNextTriggerEpochMs() {
        long intervalMs = TimeUnit.MINUTES.toMillis(config.eventIntervalMinutes());
        long last = runtimeState.lastGlobalTrigger();
        if (last <= 0L) {
            return System.currentTimeMillis();
        }
        return last + intervalMs;
    }

    public String getCurrentOrUpcomingDisplay() {
        if (activeEvent != null) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(TimeUtil.millisUntil(runtimeState.activeUntil()));
            return config.placeholderActive()
                    .replace("%event%", activeEvent.name())
                    .replace("%minutes%", String.valueOf(Math.max(0L, minutes)));
        }

        long next = getNextTriggerEpochMs();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(TimeUtil.millisUntil(next));

        List<GlobalEvent> enabled = registry.enabled();
        if (enabled.isEmpty()) {
            return config.placeholderNoneActive();
        }

        String upcomingName = enabled.get(0).name();
        return config.placeholderUpcoming()
                .replace("%event%", upcomingName)
                .replace("%minutes%", String.valueOf(Math.max(0L, minutes)));
    }

    public double moneyBoostMultiplier() {
        return moneyBoostMultiplier;
    }

    public double xpBoostMultiplier() {
        return xpBoostMultiplier;
    }

    public double dropRateMultiplier() {
        return dropRateMultiplier;
    }

    public double miningSpeedMultiplier() {
        return miningSpeedMultiplier;
    }

    public List<GlobalEvent> getRegisteredEvents() {
        List<GlobalEvent> list = new ArrayList<>(registry.all());
        list.sort(Comparator.comparing(GlobalEvent::name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public GlobalEvent getEvent(String eventId) {
        return registry.get(eventId);
    }

    public long getLastTriggerTime(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return 0L;
        }
        return runtimeState.lastTriggerTimes().getOrDefault(eventId.toLowerCase(), 0L);
    }

    public long getActiveRemainingMillis() {
        if (activeEvent == null) {
            return 0L;
        }
        return TimeUtil.millisUntil(runtimeState.activeUntil());
    }

    private void createBossBar(GlobalEvent event) {
        bossBar = Bukkit.createBossBar(
                ChatColor.GOLD + "⚡ " + event.name(),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvent == null) {
                return;
            }
            long now = System.currentTimeMillis();
            long total = TimeUnit.MINUTES.toMillis(activeEvent.durationMinutes());
            long remaining = Math.max(0L, runtimeState.activeUntil() - now);
            if (bossBar != null) {
                bossBar.setTitle(ChatColor.GOLD + "⚡ " + activeEvent.name() + ChatColor.GRAY + " - " + TimeUtil.shortDuration(remaining));
                bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, remaining / (double) total)));
            }
            if (remaining <= 0L) {
                stopActiveEvent(true);
            }
        }, 20L, 20L);
    }

    private void applyEffects(GlobalEvent event) {
        moneyBoostMultiplier = 1D;
        xpBoostMultiplier = 1D;
        dropRateMultiplier = config.baseDropRateMultiplier();
        miningSpeedMultiplier = config.baseMiningMultiplier();
        appliedPotionTypes.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyEffectsToPlayer(player, event);
        }

        for (EventEffect effect : event.effects()) {
            switch (effect.type()) {
                case MONEY_BOOST -> moneyBoostMultiplier = Math.max(0D, effect.value());
                case XP_BOOST -> xpBoostMultiplier = Math.max(0D, effect.value());
                case DROP_RATE_MULTIPLIER -> dropRateMultiplier = Math.max(0D, effect.value());
                case MINING_SPEED_MODIFIER -> miningSpeedMultiplier = Math.max(0D, effect.value());
                default -> {
                }
            }
        }
    }

    private void applyEffectsToPlayer(Player player, GlobalEvent event) {
        for (EventEffect effect : event.effects()) {
            EventEffectType type = effect.type();
            if (type == EventEffectType.WALK_SPEED_MULTIPLIER) {
                originalWalkSpeeds.putIfAbsent(player.getUniqueId(), player.getWalkSpeed());
                float base = originalWalkSpeeds.getOrDefault(player.getUniqueId(), 0.2F);
                float adjusted = (float) Math.min(1.0F, Math.max(0.05F, base * effect.value()));
                player.setWalkSpeed(adjusted);
                continue;
            }

            if (type == EventEffectType.MINING_SPEED_MODIFIER) {
                int amplifier = (int) Math.max(0D, Math.round((effect.value() - 1D) * 4D));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, amplifier, true, false, true));
                appliedPotionTypes.add(PotionEffectType.FAST_DIGGING);
                continue;
            }

            if (type == EventEffectType.POTION_EFFECT && effect.potionEffectType() != null) {
                player.addPotionEffect(new PotionEffect(effect.potionEffectType(), Integer.MAX_VALUE, effect.amplifier(), true, false, true));
                appliedPotionTypes.add(effect.potionEffectType());
            }
        }
    }

    private void removeEffects(GlobalEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Float original = originalWalkSpeeds.remove(player.getUniqueId());
            if (original != null) {
                player.setWalkSpeed(original);
            }
            for (PotionEffectType potionType : appliedPotionTypes) {
                player.removePotionEffect(potionType);
            }
        }

        moneyBoostMultiplier = 1D;
        xpBoostMultiplier = 1D;
        dropRateMultiplier = config.baseDropRateMultiplier();
        miningSpeedMultiplier = config.baseMiningMultiplier();
        appliedPotionTypes.clear();
    }

    private void saveRuntime() {
        storageManager.saveEventRuntime(runtimeState);
    }
}
