package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for mobile (push) notification template configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReminderMobileConfigDto {

    private String id;

    @NotEmpty(message = "titleTemplate is required")
    private String titleTemplate;

    @NotEmpty(message = "messageTemplate is required")
    private String messageTemplate;
}
