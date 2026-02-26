package com.avertox.questsystem.data;

import com.avertox.questsystem.config.MainConfig;
import com.avertox.questsystem.data.mysql.MySqlDataStorage;
import com.avertox.questsystem.data.yaml.YamlDataStorage;
import com.avertox.questsystem.model.EventRuntimeState;
import com.avertox.questsystem.model.PlayerQuestProfile;
import com.avertox.questsystem.util.AsyncExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StorageManager {
    private final JavaPlugin plugin;
    private final MainConfig config;
    private final AsyncExecutor async;
    private DataStorage delegate;

    public StorageManager(JavaPlugin plugin, MainConfig config, AsyncExecutor async) {
        this.plugin = plugin;
        this.config = config;
        this.async = async;
    }

    public CompletableFuture<Void> initialize() {
        this.delegate = buildDelegate();
        return delegate.initialize();
    }

    public CompletableFuture<PlayerQuestProfile> loadPlayerProfile(UUID uuid) {
        return delegate.loadPlayerProfile(uuid);
    }

    public CompletableFuture<Void> savePlayerProfile(PlayerQuestProfile profile) {
        return delegate.savePlayerProfile(profile);
    }

    public CompletableFuture<EventRuntimeState> loadEventRuntime() {
        return delegate.loadEventRuntime();
    }

    public CompletableFuture<Void> saveEventRuntime(EventRuntimeState state) {
        return delegate.saveEventRuntime(state);
    }

    public CompletableFuture<Map<String, Map<String, Object>>> loadQuestDefinitions() {
        return delegate.loadQuestDefinitions();
    }

    public CompletableFuture<Void> saveQuestDefinitions(Map<String, Map<String, Object>> definitions) {
        return delegate.saveQuestDefinitions(definitions);
    }

    public CompletableFuture<Map<String, Map<String, Object>>> loadEventDefinitions() {
        return delegate.loadEventDefinitions();
    }

    public CompletableFuture<Void> saveEventDefinitions(Map<String, Map<String, Object>> definitions) {
        return delegate.saveEventDefinitions(definitions);
    }

    public CompletableFuture<Void> shutdown() {
        if (delegate == null) {
            return CompletableFuture.completedFuture(null);
        }
        return delegate.close();
    }

    public StorageMode mode() {
        return config.storageMode();
    }

    private DataStorage buildDelegate() {
        if (config.storageMode() == StorageMode.MYSQL) {
            plugin.getLogger().info("Storage backend: MySQL");
            return new MySqlDataStorage(plugin, config, async);
        }
        plugin.getLogger().info("Storage backend: YAML");
        return new YamlDataStorage(plugin, async);
    }
}
