package com.playmotech.api.core.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PaymentReminderDto {
	@NotEmpty(message = "traineeUserId is required")
	private String traineeUserId;
	@NotEmpty(message = "amount is required")
	private String amount;
	@NotEmpty(message = "academyId is required")
	private String academyId;

	/**
	 * Additional parameters for customizing notifications
	 * e.g. reminderType, dueDaysCount, etc.
	 */
	private Map<String, String> extraParams;
}
