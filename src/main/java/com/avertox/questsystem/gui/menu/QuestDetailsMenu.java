package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestManager;
import com.avertox.questsystem.quest.QuestManager.QuestProgressView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QuestDetailsMenu implements BaseMenu {
    private final QuestManager questManager;
    private final MenuManager menuManager;
    private final String questId;
    private final Inventory inventory;

    public QuestDetailsMenu(QuestManager questManager, MenuManager menuManager, String questId) {
        this.questManager = questManager;
        this.menuManager = menuManager;
        this.questId = questId;
        this.inventory = Bukkit.createInventory(null, 45, "§9§lQuest Details");
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
        if (slot == 40) {
            menuManager.open(player, new QuestMainMenu(questManager, menuManager));
            return;
        }
        if (slot == 44) {
            menuManager.open(player, new QuestHistoryMenu(questManager, menuManager));
            return;
        }
        if (slot == 31) {
            menuManager.open(player, new QuestCompletionMenu(questManager, menuManager, questId));
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.BLUE_STAINED_GLASS_PANE, "§0");

        QuestProgressView view = questManager.getQuestView(player, questId);
        if (view == null) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "§cQuest Unavailable", List.of(
                    "§7This quest is no longer active."
            )));
            inventory.setItem(40, MenuUtil.item(Material.ARROW, "§fBack", List.of("§7Return to quest hub")));
            return;
        }

        Quest quest = view.quest();
        PlayerQuestState state = view.state();

        inventory.setItem(4, MenuUtil.item(Material.ENCHANTED_BOOK, "§b§l" + quest.title(), List.of(
                "§7Type: §f" + quest.type().display(),
                "§7Task: §f" + quest.task().describeTarget()
        )));

        inventory.setItem(20, MenuUtil.item(Material.PAPER, "§fObjective", List.of(
                "§7" + quest.description(),
                "§8",
                "§7Need: §f" + state.target(),
                "§7Remaining: §f" + state.remaining()
        )));

        inventory.setItem(22, MenuUtil.item(Material.CLOCK, "§eProgress", List.of(
                "§7Current: §f" + state.progress() + "§7/§f" + state.target(),
                MenuUtil.bar(state.progressPercent()),
                "§7Completion: §f" + String.format("%.2f", state.progressPercent()) + "%"
        )));

        inventory.setItem(24, rewardItem(quest));

        if (state.completed() && !state.claimed()) {
            inventory.setItem(31, MenuUtil.item(Material.LIME_CONCRETE, "§a§lClaim Reward", List.of(
                    "§7You completed this quest.",
                    "§fClick to claim rewards"
            )));
        } else if (state.claimed()) {
            inventory.setItem(31, MenuUtil.item(Material.GREEN_STAINED_GLASS_PANE, "§aReward Claimed", List.of(
                    "§7Rewards already collected."
            )));
        } else {
            inventory.setItem(31, MenuUtil.item(Material.GRAY_CONCRETE, "§7Not Complete Yet", List.of(
                    "§7Finish objective to unlock claim."
            )));
        }

        inventory.setItem(40, MenuUtil.item(Material.ARROW, "§fBack", List.of("§7Return to quest hub")));
        inventory.setItem(44, MenuUtil.item(Material.WRITABLE_BOOK, "§dHistory", List.of("§7Open completion history")));
    }

    private ItemStack rewardItem(Quest quest) {
        List<String> lore = new ArrayList<>();
        lore.add("§7XP: §f" + (int) quest.rewards().xp());
        lore.add("§7Money: §a$" + String.format("%.2f", quest.rewards().money()));
        if (!quest.rewards().items().isEmpty()) {
            lore.add("§8");
            lore.add("§7Items:");
            for (ItemStack item : quest.rewards().items()) {
                lore.add("§f- " + item.getType().name() + " x" + item.getAmount());
            }
        }
        return MenuUtil.item(Material.CHEST, "§6Reward Package", lore);
    }
}
