package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentSchedule;

import lombok.Data;

@Data
public class UpdateScheduleDto {
	private String startDate;
	private String endDate;
	private String startTime;
	private String endTime;
	private Long amount;
	private Currency currency;
	private List<String> rulesAndRegulations;
	private Map<PaymentSchedule, Long> paymentOptions;
	private String timezone;
}
