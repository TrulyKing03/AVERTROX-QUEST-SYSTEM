package com.avertox.questsystem.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class AsyncExecutor {
    private final JavaPlugin plugin;
    private final ExecutorService pool;

    public AsyncExecutor(JavaPlugin plugin, int threads) {
        this.plugin = plugin;
        this.pool = Executors.newFixedThreadPool(Math.max(2, threads));
    }

    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, pool);
    }

    public CompletableFuture<Void> run(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, pool);
    }

    public void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
