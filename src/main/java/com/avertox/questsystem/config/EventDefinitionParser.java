package com.avertox.questsystem.config;

import com.avertox.questsystem.model.EventEffect;
import com.avertox.questsystem.model.EventEffectType;
import com.avertox.questsystem.model.GlobalEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class EventDefinitionParser {
    private final Logger logger;

    public EventDefinitionParser(Logger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    public GlobalEvent parse(Map<String, Object> section) {
        if (section == null || section.isEmpty()) {
            return null;
        }

        String id = asString(section.get("id"));
        String name = asString(section.get("name"));
        String description = asString(section.get("description"));
        int durationMinutes = (int) asDouble(section.get("duration_minutes"), 30D);
        int scheduleMinutes = (int) asDouble(section.getOrDefault("schedule_minutes", 0), 0D);
        boolean enabled = asBoolean(section.get("enabled"), true);

        if (id.isBlank() || name.isBlank()) {
            logger.warning("Event definition missing required fields: id/name");
            return null;
        }

        List<EventEffect> effects = new ArrayList<>();
        Object effectsObj = section.get("effects");
        if (effectsObj instanceof List<?> list) {
            for (Object obj : list) {
                if (!(obj instanceof Map<?, ?> effectRawMap)) {
                    continue;
                }
                Map<String, Object> effectMap = (Map<String, Object>) effectRawMap;
                EventEffect effect = parseEffect(effectMap, id);
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }

        return new GlobalEvent(id, name, description, Math.max(1, durationMinutes), scheduleMinutes, enabled, effects);
    }

    private EventEffect parseEffect(Map<String, Object> section, String eventId) {
        String typeRaw = asString(section.get("type"));
        EventEffectType type = EventEffectType.from(typeRaw);
        if (type == null) {
            logger.warning("Event " + eventId + " has invalid effect type: " + typeRaw);
            return null;
        }

        double value = asDouble(section.get("value"), 1D);
        if (type == EventEffectType.POTION_EFFECT) {
            String potionRaw = asString(section.get("potion"));
            PotionEffectType potionEffectType = PotionEffectType.getByName(potionRaw.toUpperCase(Locale.ROOT));
            if (potionEffectType == null) {
                logger.warning("Event " + eventId + " potion effect is invalid: " + potionRaw);
                return null;
            }
            int amplifier = (int) asDouble(section.get("amplifier"), 0D);
            return new EventEffect(type, value, potionEffectType, amplifier);
        }

        return new EventEffect(type, value, null, 0);
    }

    private String asString(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private double asDouble(Object raw, double fallback) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean asBoolean(Object raw, boolean fallback) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
