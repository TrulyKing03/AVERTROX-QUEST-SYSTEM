package com.avertox.questsystem.config;

import com.avertox.questsystem.data.StorageMode;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.util.Locale;

public class MainConfig {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public MainConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public StorageMode storageMode() {
        return StorageMode.from(cfg.getString("storage.mode", "yaml"));
    }

    public int autosaveSeconds() {
        return Math.max(15, cfg.getInt("storage.autosave_seconds", 60));
    }

    public int questsPerType() {
        return Math.max(1, cfg.getInt("quests.assignment.per_type", 3));
    }

    public int dailyResetHours() {
        return Math.max(1, cfg.getInt("quests.assignment.daily_reset_hours", 24));
    }

    public DayOfWeek weeklyResetDay() {
        String raw = cfg.getString("quests.assignment.weekly_reset_day", "MONDAY");
        try {
            return DayOfWeek.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DayOfWeek.MONDAY;
        }
    }

    public int monthlyResetDays() {
        return Math.max(1, cfg.getInt("quests.assignment.monthly_reset_days", 30));
    }

    public double questXpMultiplier() {
        return Math.max(0D, cfg.getDouble("quests.rewards.xp_multiplier", 1.0D));
    }

    public double questMoneyMultiplier() {
        return Math.max(0D, cfg.getDouble("quests.rewards.money_multiplier", 1.0D));
    }

    public String defaultQuestPermission() {
        return cfg.getString("quests.integration.required_permission", "");
    }

    public boolean eventsSchedulerEnabled() {
        return cfg.getBoolean("events.scheduler.enabled", true);
    }

    public int eventIntervalMinutes() {
        return Math.max(5, cfg.getInt("events.scheduler.interval_minutes", 180));
    }

    public long eventCheckIntervalTicks() {
        return Math.max(200L, cfg.getLong("events.scheduler.check_interval_ticks", 1200L));
    }

    public boolean bossBarEnabled() {
        return cfg.getBoolean("events.broadcast.bossbar_enabled", true);
    }

    public double baseMiningMultiplier() {
        return Math.max(0D, cfg.getDouble("events.effects.mining_speed_base_multiplier", 1D));
    }

    public double baseDropRateMultiplier() {
        return Math.max(0D, cfg.getDouble("events.effects.drop_rate_base_multiplier", 1D));
    }

    public String placeholderNoneActive() {
        return cfg.getString("api.placeholder_format.none_active", "No active event");
    }

    public String placeholderUpcoming() {
        return cfg.getString("api.placeholder_format.upcoming", "Upcoming: %event% in %minutes%m");
    }

    public String placeholderActive() {
        return cfg.getString("api.placeholder_format.active", "Active: %event% (%minutes%m left)");
    }

    public String mysqlHost() {
        return cfg.getString("mysql.host", "localhost");
    }

    public int mysqlPort() {
        return cfg.getInt("mysql.port", 3306);
    }

    public String mysqlDatabase() {
        return cfg.getString("mysql.database", "avertox_quest");
    }

    public String mysqlUsername() {
        return cfg.getString("mysql.username", "root");
    }

    public String mysqlPassword() {
        return cfg.getString("mysql.password", "password");
    }

    public int mysqlPoolSize() {
        return Math.max(2, cfg.getInt("mysql.pool_size", 10));
    }
}
