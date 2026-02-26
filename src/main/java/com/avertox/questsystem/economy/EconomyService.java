package com.avertox.questsystem.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class EconomyService {
    private final JavaPlugin plugin;

    private Object provider;
    private Method depositMethod;
    private Method formatMethod;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean setup() {
        try {
            Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                plugin.getLogger().warning("Vault economy provider not found. Money rewards will be skipped.");
                return false;
            }

            Object candidate = registration.getProvider();
            if (candidate == null) {
                plugin.getLogger().warning("Vault economy provider returned null. Money rewards will be skipped.");
                return false;
            }

            this.depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            this.formatMethod = economyClass.getMethod("format", double.class);
            this.provider = candidate;
            plugin.getLogger().info("Vault economy hooked successfully.");
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Vault API not available at compile/runtime. Money rewards will be skipped.");
            return false;
        }
    }

    public boolean available() {
        return provider != null && depositMethod != null;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!available() || amount <= 0D) {
            return;
        }
        try {
            depositMethod.invoke(provider, player, amount);
        } catch (Exception ignored) {
        }
    }

    public String format(double amount) {
        if (provider == null || formatMethod == null) {
            return String.format("$%.2f", amount);
        }
        try {
            Object value = formatMethod.invoke(provider, amount);
            return value == null ? String.format("$%.2f", amount) : String.valueOf(value);
        } catch (Exception ignored) {
            return String.format("$%.2f", amount);
        }
    }
}
