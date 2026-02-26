package com.avertox.questsystem.model;

public class PlayerQuestState {
    private final String questId;
    private QuestType questType;
    private int progress;
    private int target;
    private boolean completed;
    private boolean claimed;
    private long assignedAt;
    private long completedAt;
    private long expiresAt;
    private long lastReset;

    public PlayerQuestState(
            String questId,
            QuestType questType,
            int progress,
            int target,
            boolean completed,
            boolean claimed,
            long assignedAt,
            long completedAt,
            long expiresAt,
            long lastReset
    ) {
        this.questId = questId;
        this.questType = questType;
        this.progress = Math.max(0, progress);
        this.target = Math.max(1, target);
        this.completed = completed;
        this.claimed = claimed;
        this.assignedAt = assignedAt;
        this.completedAt = completedAt;
        this.expiresAt = expiresAt;
        this.lastReset = lastReset;
    }

    public static PlayerQuestState createFresh(String questId, QuestType questType, int target, long expiresAt, long resetAt) {
        long now = System.currentTimeMillis();
        return new PlayerQuestState(questId, questType, 0, target, false, false, now, 0L, expiresAt, resetAt);
    }

    public String questId() {
        return questId;
    }

    public QuestType questType() {
        return questType;
    }

    public void setQuestType(QuestType questType) {
        this.questType = questType;
    }

    public int progress() {
        return progress;
    }

    public int target() {
        return target;
    }

    public void setTarget(int target) {
        this.target = Math.max(1, target);
        if (progress >= this.target && !completed) {
            completed = true;
            completedAt = System.currentTimeMillis();
        }
    }

    public boolean completed() {
        return completed;
    }

    public boolean claimed() {
        return claimed;
    }

    public long assignedAt() {
        return assignedAt;
    }

    public long completedAt() {
        return completedAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long lastReset() {
        return lastReset;
    }

    public void setLastReset(long lastReset) {
        this.lastReset = lastReset;
    }

    public int remaining() {
        return Math.max(0, target - progress);
    }

    public double progressPercent() {
        return Math.min(100D, (progress * 100D) / target);
    }

    public boolean isExpired(long now) {
        return expiresAt > 0L && now >= expiresAt;
    }

    public int increment(int amount) {
        if (completed || claimed) {
            return progress;
        }
        progress = Math.min(target, progress + Math.max(0, amount));
        if (progress >= target) {
            completed = true;
            completedAt = System.currentTimeMillis();
        }
        return progress;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public void forceProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public void reset(int target, long expiresAt, long resetAt) {
        this.progress = 0;
        this.target = Math.max(1, target);
        this.completed = false;
        this.claimed = false;
        this.assignedAt = System.currentTimeMillis();
        this.completedAt = 0L;
        this.expiresAt = expiresAt;
        this.lastReset = resetAt;
    }
}
