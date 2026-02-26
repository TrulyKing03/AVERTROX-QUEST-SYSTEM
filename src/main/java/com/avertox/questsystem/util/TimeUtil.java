package com.avertox.questsystem.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static boolean needsDailyReset(long lastResetEpochMs, int intervalHours) {
        if (lastResetEpochMs <= 0L) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - lastResetEpochMs;
        return elapsed >= TimeUnit.HOURS.toMillis(Math.max(1, intervalHours));
    }

    public static boolean needsWeeklyReset(long lastResetEpochMs, DayOfWeek resetDay, ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        if (now.getDayOfWeek() != resetDay) {
            return false;
        }
        if (lastResetEpochMs <= 0L) {
            return true;
        }
        LocalDate lastDate = Instant.ofEpochMilli(lastResetEpochMs).atZone(zoneId).toLocalDate();
        return !lastDate.equals(now.toLocalDate());
    }

    public static boolean needsMonthlyReset(long lastResetEpochMs, int resetDays) {
        if (lastResetEpochMs <= 0L) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - lastResetEpochMs;
        return elapsed >= TimeUnit.DAYS.toMillis(Math.max(1, resetDays));
    }

    public static long millisUntil(long targetEpochMs) {
        return Math.max(0L, targetEpochMs - System.currentTimeMillis());
    }

    public static String shortDuration(long millis) {
        if (millis <= 0L) {
            return "0m";
        }
        long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long days = totalMinutes / (60L * 24L);
        long hours = (totalMinutes / 60L) % 24L;
        long minutes = totalMinutes % 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
