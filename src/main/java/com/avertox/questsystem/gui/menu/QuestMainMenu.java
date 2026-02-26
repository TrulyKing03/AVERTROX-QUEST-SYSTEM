package com.avertox.questsystem.gui.menu;

import com.avertox.questsystem.gui.BaseMenu;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.MenuUtil;
import com.avertox.questsystem.quest.QuestManager;
import com.avertox.questsystem.quest.QuestManager.QuestProgressView;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.model.QuestType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestMainMenu implements BaseMenu {
    private final QuestManager questManager;
    private final MenuManager menuManager;
    private final Inventory inventory;
    private final Map<Integer, String> questSlots = new HashMap<>();

    public QuestMainMenu(QuestManager questManager, MenuManager menuManager) {
        this.questManager = questManager;
        this.menuManager = menuManager;
        this.inventory = Bukkit.createInventory(null, 54, "§6§lAvertox Quest Nexus");
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
        if (slot == 49) {
            menuManager.open(player, new QuestHistoryMenu(questManager, menuManager));
            return;
        }
        if (slot == 50) {
            refresh(player);
            return;
        }

        String questId = questSlots.get(slot);
        if (questId == null) {
            return;
        }
        menuManager.open(player, new QuestDetailsMenu(questManager, menuManager, questId));
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        questSlots.clear();

        MenuUtil.frame(inventory, Material.BLACK_STAINED_GLASS_PANE, "§0");
        fillGradient();

        inventory.setItem(4, MenuUtil.item(Material.NETHER_STAR, "§6§lAvertox Quest Nexus", List.of(
                "§7Live contracts updated in real-time.",
                "§7Complete objectives, claim rewards,",
                "§7and track your legacy from one hub."
        )));

        List<QuestProgressView> active = questManager.getActiveQuests(player);
        int[] slots = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        int used = 0;
        for (QuestProgressView view : active) {
            if (used >= slots.length) {
                break;
            }
            Quest quest = view.quest();
            PlayerQuestState state = view.state();
            int slot = slots[used++];

            String status = state.claimed() ? "§aClaimed" : state.completed() ? "§eReady to Claim" : "§7In Progress";
            List<String> lore = List.of(
                    "§8" + typeLabel(quest.type()),
                    "§7" + quest.description(),
                    "§8",
                    "§7Progress: §f" + state.progress() + "§7/§f" + state.target(),
                    MenuUtil.bar(state.progressPercent()),
                    "§7Completion: §f" + String.format("%.1f", state.progressPercent()) + "%",
                    "§8",
                    "§7Status: " + status,
                    "§bClick for full details"
            );

            inventory.setItem(slot, MenuUtil.item(typeIcon(quest.type()), title(quest, state), lore));
            questSlots.put(slot, quest.id());
        }

        if (active.isEmpty()) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "§cNo Active Quests", List.of(
                    "§7No contracts are currently active.",
                    "§7Use §f/quest reload §7or wait for reset cycle."
            )));
        }

        inventory.setItem(49, MenuUtil.item(Material.WRITABLE_BOOK, "§dQuest History", List.of(
                "§7Review your latest completions",
                "§7and claim timeline."
        )));
        inventory.setItem(50, MenuUtil.item(Material.CLOCK, "§bRefresh", List.of(
                "§7Refresh this dashboard now."
        )));
    }

    private void fillGradient() {
        for (int i = 1; i < 8; i++) {
            if (i % 2 == 0) {
                inventory.setItem(i, MenuUtil.glass(Material.GRAY_STAINED_GLASS_PANE, "§0"));
            } else {
                inventory.setItem(i, MenuUtil.glass(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§0"));
            }
        }
    }

    private Material typeIcon(QuestType type) {
        return switch (type) {
            case DAILY -> Material.SUNFLOWER;
            case WEEKLY -> Material.AMETHYST_SHARD;
            case MONTHLY -> Material.NETHER_STAR;
        };
    }

    private String title(Quest quest, PlayerQuestState state) {
        if (state.completed() && !state.claimed()) {
            return "§e§l" + quest.title();
        }
        if (state.claimed()) {
            return "§a§l" + quest.title();
        }
        return "§f§l" + quest.title();
    }

    private String typeLabel(QuestType type) {
        return switch (type) {
            case DAILY -> "Daily Contract";
            case WEEKLY -> "Weekly Contract";
            case MONTHLY -> "Monthly Contract";
        };
    }
}
