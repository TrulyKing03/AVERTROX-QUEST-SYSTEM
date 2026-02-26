package com.avertox.questsystem.quest;

import com.avertox.questsystem.api.QuestCompletedEvent;
import com.avertox.questsystem.api.QuestProgressUpdateEvent;
import com.avertox.questsystem.api.QuestStoryContext;
import com.avertox.questsystem.api.QuestStoryEventBus;
import com.avertox.questsystem.config.MainConfig;
import com.avertox.questsystem.data.StorageManager;
import com.avertox.questsystem.economy.EconomyService;
import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.integration.QuestEligibilityProvider;
import com.avertox.questsystem.model.PlayerQuestProfile;
import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.model.QuestAction;
import com.avertox.questsystem.model.QuestActionType;
import com.avertox.questsystem.model.QuestReward;
import com.avertox.questsystem.model.QuestType;
import com.avertox.questsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class QuestManager {
    private final JavaPlugin plugin;
    private final MainConfig config;
    private final StorageManager storage;
    private final EconomyService economy;
    private final EventManager eventManager;
    private final QuestRegistry questRegistry;
    private final QuestProgressTracker progressTracker;
    private final QuestStoryEventBus storyEventBus;
    private final Map<String, QuestEligibilityProvider> eligibilityProviders = new ConcurrentHashMap<>();

    private QuestUpdateNotifier updateNotifier;
    private BukkitTask resetTask;

    public QuestManager(
            JavaPlugin plugin,
            MainConfig config,
            StorageManager storage,
            EconomyService economy,
            EventManager eventManager,
            QuestRegistry questRegistry,
            QuestProgressTracker progressTracker,
            QuestStoryEventBus storyEventBus
    ) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.economy = economy;
        this.eventManager = eventManager;
        this.questRegistry = questRegistry;
        this.progressTracker = progressTracker;
        this.storyEventBus = storyEventBus;
    }

    public void start() {
        if (resetTask != null) {
            resetTask.cancel();
        }
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllResets, 200L, 1200L);
    }

    public void stop() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }

    public void setUpdateNotifier(QuestUpdateNotifier updateNotifier) {
        this.updateNotifier = updateNotifier;
    }

    public void registerEligibilityProvider(String id, QuestEligibilityProvider provider) {
        if (id == null || id.isBlank() || provider == null) {
            return;
        }
        eligibilityProviders.put(id.toLowerCase(Locale.ROOT), provider);
    }

    public void unregisterEligibilityProvider(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        eligibilityProviders.remove(id.toLowerCase(Locale.ROOT));
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        storage.loadPlayerProfile(uuid).thenAccept(profile -> Bukkit.getScheduler().runTask(plugin, () -> {
            progressTracker.set(profile);
            processResets(uuid, true);
            notifyUpdated(player);
        }));
    }

    public void unloadPlayer(Player player) {
        PlayerQuestProfile profile = progressTracker.remove(player.getUniqueId());
        if (profile != null) {
            storage.savePlayerProfile(profile);
        }
    }

    public void saveAll() {
        for (PlayerQuestProfile profile : progressTracker.all()) {
            storage.savePlayerProfile(profile);
        }
    }

    public boolean acceptQuest(Player player, String questId) {
        Quest quest = questRegistry.getQuest(questId);
        if (quest == null) {
            return false;
        }

        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        if (!canAccept(player, quest)) {
            return false;
        }

        String key = quest.id().toLowerCase();
        PlayerQuestState existing = profile.questStates().get(key);
        if (existing != null && !existing.isExpired(System.currentTimeMillis())) {
            return true;
        }

        long now = System.currentTimeMillis();
        long expiry = calculateExpiry(quest.type(), now);
        PlayerQuestState state = PlayerQuestState.createFresh(quest.id(), quest.type(), quest.targetValue(), expiry, now);
        profile.questStates().put(key, state);
        saveProfile(profile);
        notifyUpdated(player);
        return true;
    }

    public boolean completeQuest(Player player, String questId) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        PlayerQuestState state = profile.questStates().get(questId.toLowerCase());
        if (state == null || !state.completed() || state.claimed()) {
            return false;
        }

        Quest quest = questRegistry.getQuest(questId);
        if (quest == null) {
            return false;
        }

        applyRewards(player, quest.rewards());
        state.setClaimed(true);
        profile.addHistory(historyLine(quest.id(), quest.title(), System.currentTimeMillis(), "CLAIMED"));

        storyEventBus.fire("quest_reward_claimed", new QuestStoryContext(player, quest, Collections.emptyMap()));

        saveProfile(profile);
        notifyUpdated(player);
        return true;
    }

    public double checkProgress(Player player, String questId) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        PlayerQuestState state = profile.questStates().get(questId.toLowerCase());
        if (state == null) {
            return 0D;
        }
        return state.progressPercent();
    }

    public void resetPlayer(UUID uuid) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(uuid);
        profile.questStates().clear();
        profile.history().clear();
        profile.setLastDailyReset(0L);
        profile.setLastWeeklyReset(0L);
        profile.setLastMonthlyReset(0L);
        processResets(uuid, true);
        saveProfile(profile);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            notifyUpdated(player);
        }
    }

    public void onQuestAction(Player player, QuestAction action) {
        if (action == null) {
            return;
        }

        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        long now = System.currentTimeMillis();

        boolean changed = false;
        for (PlayerQuestState state : profile.questStates().values()) {
            Quest quest = questRegistry.getQuest(state.questId());
            if (quest == null) {
                continue;
            }

            if (state.isExpired(now)) {
                changed = true;
                continue;
            }
            if (state.completed() || state.claimed()) {
                continue;
            }
            if (!quest.task().matches(action)) {
                continue;
            }

            int previous = state.progress();
            state.increment(quest.task().progressAmount(action));
            state.setTarget(quest.targetValue());
            if (state.progress() != previous) {
                changed = true;
                Bukkit.getPluginManager().callEvent(new QuestProgressUpdateEvent(player, quest, state));
                sendProgressActionBar(player, quest, state);
            }

            if (state.completed()) {
                onReachedCompletion(player, quest, state, profile);
                changed = true;
            }
        }

        if (changed) {
            notifyUpdated(player);
        }
    }

    public void onExternalProgress(Player player, String sourceKey, int amount) {
        onQuestAction(player, QuestAction.external(sourceKey, amount));
    }

    public List<QuestProgressView> getActiveQuests(Player player) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        List<QuestProgressView> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (PlayerQuestState state : profile.questStates().values()) {
            if (state.isExpired(now)) {
                continue;
            }
            Quest quest = questRegistry.getQuest(state.questId());
            if (quest != null) {
                list.add(new QuestProgressView(quest, state));
            }
        }

        list.sort(Comparator
                .comparing((QuestProgressView view) -> view.quest().type().ordinal())
                .thenComparing(view -> view.quest().title()));
        return list;
    }

    public QuestProgressView getQuestView(Player player, String questId) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        PlayerQuestState state = profile.questStates().get(questId.toLowerCase());
        if (state == null) {
            return null;
        }
        Quest quest = questRegistry.getQuest(questId);
        if (quest == null) {
            return null;
        }
        return new QuestProgressView(quest, state);
    }

    public List<String> getHistory(Player player) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(player.getUniqueId());
        return new ArrayList<>(profile.history());
    }

    public Quest getQuestById(String questId) {
        return questRegistry.getQuest(questId);
    }

    public List<Quest> allQuests() {
        return new ArrayList<>(questRegistry.getAll());
    }

    public void processResets(UUID uuid, boolean assignIfMissing) {
        PlayerQuestProfile profile = progressTracker.getOrCreate(uuid);
        long now = System.currentTimeMillis();
        boolean changed = false;

        Iterator<Map.Entry<String, PlayerQuestState>> iterator = profile.questStates().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PlayerQuestState> entry = iterator.next();
            PlayerQuestState state = entry.getValue();
            if (!state.isExpired(now)) {
                continue;
            }
            iterator.remove();
            changed = true;
        }

        if (TimeUtil.needsDailyReset(profile.lastDailyReset(), config.dailyResetHours())) {
            resetByType(profile, QuestType.DAILY, now);
            profile.setLastDailyReset(now);
            changed = true;
        }
        if (TimeUtil.needsWeeklyReset(profile.lastWeeklyReset(), config.weeklyResetDay(), ZoneId.systemDefault())) {
            resetByType(profile, QuestType.WEEKLY, now);
            profile.setLastWeeklyReset(now);
            changed = true;
        }
        if (TimeUtil.needsMonthlyReset(profile.lastMonthlyReset(), config.monthlyResetDays())) {
            resetByType(profile, QuestType.MONTHLY, now);
            profile.setLastMonthlyReset(now);
            changed = true;
        }

        if (assignIfMissing) {
            changed |= assignMissing(profile, QuestType.DAILY, now);
            changed |= assignMissing(profile, QuestType.WEEKLY, now);
            changed |= assignMissing(profile, QuestType.MONTHLY, now);
        }

        if (changed) {
            saveProfile(profile);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                notifyUpdated(player);
            }
        }
    }

    private void checkAllResets() {
        for (PlayerQuestProfile profile : progressTracker.all()) {
            processResets(profile.uuid(), true);
        }
    }

    private void resetByType(PlayerQuestProfile profile, QuestType questType, long now) {
        Iterator<Map.Entry<String, PlayerQuestState>> iterator = profile.questStates().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PlayerQuestState> entry = iterator.next();
            PlayerQuestState state = entry.getValue();
            if (state.questType() == questType) {
                iterator.remove();
                if (state.completed() && !state.claimed()) {
                    profile.addHistory(historyLine(state.questId(), state.questId(), now, "EXPIRED"));
                }
            }
        }
    }

    private boolean assignMissing(PlayerQuestProfile profile, QuestType questType, long now) {
        int count = countByType(profile, questType, now);
        int need = Math.max(0, config.questsPerType() - count);
        if (need <= 0) {
            return false;
        }

        List<Quest> available = new ArrayList<>(questRegistry.getByType(questType));
        if (available.isEmpty()) {
            return false;
        }

        Collections.shuffle(available, ThreadLocalRandom.current());
        boolean changed = false;

        for (Quest quest : available) {
            if (need <= 0) {
                break;
            }
            String key = quest.id().toLowerCase();
            if (profile.questStates().containsKey(key)) {
                continue;
            }
            if (!quest.repeatable() && hasCompleted(profile, quest.id())) {
                continue;
            }
            long expiry = calculateExpiry(questType, now);
            PlayerQuestState state = PlayerQuestState.createFresh(quest.id(), questType, quest.targetValue(), expiry, now);
            profile.questStates().put(key, state);
            need--;
            changed = true;
        }

        return changed;
    }

    private boolean hasCompleted(PlayerQuestProfile profile, String questId) {
        String needle = questId.toLowerCase(Locale.ROOT) + "|";
        for (String history : profile.history()) {
            if (history.toLowerCase(Locale.ROOT).startsWith(needle)) {
                return true;
            }
        }
        return false;
    }

    private int countByType(PlayerQuestProfile profile, QuestType type, long now) {
        int count = 0;
        for (PlayerQuestState state : profile.questStates().values()) {
            if (state.questType() == type && !state.isExpired(now)) {
                count++;
            }
        }
        return count;
    }

    private long calculateExpiry(QuestType type, long now) {
        if (type == QuestType.DAILY) {
            return now + TimeUnit.HOURS.toMillis(config.dailyResetHours());
        }
        if (type == QuestType.MONTHLY) {
            return now + TimeUnit.DAYS.toMillis(config.monthlyResetDays());
        }
        ZonedDateTime zonedNow = ZonedDateTime.now();
        int current = zonedNow.getDayOfWeek().getValue();
        int target = config.weeklyResetDay().getValue();
        int daysUntil = (target - current + 7) % 7;
        if (daysUntil == 0) {
            daysUntil = 7;
        }
        return zonedNow.plusDays(daysUntil)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant().toEpochMilli();
    }

    private boolean canAccept(Player player, Quest quest) {
        if (!quest.canAccept(player)) {
            return false;
        }
        for (QuestEligibilityProvider provider : eligibilityProviders.values()) {
            if (!provider.canAccept(player, quest)) {
                return false;
            }
        }
        return true;
    }

    private void onReachedCompletion(Player player, Quest quest, PlayerQuestState state, PlayerQuestProfile profile) {
        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, quest, state));
        storyEventBus.fire("quest_completed", new QuestStoryContext(player, quest, Collections.emptyMap()));
        profile.addHistory(historyLine(quest.id(), quest.title(), System.currentTimeMillis(), "COMPLETED"));
        playCompletionCelebration(player);
        player.sendMessage("§aQuest completed: §f" + quest.title() + " §7- claim your rewards in the quest menu.");
        saveProfile(profile);
    }

    private void applyRewards(Player player, QuestReward reward) {
        double xpAmount = reward.xp() * config.questXpMultiplier() * eventManager.xpBoostMultiplier();
        double moneyAmount = reward.money() * config.questMoneyMultiplier() * eventManager.moneyBoostMultiplier();

        if (xpAmount > 0D) {
            player.giveExp((int) Math.round(xpAmount));
        }
        if (moneyAmount > 0D) {
            economy.deposit(player, moneyAmount);
        }
        for (ItemStack item : reward.items()) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            if (!leftovers.isEmpty()) {
                Location drop = player.getLocation();
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(drop, leftover);
                }
            }
        }

        player.sendMessage("§6Rewards claimed! §7+" + (int) Math.round(xpAmount)
                + " XP, §a" + economy.format(moneyAmount) + "§7, items delivered.");
    }

    private void saveProfile(PlayerQuestProfile profile) {
        storage.savePlayerProfile(profile);
    }

    private void notifyUpdated(Player player) {
        if (updateNotifier != null) {
            updateNotifier.onUpdate(player);
        }
    }

    private String historyLine(String questId, String title, long timestamp, String status) {
        return questId + "|" + title + "|" + timestamp + "|" + status;
    }

    private void sendProgressActionBar(Player player, Quest quest, PlayerQuestState state) {
        String bar = buildProgressBar(state.progressPercent());
        String message = "§6" + quest.title()
                + " §8| §f" + state.progress() + "/" + state.target()
                + " §8| " + bar
                + " §e" + (int) Math.round(state.progressPercent()) + "%";
        sendActionBarCompat(player, message);
    }

    private String buildProgressBar(double percent) {
        int filled = (int) Math.round(Math.max(0D, Math.min(100D, percent)) / 10D);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "§a▌" : "§7▌");
        }
        sb.append("§8]");
        return sb.toString();
    }

    private void sendActionBarCompat(Player player, String message) {
        try {
            player.getClass().getMethod("sendActionBar", String.class).invoke(player, message);
        } catch (Exception ignored) {
            player.sendTitle("", message, 0, 20, 5);
        }
    }

    private void playCompletionCelebration(Player player) {
        Location base = player.getLocation().clone().add(0D, 1D, 0D);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.15F);
        launchFirework(base, Color.AQUA, Color.LIME, 1L);
        launchFirework(base.clone().add(0.5D, 0D, 0.5D), Color.YELLOW, Color.ORANGE, 7L);
    }

    private void launchFirework(Location location, Color primary, Color fade, long detonateDelayTicks) {
        if (location.getWorld() == null) {
            return;
        }
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(primary)
                .withFade(fade)
                .flicker(true)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, Math.max(1L, detonateDelayTicks));
    }

    public record QuestProgressView(Quest quest, PlayerQuestState state) {
    }
}
