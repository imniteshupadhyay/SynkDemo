package com.playmotech.api.core.constants;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Represents time periods for filtering leaderboard data
 */
public enum TimePeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME;

    /**
     * Get the start date for the time period relative to current time
     *
     * @return LocalDateTime representing the start of the period
     */
    public LocalDateTime getStartDate() {
        LocalDateTime now = LocalDateTime.now();
        switch (this) {
            case WEEKLY:
                return now.minusWeeks(1);
            case MONTHLY:
                return now.minusMonths(1);
            case YEARLY:
                return now.minusYears(1);
            case ALL_TIME:
            default:
                return LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC); // Beginning of epoch time
        }
    }

    /**
     * Get TimePeriod enum from string value (case-insensitive)
     *
     * @param value String representation of time period
     * @return TimePeriod enum or ALL_TIME as default
     */
    public static TimePeriod fromString(String value) {
        if (value == null) {
            return ALL_TIME;
        }
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL_TIME; // Default if invalid value
        }
    }
}
