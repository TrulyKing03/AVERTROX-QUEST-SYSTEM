package com.avertox.questsystem.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager implements Listener {
    private final Map<UUID, BaseMenu> openMenus = new ConcurrentHashMap<>();

    public void open(Player player, BaseMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    public void refreshIfOpen(Player player) {
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (!player.getOpenInventory().getTopInventory().equals(menu.getInventory())) {
            return;
        }
        menu.refresh(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (!event.getView().getTopInventory().equals(menu.getInventory())) {
            return;
        }

        event.setCancelled(true);
        menu.handleClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (event.getInventory().equals(menu.getInventory())) {
            openMenus.remove(player.getUniqueId());
        }
    }
}
