package com.avertox.questsystem.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class DefinitionLoader {
    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;

    public DefinitionLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public LoadedDefinitions load() {
        ensureDefaults();

        Map<String, Map<String, Object>> quests = new LinkedHashMap<>();
        Map<String, Map<String, Object>> events = new LinkedHashMap<>();

        File definitionDir = new File(plugin.getDataFolder(), "definitions");
        File[] files = definitionDir.listFiles();
        if (files == null) {
            return new LoadedDefinitions(quests, events);
        }

        for (File file : files) {
            String name = file.getName().toLowerCase();
            try {
                if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                    loadYaml(file, quests, events);
                } else if (name.endsWith(".json")) {
                    loadJson(file, quests, events);
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed loading definitions from " + file.getName(), ex);
            }
        }
        return new LoadedDefinitions(quests, events);
    }

    private void ensureDefaults() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        File definitions = new File(plugin.getDataFolder(), "definitions");
        if (!definitions.exists()) {
            definitions.mkdirs();
        }
        copyIfMissing("definitions/quests.yml");
        copyIfMissing("definitions/events.yml");
    }

    private void copyIfMissing(String resourcePath) {
        File out = new File(plugin.getDataFolder(), resourcePath);
        if (!out.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void loadYaml(
            File file,
            Map<String, Map<String, Object>> quests,
            Map<String, Map<String, Object>> events
    ) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection questSection = yaml.getConfigurationSection("quests");
        if (questSection != null) {
            for (String key : questSection.getKeys(false)) {
                ConfigurationSection section = questSection.getConfigurationSection(key);
                if (section != null) {
                    Map<String, Object> values = toMap(section);
                    values.putIfAbsent("id", key);
                    quests.put(key.toLowerCase(), values);
                }
            }
        }

        ConfigurationSection eventSection = yaml.getConfigurationSection("events");
        if (eventSection != null) {
            for (String key : eventSection.getKeys(false)) {
                ConfigurationSection section = eventSection.getConfigurationSection(key);
                if (section != null) {
                    Map<String, Object> values = toMap(section);
                    values.putIfAbsent("id", key);
                    events.put(key.toLowerCase(), values);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadJson(
            File file,
            Map<String, Map<String, Object>> quests,
            Map<String, Map<String, Object>> events
    ) throws IOException {
        String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();

        JsonObject questObject = root.has("quests") && root.get("quests").isJsonObject()
                ? root.getAsJsonObject("quests")
                : null;
        if (questObject != null) {
            for (Map.Entry<String, JsonElement> entry : questObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                Map<String, Object> values = GSON.fromJson(entry.getValue(), Map.class);
                values.putIfAbsent("id", entry.getKey());
                quests.put(entry.getKey().toLowerCase(), values);
            }
        }

        JsonObject eventObject = root.has("events") && root.get("events").isJsonObject()
                ? root.getAsJsonObject("events")
                : null;
        if (eventObject != null) {
            for (Map.Entry<String, JsonElement> entry : eventObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                Map<String, Object> values = GSON.fromJson(entry.getValue(), Map.class);
                values.putIfAbsent("id", entry.getKey());
                events.put(entry.getKey().toLowerCase(), values);
            }
        }
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nestedSection) {
                map.put(key, toMap(nestedSection));
                continue;
            }
            map.put(key, value);
        }
        return map;
    }
}
