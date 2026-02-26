package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
import com.avertox.questsystem.model.EventEffect;
import com.avertox.questsystem.model.GlobalEvent;
import com.avertox.questsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EventAdminDetailsMenu implements BaseMenu {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final EventManager eventManager;
    private final MenuManager menuManager;
    private final String eventId;
    private final int returnPage;
    private final Inventory inventory;

    public EventAdminDetailsMenu(EventManager eventManager, MenuManager menuManager, String eventId, int returnPage) {
        this.eventManager = eventManager;
        this.menuManager = menuManager;
        this.eventId = eventId;
        this.returnPage = Math.max(0, returnPage);
        this.inventory = Bukkit.createInventory(null, 45, "§c§lEvent Details");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        refresh(player);
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();

        if (slot == 38) {
            menuManager.open(player, new EventAdminMenu(eventManager, menuManager, returnPage));
            return;
        }
        if (slot == 40) {
            refresh(player);
            return;
        }
        if (slot == 29) {
            boolean started = eventManager.startEvent(eventId, true);
            player.sendMessage(started ? "§aEvent started with broadcast." : "§cCould not start event.");
            refresh(player);
            return;
        }
        if (slot == 31) {
            boolean started = eventManager.startEvent(eventId, false);
            player.sendMessage(started ? "§aEvent started silently." : "§cCould not start event.");
            refresh(player);
            return;
        }
        if (slot == 33) {
            eventManager.stopActiveEvent(true);
            player.sendMessage("§aActive event stopped.");
            refresh(player);
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.NETHER_BRICK, "§0");

        GlobalEvent target = eventManager.getEvent(eventId);
        if (target == null) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "§cEvent Not Found", List.of(
                    "§7This event no longer exists in registry."
            )));
            inventory.setItem(38, MenuUtil.item(Material.ARROW, "§fBack", List.of("§7Return to event console")));
            return;
        }

        GlobalEvent active = eventManager.getActiveEvent();
        boolean isActive = active != null && active.id().equalsIgnoreCase(target.id());

        long lastTrigger = eventManager.getLastTriggerTime(target.id());
        String lastTriggerLine = lastTrigger <= 0L ? "Never" : TIME_FORMATTER.format(Instant.ofEpochMilli(lastTrigger));

        inventory.setItem(4, MenuUtil.item(isActive ? Material.BEACON : Material.REDSTONE_TORCH,
                isActive ? "§a§l" + target.name() + " (ACTIVE)" : "§f§l" + target.name(),
                List.of(
                        "§8ID: " + target.id(),
                        "§7Description: §f" + target.description(),
                        "§7Duration: §f" + target.durationMinutes() + "m",
                        "§7Enabled: " + (target.enabled() ? "§aYes" : "§cNo")
                )));

        inventory.setItem(19, MenuUtil.item(Material.CLOCK, "§bRuntime", List.of(
                "§7Last Trigger: §f" + lastTriggerLine,
                "§7Next Trigger In: §f" + TimeUtil.shortDuration(TimeUtil.millisUntil(eventManager.getNextTriggerEpochMs())),
                "§7Active Remaining: §f" + TimeUtil.shortDuration(eventManager.getActiveRemainingMillis())
        )));

        inventory.setItem(21, MenuUtil.item(Material.ENCHANTED_BOOK, "§dEffects", effectsLore(target.effects())));
        inventory.setItem(23, MenuUtil.item(Material.PAPER, "§eDisplay Preview", List.of(
                "§7" + eventManager.getCurrentOrUpcomingDisplay()
        )));
        inventory.setItem(25, MenuUtil.item(Material.COMPARATOR, "§6Control Notes", List.of(
                "§7Use controls below to run tests,",
                "§7force live starts, or stop events."
        )));

        inventory.setItem(29, MenuUtil.item(Material.LIME_CONCRETE, "§aStart (Broadcast)", List.of(
                "§7Starts this event and broadcasts",
                "§7chat/title/bossbar notifications."
        )));
        inventory.setItem(31, MenuUtil.item(Material.YELLOW_CONCRETE, "§eStart (Silent)", List.of(
                "§7Starts this event without broadcast.",
                "§7Useful for internal testing."
        )));
        inventory.setItem(33, MenuUtil.item(Material.RED_CONCRETE, "§cStop Active Event", List.of(
                "§7Ends whatever event is active now."
        )));

        inventory.setItem(38, MenuUtil.item(Material.ARROW, "§fBack to Event Console", List.of(
                "§7Return to page " + (returnPage + 1)
        )));
        inventory.setItem(40, MenuUtil.item(Material.CLOCK, "§bRefresh", List.of(
                "§7Refresh runtime/event information."
        )));
    }

    private List<String> effectsLore(List<EventEffect> effects) {
        if (effects.isEmpty()) {
            return List.of("§7No effects configured.");
        }
        List<String> lore = new ArrayList<>();
        for (EventEffect effect : effects) {
            if (effect.type() == null) {
                continue;
            }
            if (effect.type().name().equalsIgnoreCase("POTION_EFFECT") && effect.potionEffectType() != null) {
                lore.add("§f- " + effect.type().name() + ": " + effect.potionEffectType().getName()
                        + " (amp " + effect.amplifier() + ")");
            } else {
                lore.add("§f- " + effect.type().name() + ": " + String.format("%.2f", effect.value()));
            }
        }
        return lore;
    }
}

