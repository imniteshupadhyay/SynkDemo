package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.ScheduleType;

import lombok.Data;

@Data
public class TraineePaymentDetailsDto {
	private String enrollmentId;
	private String courseId;
	private String courseName;
	private ScheduleType courseScheduleType;
	private Boolean isDuePaymentPending;
	private Long dueAmount;
	private Currency currency;
	private Map<String, PaymentDto> paymentHistory;
	private Boolean isInProgress;
}
