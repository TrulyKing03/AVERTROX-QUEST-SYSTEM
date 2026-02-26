package com.avertox.questsystem.command;

import com.avertox.questsystem.event.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EventCommand implements CommandExecutor {
    private final EventManager eventManager;

    public EventCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("avertoxquest.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /event start <id>, /event stop, /event now, /event status");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /event start <id>");
                    return true;
                }
                boolean ok = eventManager.startEvent(args[1], true);
                sender.sendMessage(ok ? "§aEvent started." : "§cUnable to start event.");
            }
            case "stop" -> {
                eventManager.stopActiveEvent(true);
                sender.sendMessage("§aActive event stopped.");
            }
            case "now" -> {
                boolean ok = eventManager.triggerRandomEvent(true);
                sender.sendMessage(ok ? "§aRandom event triggered." : "§cNo enabled events available.");
            }
            case "status" -> sender.sendMessage("§7" + eventManager.getCurrentOrUpcomingDisplay());
            default -> sender.sendMessage("§eUsage: /event start <id>, /event stop, /event now, /event status");
        }
        return true;
    }
}
