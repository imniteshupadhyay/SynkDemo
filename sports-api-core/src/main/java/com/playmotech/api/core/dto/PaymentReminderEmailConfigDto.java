package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for email notification template configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReminderEmailConfigDto {

    private String id;

    @NotEmpty(message = "subjectTemplate is required")
    private String subjectTemplate;

    @NotEmpty(message = "messageBody is required")
    private String messageBody;
}
