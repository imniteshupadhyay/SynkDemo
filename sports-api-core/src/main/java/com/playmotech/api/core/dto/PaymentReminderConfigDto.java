package com.playmotech.api.core.dto;

import java.time.LocalTime;
import java.util.Set;

import com.playmotech.api.core.constants.PaymentReminderType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for payment reminder configuration
 * Supports multiple academies per configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReminderConfigDto {
    private String id;

    @NotEmpty(message = "academyIds must not be empty")
    private Set<String> academyIds;

    @NotNull(message = "reminderType is required")
    private PaymentReminderType reminderType;

    @NotNull(message = "sendPushNotification is required")
    private Boolean sendPushNotification;

    @NotNull(message = "sendEmailNotification is required")
    private Boolean sendEmailNotification;

    private PaymentReminderMobileConfigDto mobileConfig;

    private PaymentReminderEmailConfigDto emailConfig;

    @NotNull(message = "notificationTime is required")
    private LocalTime notificationTime;

    @NotNull(message = "frequencyDays is required")
    private Integer frequencyDays;

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    private String createdAt;
    private String updatedAt;
}
