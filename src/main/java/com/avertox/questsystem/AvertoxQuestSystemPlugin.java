package com.avertox.questsystem;

import com.avertox.questsystem.api.QuestStoryEventBus;
import com.avertox.questsystem.api.QuestSystemApi;
import com.avertox.questsystem.api.QuestSystemApiImpl;
import com.avertox.questsystem.command.EventCommand;
import com.avertox.questsystem.command.QuestCommand;
import com.avertox.questsystem.config.DefinitionLoader;
import com.avertox.questsystem.config.EventDefinitionParser;
import com.avertox.questsystem.config.LoadedDefinitions;
import com.avertox.questsystem.config.MainConfig;
import com.avertox.questsystem.config.QuestDefinitionParser;
import com.avertox.questsystem.data.StorageManager;
import com.avertox.questsystem.economy.EconomyService;
import com.avertox.questsystem.event.EventManager;
import com.avertox.questsystem.event.EventRegistry;
import com.avertox.questsystem.event.EventScheduler;
import com.avertox.questsystem.gui.MenuManager;
import com.avertox.questsystem.listener.PlayerConnectionListener;
import com.avertox.questsystem.listener.QuestProgressListener;
import com.avertox.questsystem.model.GlobalEvent;
import com.avertox.questsystem.quest.Quest;
import com.avertox.questsystem.quest.QuestManager;
import com.avertox.questsystem.quest.QuestProgressTracker;
import com.avertox.questsystem.quest.QuestRegistry;
import com.avertox.questsystem.quest.QuestTaskRegistry;
import com.avertox.questsystem.util.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;

public class AvertoxQuestSystemPlugin extends JavaPlugin {
    private MainConfig mainConfig;
    private AsyncExecutor asyncExecutor;
    private StorageManager storageManager;
    private EconomyService economyService;

    private QuestTaskRegistry questTaskRegistry;
    private QuestRegistry questRegistry;
    private QuestProgressTracker questProgressTracker;
    private QuestStoryEventBus questStoryEventBus;

    private EventRegistry eventRegistry;
    private EventManager eventManager;
    private EventScheduler eventScheduler;

    private QuestManager questManager;
    private MenuManager menuManager;

    private DefinitionLoader definitionLoader;
    private QuestDefinitionParser questParser;
    private EventDefinitionParser eventParser;

    private QuestSystemApi api;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        this.mainConfig = new MainConfig(this);
        this.asyncExecutor = new AsyncExecutor(this, 4);
        this.storageManager = new StorageManager(this, mainConfig, asyncExecutor);
        storageManager.initialize().join();

        this.economyService = new EconomyService(this);
        economyService.setup();

        this.questTaskRegistry = new QuestTaskRegistry();
        this.questRegistry = new QuestRegistry();
        this.questProgressTracker = new QuestProgressTracker();
        this.questStoryEventBus = new QuestStoryEventBus();

        this.eventRegistry = new EventRegistry();
        this.eventManager = new EventManager(this, mainConfig, storageManager, eventRegistry);
        this.eventScheduler = new EventScheduler(this, mainConfig, eventManager);

        this.questManager = new QuestManager(
                this,
                mainConfig,
                storageManager,
                economyService,
                eventManager,
                questRegistry,
                questProgressTracker,
                questStoryEventBus
        );

        this.menuManager = new MenuManager();
        questManager.setUpdateNotifier(menuManager::refreshIfOpen);

        this.definitionLoader = new DefinitionLoader(this);
        this.questParser = new QuestDefinitionParser(questTaskRegistry, mainConfig, getLogger());
        this.eventParser = new EventDefinitionParser(getLogger());

        this.api = new QuestSystemApiImpl(
                questRegistry,
                eventRegistry,
                eventManager,
                questTaskRegistry,
                questManager,
                questStoryEventBus
        );

        reloadSystem();

        storageManager.loadEventRuntime().thenAccept(state -> Bukkit.getScheduler().runTask(this, () -> {
            eventManager.hydrateRuntime(state);
            for (Player player : Bukkit.getOnlinePlayers()) {
                questManager.loadPlayer(player);
            }
        }));

        registerListeners();
        registerCommands();

        questManager.start();
        eventScheduler.start();
        scheduleAutosave();
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        if (eventScheduler != null) {
            eventScheduler.stop();
        }

        if (questManager != null) {
            questManager.stop();
            questManager.saveAll();
        }

        if (eventManager != null) {
            storageManager.saveEventRuntime(eventManager.getRuntimeState());
            eventManager.shutdown();
        }

        if (storageManager != null) {
            storageManager.shutdown();
        }

        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }

    public void reloadSystem() {
        mainConfig.reload();
        loadDefinitions();

        for (Player player : Bukkit.getOnlinePlayers()) {
            questManager.processResets(player.getUniqueId(), true);
        }
    }

    public QuestSystemApi getApi() {
        return api;
    }

    private void loadDefinitions() {
        LoadedDefinitions loaded = definitionLoader.load();
        Map<String, Map<String, Object>> questSections = new LinkedHashMap<>(loaded.questSections());
        Map<String, Map<String, Object>> eventSections = new LinkedHashMap<>(loaded.eventSections());

        if (!questSections.isEmpty()) {
            storageManager.saveQuestDefinitions(questSections);
        }
        if (!eventSections.isEmpty()) {
            storageManager.saveEventDefinitions(eventSections);
        }

        questRegistry.clear();
        eventRegistry.clear();

        int questCount = 0;
        for (Map<String, Object> section : questSections.values()) {
            Quest quest = questParser.parse(section);
            if (quest == null) {
                continue;
            }
            questRegistry.registerQuest(quest);
            questCount++;
        }

        int eventCount = 0;
        for (Map<String, Object> section : eventSections.values()) {
            GlobalEvent event = eventParser.parse(section);
            if (event == null) {
                continue;
            }
            eventRegistry.register(event);
            eventCount++;
        }

        getLogger().info("Loaded " + questCount + " quests and " + eventCount + " events.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(menuManager, this);
        pm.registerEvents(new PlayerConnectionListener(questManager, eventManager), this);
        pm.registerEvents(new QuestProgressListener(questManager, eventManager), this);
    }

    private void registerCommands() {
        PluginCommand questCmd = getCommand("quest");
        if (questCmd != null) {
            questCmd.setExecutor(new QuestCommand(this, menuManager, questManager));
        } else {
            getLogger().warning("Command /quest missing from plugin.yml");
        }

        PluginCommand eventCmd = getCommand("event");
        if (eventCmd != null) {
            eventCmd.setExecutor(new EventCommand(eventManager, menuManager));
        } else {
            getLogger().warning("Command /event missing from plugin.yml");
        }
    }

    private void scheduleAutosave() {
        long period = mainConfig.autosaveSeconds() * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            questManager.saveAll();
            storageManager.saveEventRuntime(eventManager.getRuntimeState());
        }, period, period);
    }
}
