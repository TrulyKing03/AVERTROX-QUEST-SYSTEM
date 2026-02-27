# AVERTROX-QUEST-SYSTEM

<p align="center">
  <b>Advanced Quest and Global Event Framework for Spigot/Paper 1.20.4</b><br>
  Developed by <b>TrulyKing03</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0.0-brightgreen.svg?style=for-the-badge" alt="Version"/>
  <img src="https://img.shields.io/badge/API-Spigot%201.20.4-blue?style=for-the-badge" alt="Spigot API"/>
  <img src="https://img.shields.io/badge/Language-Java%2017-orange?style=for-the-badge" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Storage-MySQL%20or%20YAML-informational?style=for-the-badge" alt="Storage"/>
  <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge" alt="Status"/>
  <img src="https://img.shields.io/badge/License-Private-red?style=for-the-badge" alt="License"/>
</p>

---

## Overview

**AvertoxQuestSystem** is a complete quest + timed event infrastructure designed for RPG and progression servers.

It includes:
- Daily / Weekly / Monthly quest assignment and reset lifecycle
- Per-player progress tracking with expiration and reward claiming
- Live actionbar quest progress feedback with visual bar updates
- Global event scheduler with configurable effects and broadcast delivery
- Mining event boost that accelerates both mining animation and actual block break speed
- Visit-coordinate quest validation/regeneration (world border + Y-level safety)
- Async persistence layer with pluggable storage (`yaml` or `mysql`)
- External API for add-ons, story scripting, and custom quest logic
- Polished GUI menus for active quests, quest detail, claim flow, and history

---

## Feature Matrix

| System | Included |
|---|---|
| Core Managers | `QuestManager`, `EventManager`, `EventScheduler` |
| Quest Types | Collect, Kill, Mine, Craft, Visit Coordinates |
| Assignment Cadence | Daily (24h), Weekly (configured weekday), Monthly (30 days) |
| Persistence | Player progress, reset timers, event trigger logs, definition snapshots |
| Storage Backends | YAML and MySQL (switchable in config) |
| Integrations | Economy (Vault runtime hook), permission-based gating, external progress API |
| GUI | Quest suite + Event Admin Control GUI (console + detail panels) |
| Feedback UX | Actionbar progress bar + quest completion celebration effects |
| Safety Guards | Auto-fix invalid visit locations and mining boost conflict hardening |
| Admin Commands | `/quest add/reset/reload`, `/event start/stop/now/status` |
| API Hooks | Register quests/events/tasks, trigger story events, subscribe via Bukkit events |

---

## Requirements

- Java 17
- Maven 3.8+
- Spigot/Paper 1.20.4-compatible server
- Optional: Vault + economy provider
- Optional: MySQL 8+ (if `storage.mode: mysql`)

---

## Build

```bash
mvn clean package
```

Output jar:
- `target/AvertoxQuestSystem-1.0.0.jar`

---

## Installation

1. Build and place jar in `plugins/`.
2. Start server once to generate config + definition files.
3. Edit `plugins/AvertoxQuestSystem/config.yml`.
4. Adjust quest/event definitions under `plugins/AvertoxQuestSystem/definitions/`.
5. Restart server.

---

## Configuration

Primary files:
- `src/main/resources/config.yml`
- `src/main/resources/definitions/quests.yml`
- `src/main/resources/definitions/events.yml`

Key options:
- Storage mode (`yaml` or `mysql`)
- Quest reset cadence and per-type assignment size
- Reward multipliers (XP and money)
- Event interval and scheduler tick interval
- Event placeholder string formats (active/upcoming)

Definition formats supported:
- YAML (`.yml`, `.yaml`)
- JSON (`.json`)

Validation is applied at load; invalid definitions are logged and skipped safely.

---

## Commands

### Player
- `/quest`
- `/quest accept <quest_id>`
- `/quest complete <quest_id>`
- `/quest check <quest_id>`

### Admin (`avertoxquest.admin`)
- `/quest add <player> <quest_id>`
- `/quest reset <player>`
- `/quest reload`
- `/event` (opens Event Admin GUI)
- `/event gui` (opens Event Admin GUI)
- `/event start <id>`
- `/event stop`
- `/event now`
- `/event status`

---

## Event Effects

Supported global effects:
- Walk speed multiplier
- Mining speed modifier
- Money boost multiplier
- XP boost multiplier
- Drop rate multiplier
- Custom potion effects

Event lifecycle:
1. Triggered manually (`/event ...`) or by scheduler
2. Effects applied globally
3. Chat/title/bossbar broadcast shown
4. Auto-clear on duration end
5. End broadcast + state cleanup

---

