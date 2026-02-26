package com.avertox.questsystem.data.mysql;

import com.avertox.questsystem.config.MainConfig;
import com.avertox.questsystem.data.DataStorage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.avertox.questsystem.model.EventRuntimeState;
import com.avertox.questsystem.model.PlayerQuestProfile;
import com.avertox.questsystem.model.PlayerQuestState;
import com.avertox.questsystem.model.QuestType;
import com.avertox.questsystem.util.AsyncExecutor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySqlDataStorage implements DataStorage {
    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;
    private final MainConfig config;
    private final AsyncExecutor async;
    private HikariDataSource dataSource;

    public MySqlDataStorage(JavaPlugin plugin, MainConfig config, AsyncExecutor async) {
        this.plugin = plugin;
        this.config = config;
        this.async = async;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return async.run(() -> {
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl("jdbc:mysql://" + config.mysqlHost() + ":" + config.mysqlPort() + "/"
                    + config.mysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true");
            hikari.setUsername(config.mysqlUsername());
            hikari.setPassword(config.mysqlPassword());
            hikari.setMaximumPoolSize(config.mysqlPoolSize());
            hikari.setPoolName("AvertoxQuestSystemPool");
            this.dataSource = new HikariDataSource(hikari);
            createTables();
        });
    }

    @Override
    public CompletableFuture<PlayerQuestProfile> loadPlayerProfile(UUID uuid) {
        return async.supply(() -> {
            PlayerQuestProfile profile = new PlayerQuestProfile(uuid);
            if (dataSource == null) {
                return profile;
            }

            try (Connection connection = dataSource.getConnection()) {
                loadMeta(connection, profile);
                loadQuestStates(connection, profile);
                loadHistory(connection, profile);
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed loading player profile " + uuid + ": " + ex.getMessage());
            }
            return profile;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerProfile(PlayerQuestProfile profile) {
        return async.run(() -> {
            if (dataSource == null) {
                return;
            }

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    saveMeta(connection, profile);
                    saveQuestStates(connection, profile);
                    saveHistory(connection, profile);
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed saving player profile " + profile.uuid() + ": " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<EventRuntimeState> loadEventRuntime() {
        return async.supply(() -> {
            EventRuntimeState state = new EventRuntimeState();
            if (dataSource == null) {
                return state;
            }

            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT event_id, last_trigger_time FROM events")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        state.lastTriggerTimes().put(rs.getString("event_id").toLowerCase(), rs.getLong("last_trigger_time"));
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT active_event_id, active_until, last_global_trigger FROM event_runtime WHERE runtime_key='GLOBAL'"
                )) {
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        state.setActiveEventId(rs.getString("active_event_id"));
                        state.setActiveUntil(rs.getLong("active_until"));
                        state.setLastGlobalTrigger(rs.getLong("last_global_trigger"));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed loading event runtime: " + ex.getMessage());
            }
            return state;
        });
    }

    @Override
    public CompletableFuture<Void> saveEventRuntime(EventRuntimeState state) {
        return async.run(() -> {
            if (dataSource == null) {
                return;
            }

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement replaceEvent = connection.prepareStatement(
                            "REPLACE INTO events(event_id, last_trigger_time) VALUES (?,?)"
                    )) {
                        for (var entry : state.lastTriggerTimes().entrySet()) {
                            replaceEvent.setString(1, entry.getKey());
                            replaceEvent.setLong(2, entry.getValue());
                            replaceEvent.addBatch();
                        }
                        replaceEvent.executeBatch();
                    }

                    try (PreparedStatement runtime = connection.prepareStatement(
                            "REPLACE INTO event_runtime(runtime_key, active_event_id, active_until, last_global_trigger) VALUES ('GLOBAL', ?, ?, ?)"
                    )) {
                        runtime.setString(1, state.activeEventId());
                        runtime.setLong(2, state.activeUntil());
                        runtime.setLong(3, state.lastGlobalTrigger());
                        runtime.executeUpdate();
                    }

                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed saving event runtime: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Map<String, Object>>> loadQuestDefinitions() {
        return loadDefinitionTable("quest_definitions");
    }

    @Override
    public CompletableFuture<Void> saveQuestDefinitions(Map<String, Map<String, Object>> definitions) {
        return saveDefinitionTable("quest_definitions", definitions);
    }

    @Override
    public CompletableFuture<Map<String, Map<String, Object>>> loadEventDefinitions() {
        return loadDefinitionTable("event_definitions");
    }

    @Override
    public CompletableFuture<Void> saveEventDefinitions(Map<String, Map<String, Object>> definitions) {
        return saveDefinitionTable("event_definitions", definitions);
    }

    @Override
    public CompletableFuture<Void> close() {
        return async.run(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        });
    }

    private void createTables() {
        String playerQuests = "CREATE TABLE IF NOT EXISTS player_quests ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "quest_id VARCHAR(96) NOT NULL,"
                + "quest_type VARCHAR(16) NOT NULL,"
                + "progress INT NOT NULL,"
                + "target INT NOT NULL,"
                + "completed TINYINT(1) NOT NULL,"
                + "claimed TINYINT(1) NOT NULL,"
                + "assigned_at BIGINT NOT NULL,"
                + "completed_at BIGINT NOT NULL,"
                + "expires_at BIGINT NOT NULL,"
                + "last_reset BIGINT NOT NULL,"
                + "PRIMARY KEY (uuid, quest_id)"
                + ")";

        String questMeta = "CREATE TABLE IF NOT EXISTS player_quest_meta ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "last_daily_reset BIGINT NOT NULL,"
                + "last_weekly_reset BIGINT NOT NULL,"
                + "last_monthly_reset BIGINT NOT NULL"
                + ")";

        String questHistory = "CREATE TABLE IF NOT EXISTS player_quest_history ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "entry_value VARCHAR(255) NOT NULL,"
                + "completed_at BIGINT NOT NULL"
                + ")";

        String events = "CREATE TABLE IF NOT EXISTS events ("
                + "event_id VARCHAR(96) NOT NULL PRIMARY KEY,"
                + "last_trigger_time BIGINT NOT NULL"
                + ")";

        String runtime = "CREATE TABLE IF NOT EXISTS event_runtime ("
                + "runtime_key VARCHAR(16) NOT NULL PRIMARY KEY,"
                + "active_event_id VARCHAR(96),"
                + "active_until BIGINT NOT NULL,"
                + "last_global_trigger BIGINT NOT NULL"
                + ")";

        String questDefinitions = "CREATE TABLE IF NOT EXISTS quest_definitions ("
                + "definition_id VARCHAR(96) NOT NULL PRIMARY KEY,"
                + "payload_json LONGTEXT NOT NULL"
                + ")";

        String eventDefinitions = "CREATE TABLE IF NOT EXISTS event_definitions ("
                + "definition_id VARCHAR(96) NOT NULL PRIMARY KEY,"
                + "payload_json LONGTEXT NOT NULL"
                + ")";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps1 = connection.prepareStatement(playerQuests);
             PreparedStatement ps2 = connection.prepareStatement(questMeta);
             PreparedStatement ps3 = connection.prepareStatement(questHistory);
             PreparedStatement ps4 = connection.prepareStatement(events);
             PreparedStatement ps5 = connection.prepareStatement(runtime);
             PreparedStatement ps6 = connection.prepareStatement(questDefinitions);
             PreparedStatement ps7 = connection.prepareStatement(eventDefinitions)) {
            ps1.executeUpdate();
            ps2.executeUpdate();
            ps3.executeUpdate();
            ps4.executeUpdate();
            ps5.executeUpdate();
            ps6.executeUpdate();
            ps7.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed creating MySQL tables: " + ex.getMessage());
        }
    }

    private CompletableFuture<Map<String, Map<String, Object>>> loadDefinitionTable(String table) {
        return async.supply(() -> {
            Map<String, Map<String, Object>> output = new LinkedHashMap<>();
            if (dataSource == null) {
                return output;
            }

            String query = "SELECT definition_id, payload_json FROM " + table;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String id = rs.getString("definition_id");
                    String json = rs.getString("payload_json");
                    Map<String, Object> payload = GSON.fromJson(json, new TypeToken<Map<String, Object>>() { }.getType());
                    if (payload != null) {
                        payload.putIfAbsent("id", id);
                        output.put(id.toLowerCase(), payload);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed loading definitions from " + table + ": " + ex.getMessage());
            }
            return output;
        });
    }

    private CompletableFuture<Void> saveDefinitionTable(String table, Map<String, Map<String, Object>> definitions) {
        return async.run(() -> {
            if (dataSource == null) {
                return;
            }
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table)) {
                        delete.executeUpdate();
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO " + table + "(definition_id, payload_json) VALUES (?,?)"
                    )) {
                        if (definitions != null) {
                            for (Map.Entry<String, Map<String, Object>> entry : definitions.entrySet()) {
                                insert.setString(1, entry.getKey().toLowerCase());
                                insert.setString(2, GSON.toJson(entry.getValue()));
                                insert.addBatch();
                            }
                        }
                        insert.executeBatch();
                    }
                    connection.commit();
                } catch (Exception ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed saving definitions to " + table + ": " + ex.getMessage());
            }
        });
    }

    private void loadMeta(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_daily_reset, last_weekly_reset, last_monthly_reset FROM player_quest_meta WHERE uuid=?"
        )) {
            ps.setString(1, profile.uuid().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                profile.setLastDailyReset(rs.getLong("last_daily_reset"));
                profile.setLastWeeklyReset(rs.getLong("last_weekly_reset"));
                profile.setLastMonthlyReset(rs.getLong("last_monthly_reset"));
            }
        }
    }

    private void loadQuestStates(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT quest_id, quest_type, progress, target, completed, claimed, assigned_at, completed_at, expires_at, last_reset "
                        + "FROM player_quests WHERE uuid=?"
        )) {
            ps.setString(1, profile.uuid().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuestType type;
                try {
                    type = QuestType.valueOf(rs.getString("quest_type"));
                } catch (IllegalArgumentException ex) {
                    type = QuestType.DAILY;
                }
                String questId = rs.getString("quest_id");
                PlayerQuestState state = new PlayerQuestState(
                        questId,
                        type,
                        rs.getInt("progress"),
                        rs.getInt("target"),
                        rs.getBoolean("completed"),
                        rs.getBoolean("claimed"),
                        rs.getLong("assigned_at"),
                        rs.getLong("completed_at"),
                        rs.getLong("expires_at"),
                        rs.getLong("last_reset")
                );
                profile.questStates().put(questId.toLowerCase(), state);
            }
        }
    }

    private void loadHistory(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT entry_value FROM player_quest_history WHERE uuid=? ORDER BY completed_at DESC LIMIT 60"
        )) {
            ps.setString(1, profile.uuid().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                profile.addHistory(rs.getString("entry_value"));
            }
        }
    }

    private void saveMeta(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO player_quest_meta(uuid, last_daily_reset, last_weekly_reset, last_monthly_reset) VALUES (?,?,?,?)"
        )) {
            ps.setString(1, profile.uuid().toString());
            ps.setLong(2, profile.lastDailyReset());
            ps.setLong(3, profile.lastWeeklyReset());
            ps.setLong(4, profile.lastMonthlyReset());
            ps.executeUpdate();
        }
    }

    private void saveQuestStates(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_quests WHERE uuid=?")) {
            delete.setString(1, profile.uuid().toString());
            delete.executeUpdate();
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO player_quests(uuid, quest_id, quest_type, progress, target, completed, claimed, assigned_at, completed_at, expires_at, last_reset) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)"
        )) {
            for (PlayerQuestState state : profile.questStates().values()) {
                insert.setString(1, profile.uuid().toString());
                insert.setString(2, state.questId());
                insert.setString(3, state.questType().name());
                insert.setInt(4, state.progress());
                insert.setInt(5, state.target());
                insert.setBoolean(6, state.completed());
                insert.setBoolean(7, state.claimed());
                insert.setLong(8, state.assignedAt());
                insert.setLong(9, state.completedAt());
                insert.setLong(10, state.expiresAt());
                insert.setLong(11, state.lastReset());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void saveHistory(Connection connection, PlayerQuestProfile profile) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_quest_history WHERE uuid=?")) {
            delete.setString(1, profile.uuid().toString());
            delete.executeUpdate();
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO player_quest_history(uuid, entry_value, completed_at) VALUES (?,?,?)"
        )) {
            long now = System.currentTimeMillis();
            int index = 0;
            for (String entry : profile.history()) {
                insert.setString(1, profile.uuid().toString());
                insert.setString(2, entry);
                insert.setLong(3, now - index++);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
