package com.avertox.questsystem.config;

import java.util.Map;

public record LoadedDefinitions(
        Map<String, Map<String, Object>> questSections,
        Map<String, Map<String, Object>> eventSections
) {
}
