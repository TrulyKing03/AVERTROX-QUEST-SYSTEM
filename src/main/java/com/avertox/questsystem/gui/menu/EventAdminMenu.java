package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventAdminMenu implements BaseMenu {
    private static final int[] EVENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int PER_PAGE = EVENT_SLOTS.length;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final EventManager eventManager;
    private final MenuManager menuManager;
    private final Inventory inventory;
    private final Map<Integer, String> eventSlotMap = new HashMap<>();
    private int page;

    public EventAdminMenu(EventManager eventManager, MenuManager menuManager) {
        this(eventManager, menuManager, 0);
    }

    public EventAdminMenu(EventManager eventManager, MenuManager menuManager, int page) {
        this.eventManager = eventManager;
        this.menuManager = menuManager;
        this.page = Math.max(0, page);
        this.inventory = Bukkit.createInventory(null, 54, "§4§lEvent Control Center");
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

        if (slot == 47) {
            page = Math.max(0, page - 1);
            refresh(player);
            return;
        }
        if (slot == 53) {
            page = page + 1;
            refresh(player);
            return;
        }
        if (slot == 49) {
            eventManager.stopActiveEvent(true);
            player.sendMessage("§aEvent system: active event stopped.");
            refresh(player);
            return;
        }
        if (slot == 50) {
            boolean triggered = eventManager.triggerRandomEvent(true);
            player.sendMessage(triggered ? "§aEvent system: random event triggered." : "§cNo enabled events to trigger.");
            refresh(player);
            return;
        }
        if (slot == 51) {
            refresh(player);
            return;
        }

        String eventId = eventSlotMap.get(slot);
        if (eventId == null) {
            return;
        }

        GlobalEvent target = eventManager.getEvent(eventId);
        if (target == null) {
            player.sendMessage("§cEvent no longer exists.");
            refresh(player);
            return;
        }

        if (event.getClick().isRightClick()) {
            menuManager.open(player, new EventAdminDetailsMenu(eventManager, menuManager, target.id(), page));
            return;
        }

        boolean broadcast = !event.getClick().isShiftClick();
        boolean started = eventManager.startEvent(target.id(), broadcast);
        if (!started) {
            player.sendMessage("§cCould not start event. Make sure it is enabled.");
        } else if (broadcast) {
            player.sendMessage("§aStarted event with broadcast: §f" + target.name());
        } else {
            player.sendMessage("§aStarted event silently: §f" + target.name());
        }
        refresh(player);
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        eventSlotMap.clear();

        MenuUtil.frame(inventory, Material.BLACK_STAINED_GLASS_PANE, "§0");
        decorateTop();

        GlobalEvent active = eventManager.getActiveEvent();
        if (active == null) {
            inventory.setItem(4, MenuUtil.item(Material.REDSTONE_BLOCK, "§c§lNo Active Global Event", List.of(
                    "§7Scheduler Status: §fMonitoring",
                    "§7Next Trigger In: §f" + TimeUtil.shortDuration(TimeUtil.millisUntil(eventManager.getNextTriggerEpochMs())),
                    "§7Upcoming Display: §f" + eventManager.getCurrentOrUpcomingDisplay()
            )));
        } else {
            inventory.setItem(4, MenuUtil.item(Material.BEACON, "§a§lActive: " + active.name(), List.of(
                    "§7Description: §f" + active.description(),
                    "§7Remaining: §f" + TimeUtil.shortDuration(eventManager.getActiveRemainingMillis()),
                    "§7Effects: §f" + active.effects().size()
            )));
        }

        List<GlobalEvent> events = eventManager.getRegisteredEvents();
        int maxPage = Math.max(0, (events.size() - 1) / PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
        }

        int start = page * PER_PAGE;
        int end = Math.min(events.size(), start + PER_PAGE);
        int writeIndex = 0;

        for (int i = start; i < end; i++) {
            GlobalEvent data = events.get(i);
            int slot = EVENT_SLOTS[writeIndex++];
            long lastTrigger = eventManager.getLastTriggerTime(data.id());

            Material icon = active != null && active.id().equalsIgnoreCase(data.id())
                    ? Material.BEACON
                    : data.enabled() ? Material.REDSTONE_TORCH : Material.GRAY_DYE;

            String last = lastTrigger <= 0L ? "Never" : TIME_FORMATTER.format(Instant.ofEpochMilli(lastTrigger));
            List<String> lore = List.of(
                    "§8ID: " + data.id(),
                    "§7Duration: §f" + data.durationMinutes() + "m",
                    "§7Effects: §f" + data.effects().size(),
                    "§7Last Trigger: §f" + last,
                    "§8",
                    "§aLeft Click: Start with broadcast",
                    "§eShift + Left Click: Start silently",
                    "§bRight Click: Open detail controls"
            );
            inventory.setItem(slot, MenuUtil.item(icon, title(data, active), lore));
            eventSlotMap.put(slot, data.id());
        }

        if (events.isEmpty()) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "§cNo Events Loaded", List.of(
                    "§7No event definitions are loaded.",
                    "§7Use §f/event status §7or reload definitions."
            )));
        }

        inventory.setItem(47, MenuUtil.item(Material.ARROW, "§fPrevious Page", List.of(
                "§7Current: §f" + (page + 1),
                "§7Max: §f" + (maxPage + 1)
        )));
        inventory.setItem(49, MenuUtil.item(Material.BARRIER, "§cStop Active Event", List.of(
                "§7Immediately end the active global event."
        )));
        inventory.setItem(50, MenuUtil.item(Material.LIGHTNING_ROD, "§6Trigger Random Event", List.of(
                "§7Force a random enabled event now."
        )));
        inventory.setItem(51, MenuUtil.item(Material.CLOCK, "§bRefresh Dashboard", List.of(
                "§7Refresh all event states and cards."
        )));
        inventory.setItem(53, MenuUtil.item(Material.ARROW, "§fNext Page", List.of(
                "§7Current: §f" + (page + 1),
                "§7Max: §f" + (maxPage + 1)
        )));
    }

    private void decorateTop() {
        Material[] palette = new Material[]{
                Material.RED_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE
        };
        for (int i = 0; i < palette.length; i++) {
            inventory.setItem(i + 1, MenuUtil.glass(palette[i], "§0"));
        }
    }

    private String title(GlobalEvent event, GlobalEvent active) {
        if (active != null && active.id().equalsIgnoreCase(event.id())) {
            return "§a§l" + event.name() + " §7(ACTIVE)";
        }
        if (!event.enabled()) {
            return "§8§l" + event.name() + " §7(DISABLED)";
        }
        return "§f§l" + event.name();
    }
}