## Data Design

### MySQL tables
- `player_quests`
- `player_quest_meta`
- `player_quest_history`
- `events`
- `event_runtime`
- `quest_definitions`
- `event_definitions`

### YAML storage files
- `data/player_quests.yml`
- `data/events.yml`
- `data/quest_definitions.yml`
- `data/event_definitions.yml`

All save/load operations run async through the plugin executor.

---

## API and Extensibility

Implemented API endpoints:
- `registerQuest(Quest)`
- `unregisterQuest(String)`
- `registerEvent(GlobalEvent)`
- `triggerEvent(String)`
- `registerQuestTaskType(String, QuestTaskFactory)`
- `registerEligibilityProvider(String, QuestEligibilityProvider)`
- `registerQuestEvent(String, Consumer<QuestStoryContext>)`
- `recordExternalProgress(Player, String, int)`
- `getCurrentOrUpcomingEventDisplay()`

Story scripting support:
- `registerQuestEvent(eventKey, handler)` style hook via `QuestStoryEventBus`

Bukkit hook events:
- `QuestProgressUpdateEvent`
- `QuestCompletedEvent`
- `GlobalEventStartEvent`
- `GlobalEventEndEvent`

Detailed API usage is available in [`docs/API.md`](docs/API.md).

---

## GUI Suite

Menus included:
- Quest Main Menu (active contracts with progress percent by type)
- Quest Details Menu (objective, remaining amount, rewards)
- Quest Completion Menu (explicit claim action)
- Quest History Menu (completion/claim timeline)
- Event Admin Control Center (`/event` or `/event gui`)
- Event Admin Detail Panel (per-event runtime and direct controls)

Menu refresh is wired to quest progress updates for live UX.
Quest progress is also surfaced in the actionbar as a live progress bar while objectives advance.

Completion celebration:
- Firework burst at player location
- Level-up sound cue (`ENTITY_PLAYER_LEVELUP`)

---

## Event Admin GUI Controls

Main console (`/event`):
- Left click event card: start selected event with broadcast
- Shift + left click: start selected event silently
- Right click event card: open event detail panel
- Global controls: stop active, trigger random, refresh, page navigation

Detail panel:
- Start with broadcast
- Start silently
- Stop active event
- View effects, last trigger time, remaining active time, next trigger ETA

Full operator notes: [`docs/ADMIN_EVENT_GUI.md`](docs/ADMIN_EVENT_GUI.md)

---

## Understanding the Plugin (Player FAQ)

### What is a quest?
A quest is a timed contract (Daily, Weekly, Monthly) with one objective and one reward package.

### How do I start?
Run `/quest` and complete active objectives shown in your Quest Main Menu.

### How do I claim rewards?
When progress reaches 100%, open quest details and click **Claim Reward**.

### What rewards do I get?
Quests can award XP, money, and item bundles from the quest definition.

### Do quests reset?
Yes. Daily, weekly, and monthly quests reset by configured intervals and reassign automatically.

### Can admins force quests?
Yes. `/quest add <player> <quest_id>` and `/quest reset <player>`.

---

## How Systems Interact (Big Picture)

1. Definitions load from YAML/JSON at startup/reload.
2. QuestManager assigns timed quests per player profile.
3. Gameplay events (break/kill/craft/collect/move) feed progress updates.
4. Completion unlocks reward claim flow in GUI.
5. EventScheduler triggers global events on cadence or admin command.
6. EventManager applies world modifiers and broadcasts start/end messages.
7. Storage layer saves player progress + runtime state asynchronously.
8. External systems use API hooks to add tasks, events, and custom progression.

---

## Progression Deep Dive

### 1) Quest Lifecycle
Assigned -> Progressed -> Completed -> Claimed -> Reset/Reassigned.

### 2) Assignment Cadence
- Daily: 24h interval
- Weekly: configured reset weekday
- Monthly: 30-day interval

### 3) Reward Scaling
XP and money are multiplied by configurable formulas and active global event boosts.

### 4) Event Synergy
Global events can boost XP, money, drop rate, mining speed, and movement, directly affecting quest pace.

### 5) Safety and Persistence
Reset timers and event runtime are persisted, so restart recovery continues cadence safely.

---

## Notes

- Built with Spigot API `1.20.4-R0.1-SNAPSHOT`
- Java 17 target
- Vault integration is runtime-discovered (no hard dependency required)

---

## Developer & Rights

Developed by **TrulyKing03**  
All rights reserved.  
Email: **TrulyKingDevs@gmail.com**

<p align="center">
  <sub><b>AvertoxQuestSystem</b> - Designed for scalable progression-driven servers.</sub>
</p>
