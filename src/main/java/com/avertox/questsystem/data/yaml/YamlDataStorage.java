package com.avertox.questsystem.data.yaml;

import com.avertox.questsystem.data.DataStorage;
import com.avertox.questsystem.model.EventRuntimeState;
import com.avertox.questsystem.model.PlayerQuestProfile;
import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.model.QuestType;
import com.avertox.questsystem.util.AsyncExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YamlDataStorage implements DataStorage {
    private final JavaPlugin plugin;
    private final AsyncExecutor async;
    private final Object ioLock = new Object();

    private final File playerFile;
    private final File eventFile;
    private final File questDefinitionsFile;
    private final File eventDefinitionsFile;

    public YamlDataStorage(JavaPlugin plugin, AsyncExecutor async) {
        this.plugin = plugin;
        this.async = async;
        this.playerFile = new File(plugin.getDataFolder(), "data/player_quests.yml");
        this.eventFile = new File(plugin.getDataFolder(), "data/events.yml");
        this.questDefinitionsFile = new File(plugin.getDataFolder(), "data/quest_definitions.yml");
        this.eventDefinitionsFile = new File(plugin.getDataFolder(), "data/event_definitions.yml");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return async.run(() -> {
            synchronized (ioLock) {
                try {
                    ensureFile(playerFile);
                    ensureFile(eventFile);
                    ensureFile(questDefinitionsFile);
                    ensureFile(eventDefinitionsFile);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed preparing YAML storage files: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<PlayerQuestProfile> loadPlayerProfile(UUID uuid) {
        return async.supply(() -> {
            PlayerQuestProfile profile = new PlayerQuestProfile(uuid);
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
                String base = "players." + uuid;
                profile.setLastDailyReset(yaml.getLong(base + ".last_daily_reset", 0L));
                profile.setLastWeeklyReset(yaml.getLong(base + ".last_weekly_reset", 0L));
                profile.setLastMonthlyReset(yaml.getLong(base + ".last_monthly_reset", 0L));

                List<String> history = yaml.getStringList(base + ".history");
                for (String entry : history) {
                    profile.addHistory(entry);
                }

                ConfigurationSection quests = yaml.getConfigurationSection(base + ".quests");
                if (quests != null) {
                    for (String questId : quests.getKeys(false)) {
                        String questBase = base + ".quests." + questId;
                        QuestType questType;
                        try {
                            questType = QuestType.valueOf(yaml.getString(questBase + ".type", "DAILY"));
                        } catch (IllegalArgumentException ex) {
                            questType = QuestType.DAILY;
                        }
                        PlayerQuestState state = new PlayerQuestState(
                                questId,
                                questType,
                                yaml.getInt(questBase + ".progress", 0),
                                yaml.getInt(questBase + ".target", 1),
                                yaml.getBoolean(questBase + ".completed", false),
                                yaml.getBoolean(questBase + ".claimed", false),
                                yaml.getLong(questBase + ".assigned_at", System.currentTimeMillis()),
                                yaml.getLong(questBase + ".completed_at", 0L),
                                yaml.getLong(questBase + ".expires_at", 0L),
                                yaml.getLong(questBase + ".last_reset", 0L)
                        );
                        profile.questStates().put(questId.toLowerCase(), state);
                    }
                }
            }
            return profile;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerProfile(PlayerQuestProfile profile) {
        return async.run(() -> {
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
                String base = "players." + profile.uuid();
                yaml.set(base + ".last_daily_reset", profile.lastDailyReset());
                yaml.set(base + ".last_weekly_reset", profile.lastWeeklyReset());
                yaml.set(base + ".last_monthly_reset", profile.lastMonthlyReset());
                yaml.set(base + ".history", profile.history());

                yaml.set(base + ".quests", null);
                for (PlayerQuestState state : profile.questStates().values()) {
                    String questBase = base + ".quests." + state.questId();
                    yaml.set(questBase + ".type", state.questType().name());
                    yaml.set(questBase + ".progress", state.progress());
                    yaml.set(questBase + ".target", state.target());
                    yaml.set(questBase + ".completed", state.completed());
                    yaml.set(questBase + ".claimed", state.claimed());
                    yaml.set(questBase + ".assigned_at", state.assignedAt());
                    yaml.set(questBase + ".completed_at", state.completedAt());
                    yaml.set(questBase + ".expires_at", state.expiresAt());
                    yaml.set(questBase + ".last_reset", state.lastReset());
                }

                try {
                    yaml.save(playerFile);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed saving player quest YAML: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<EventRuntimeState> loadEventRuntime() {
        return async.supply(() -> {
            EventRuntimeState state = new EventRuntimeState();
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(eventFile);
                state.setActiveEventId(yaml.getString("runtime.active_event_id", ""));
                state.setActiveUntil(yaml.getLong("runtime.active_until", 0L));
                state.setLastGlobalTrigger(yaml.getLong("runtime.last_global_trigger", 0L));

                ConfigurationSection events = yaml.getConfigurationSection("events");
                if (events != null) {
                    for (String eventId : events.getKeys(false)) {
                        long trigger = yaml.getLong("events." + eventId + ".last_trigger_time", 0L);
                        state.lastTriggerTimes().put(eventId.toLowerCase(), trigger);
                    }
                }
            }
            return state;
        });
    }

    @Override
    public CompletableFuture<Void> saveEventRuntime(EventRuntimeState state) {
        return async.run(() -> {
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(eventFile);
                yaml.set("runtime.active_event_id", state.activeEventId());
                yaml.set("runtime.active_until", state.activeUntil());
                yaml.set("runtime.last_global_trigger", state.lastGlobalTrigger());

                yaml.set("events", null);
                for (var entry : state.lastTriggerTimes().entrySet()) {
                    yaml.set("events." + entry.getKey() + ".last_trigger_time", entry.getValue());
                }

                try {
                    yaml.save(eventFile);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed saving event YAML: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Map<String, Object>>> loadQuestDefinitions() {
        return loadDefinitionMap(questDefinitionsFile, "quests");
    }

    @Override
    public CompletableFuture<Void> saveQuestDefinitions(Map<String, Map<String, Object>> definitions) {
        return saveDefinitionMap(questDefinitionsFile, "quests", definitions);
    }

    @Override
    public CompletableFuture<Map<String, Map<String, Object>>> loadEventDefinitions() {
        return loadDefinitionMap(eventDefinitionsFile, "events");
    }

    @Override
    public CompletableFuture<Void> saveEventDefinitions(Map<String, Map<String, Object>> definitions) {
        return saveDefinitionMap(eventDefinitionsFile, "events", definitions);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Map<String, Map<String, Object>>> loadDefinitionMap(File file, String rootKey) {
        return async.supply(() -> {
            Map<String, Map<String, Object>> output = new LinkedHashMap<>();
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection section = yaml.getConfigurationSection(rootKey);
                if (section == null) {
                    return output;
                }
                for (String key : section.getKeys(false)) {
                    ConfigurationSection nested = section.getConfigurationSection(key);
                    if (nested != null) {
                        Map<String, Object> values = toMap(nested);
                        values.putIfAbsent("id", key);
                        output.put(key.toLowerCase(), values);
                    }
                }
            }
            return output;
        });
    }

    private CompletableFuture<Void> saveDefinitionMap(File file, String rootKey, Map<String, Map<String, Object>> definitions) {
        return async.run(() -> {
            synchronized (ioLock) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                yaml.set(rootKey, null);
                if (definitions != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : definitions.entrySet()) {
                        yaml.set(rootKey + "." + entry.getKey(), entry.getValue());
                    }
                }
                try {
                    yaml.save(file);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed saving definition map: " + ex.getMessage());
                }
            }
        });
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) {
                output.put(key, toMap(nested));
                continue;
            }
            output.put(key, value);
        }
        return output;
    }

    private void ensureFile(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
