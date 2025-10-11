package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.Currency;

import lombok.Data;

@Data
public class CoursePaymentDetailsDto {
	private String enrollmentId;
	private String traineeUserId;
	private Map<String, PaymentDto> paymentHistory;
	private UserProfileMinDto userProfile;
	private Long dueAmount;
	private Currency currency;
	private Boolean isDuePending;
	private Boolean isInProgress;
}
