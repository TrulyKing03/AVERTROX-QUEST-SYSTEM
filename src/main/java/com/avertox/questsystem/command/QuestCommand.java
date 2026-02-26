package com.avertox.questsystem.command;

import com.avertox.questsystem.AvertoxQuestSystemPlugin;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.gui.menu.QuestMainMenu;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {
    private final AvertoxQuestSystemPlugin plugin;
    private final MenuManager menuManager;
    private final QuestManager questManager;

    public QuestCommand(AvertoxQuestSystemPlugin plugin, MenuManager menuManager, QuestManager questManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            menuManager.open(player, new QuestMainMenu(questManager, menuManager));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!sender.hasPermission("avertoxquest.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadSystem();
            sender.sendMessage("§aQuest and event definitions reloaded.");
            return true;
        }

        if (sub.equals("add") && args.length >= 3) {
            if (!sender.hasPermission("avertoxquest.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            String questId = args[2];
            boolean accepted = questManager.acceptQuest(target, questId);
            sender.sendMessage(accepted ? "§aQuest assigned." : "§cCould not assign quest.");
            return true;
        }

        if (sub.equals("reset") && args.length >= 2) {
            if (!sender.hasPermission("avertoxquest.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            questManager.resetPlayer(target.getUniqueId());
            sender.sendMessage("§aQuest profile reset for " + target.getName());
            return true;
        }

        if (sub.equals("accept") && args.length >= 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            boolean accepted = questManager.acceptQuest(player, args[1]);
            player.sendMessage(accepted ? "§aQuest accepted." : "§cCould not accept quest.");
            return true;
        }

        if (sub.equals("complete") && args.length >= 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            boolean completed = questManager.completeQuest(player, args[1]);
            player.sendMessage(completed ? "§aQuest rewards claimed." : "§cQuest is not ready.");
            return true;
        }

        if (sub.equals("check") && args.length >= 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            Quest quest = questManager.getQuestById(args[1]);
            if (quest == null) {
                player.sendMessage("§cUnknown quest id.");
                return true;
            }
            double progress = questManager.checkProgress(player, quest.id());
            player.sendMessage("§7" + quest.title() + " §f" + String.format("%.2f", progress) + "%");
            return true;
        }

        sender.sendMessage("§eUsage: /quest, /quest accept <id>, /quest complete <id>, /quest check <id>");
        sender.sendMessage("§eAdmin: /quest add <player> <id>, /quest reset <player>, /quest reload");
        return true;
    }
}
