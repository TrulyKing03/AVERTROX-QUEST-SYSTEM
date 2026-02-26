# API Developer Guide

This plugin exposes `QuestSystemApi` for third-party systems.

## Accessing API

From your plugin, resolve `AvertoxQuestSystemPlugin` and call `getApi()`.

```java
AvertoxQuestSystemPlugin questPlugin = (AvertoxQuestSystemPlugin) Bukkit.getPluginManager().getPlugin("AvertoxQuestSystem");
QuestSystemApi api = questPlugin.getApi();
```

## Core Methods

- `registerQuest(Quest quest)`
- `unregisterQuest(String questId)`
- `registerEvent(GlobalEvent event)`
- `triggerEvent(String eventId)`

## Custom Task Types

Register additional quest task behaviors:

```java
api.registerQuestTaskType("FISH_RARE", section -> new MyCustomQuestTask(section));
```

## Story Event Hooks

Register and dispatch quest story events:

```java
api.registerQuestEvent("chapter_1_complete", ctx -> {
    Player player = ctx.player();
    player.sendMessage("Story unlocked.");
});
```

## External Progress Integration (Job System)

Push progress from non-vanilla systems:

```java
api.recordExternalProgress(player, "jobs.mine.contract", 1);
```

## Eligibility Providers (Permissions / Job Tier)

Apply custom acceptance rules for quest assignment:

```java
api.registerEligibilityProvider("jobs-tier", (player, quest) -> player.hasPermission("jobs.tier.2"));
```

## Runtime Event String (Scoreboard)

Use for placeholders/scoreboards:

```java
String display = api.getCurrentOrUpcomingEventDisplay();
```

## Bukkit Event Hooks

Subscribe through Bukkit listener system:

- `QuestProgressUpdateEvent`
- `QuestCompletedEvent`
- `GlobalEventStartEvent`
- `GlobalEventEndEvent`

These let add-ons react to progression and global events without direct manager coupling.

---

## Rights

Developed by **TrulyKing03**  
All rights reserved.  
Contact: **TrulyKingDevs@gmail.com**
