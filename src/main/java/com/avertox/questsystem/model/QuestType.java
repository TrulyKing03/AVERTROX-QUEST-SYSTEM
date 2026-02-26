package com.avertox.questsystem.model;

public enum QuestType {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String display;

    QuestType(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
