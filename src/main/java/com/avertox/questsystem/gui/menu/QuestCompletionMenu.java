package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
import com.avertox.questsystem.quest.QuestManager;
import com.avertox.questsystem.quest.QuestManager.QuestProgressView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class QuestCompletionMenu implements BaseMenu {
    private final QuestManager questManager;
    private final MenuManager menuManager;
    private final String questId;
    private final Inventory inventory;

    public QuestCompletionMenu(QuestManager questManager, MenuManager menuManager, String questId) {
        this.questManager = questManager;
        this.menuManager = menuManager;
        this.questId = questId;
        this.inventory = Bukkit.createInventory(null, 27, "§a§lClaim Reward");
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
        if (slot == 15) {
            menuManager.open(player, new QuestDetailsMenu(questManager, menuManager, questId));
            return;
        }
        if (slot != 13) {
            return;
        }

        boolean claimed = questManager.completeQuest(player, questId);
        if (claimed) {
            player.sendMessage("§aReward claimed successfully.");
        } else {
            player.sendMessage("§cQuest is not ready to claim.");
        }
        menuManager.open(player, new QuestMainMenu(questManager, menuManager));
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.GREEN_STAINED_GLASS_PANE, "§0");

        QuestProgressView view = questManager.getQuestView(player, questId);
        if (view == null) {
            inventory.setItem(13, MenuUtil.item(Material.BARRIER, "§cQuest Not Found", List.of("§7This quest is no longer active.")));
            return;
        }

        boolean canClaim = view.state().completed() && !view.state().claimed();
        inventory.setItem(11, MenuUtil.item(Material.GOLD_BLOCK, "§6" + view.quest().title(), List.of(
                "§7Progress: §f" + view.state().progress() + "§7/§f" + view.state().target(),
                "§7Ready: " + (canClaim ? "§aYes" : "§cNo")
        )));

        inventory.setItem(13, MenuUtil.item(canClaim ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                canClaim ? "§a§lClaim Now" : "§7Unavailable",
                List.of(
                        canClaim ? "§7Click to receive reward pack" : "§7Complete quest first"
                )));

        inventory.setItem(15, MenuUtil.item(Material.ARROW, "§fBack", List.of("§7Return to details")));
    }
}
