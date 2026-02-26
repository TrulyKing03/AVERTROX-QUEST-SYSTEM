package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
import com.avertox.questsystem.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class QuestHistoryMenu implements BaseMenu {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final QuestManager questManager;
    private final MenuManager menuManager;
    private final Inventory inventory;

    public QuestHistoryMenu(QuestManager questManager, MenuManager menuManager) {
        this.questManager = questManager;
        this.menuManager = menuManager;
        this.inventory = Bukkit.createInventory(null, 54, "§d§lQuest History");
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
        if (event.getRawSlot() == 49) {
            menuManager.open(player, new QuestMainMenu(questManager, menuManager));
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.PURPLE_STAINED_GLASS_PANE, "§0");

        List<String> history = questManager.getHistory(player);
        int slot = 10;
        int shown = 0;

        for (String line : history) {
            if (shown >= 21) {
                break;
            }
            String[] split = line.split("\\|");
            String questId = split.length > 0 ? split[0] : "unknown";
            String title = split.length > 1 ? split[1] : questId;
            String time = split.length > 2 ? formatEpoch(split[2]) : "unknown";
            String status = split.length > 3 ? split[3] : "DONE";

            Material icon = switch (status.toUpperCase()) {
                case "CLAIMED" -> Material.EMERALD;
                case "COMPLETED" -> Material.LIME_DYE;
                case "EXPIRED" -> Material.GRAY_DYE;
                default -> Material.PAPER;
            };

            inventory.setItem(slot, MenuUtil.item(icon, "§f" + title, List.of(
                    "§7Quest: §f" + questId,
                    "§7Status: §f" + status,
                    "§7Time: §f" + time
            )));

            slot = nextSlot(slot);
            shown++;
        }

        if (shown == 0) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "§cNo History Yet", List.of(
                    "§7Complete quests to build history."
            )));
        }

        inventory.setItem(49, MenuUtil.item(Material.ARROW, "§fBack", List.of("§7Return to quest hub")));
    }

    private int nextSlot(int slot) {
        int next = slot + 1;
        if (next % 9 == 8) {
            next += 2;
        }
        return next;
    }

    private String formatEpoch(String raw) {
        try {
            return FORMATTER.format(Instant.ofEpochMilli(Long.parseLong(raw)));
        } catch (NumberFormatException ex) {
            return "unknown";
        }
    }
}
