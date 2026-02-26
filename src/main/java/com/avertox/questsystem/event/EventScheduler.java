package com.avertox.questsystem.event;

import com.avertox.questsystem.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class EventScheduler {
    private final JavaPlugin plugin;
    private final MainConfig config;
    private final EventManager eventManager;

    private BukkitTask task;

    public EventScheduler(JavaPlugin plugin, MainConfig config, EventManager eventManager) {
        this.plugin = plugin;
        this.config = config;
        this.eventManager = eventManager;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, eventManager::schedulerTick, 20L, config.eventCheckIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
