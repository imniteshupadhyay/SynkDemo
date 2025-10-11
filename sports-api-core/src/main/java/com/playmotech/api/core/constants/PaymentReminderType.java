package com.playmotech.api.core.constants;

/**
 * Types of payment reminders that can be sent to users
 */
public enum PaymentReminderType {
    /**
     * Upcoming payment reminder for payments that are due soon but not yet overdue
     */
    UPCOMING,

    /**
     * Overdue payment reminder for payments that are past their due date
     */
    OVERDUE
}
