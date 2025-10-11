package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;

import lombok.Data;

@Data
public class PaymentDetailsDto {
	private Map<String, Long> futurePayments;
	private Map<String, Long> pendingPayments;
	private Boolean isInProgress;
	private Long nextDueAmount;
	private String enrollmentId;
	private Currency currency;
	private PaymentSchedule paymentSchedule;
	private PaymentCategory paymentCategory;
	private Map<String, PaymentDto> paymentHistory;
	private Boolean isDue;
	private Long pendingAmount;
	private String nextDueAt;
	private Boolean useForFuture;
	private Long totalPayableAmount;
	private Long totalPastDues; // sum of all values in pendingPayments
	private Long currentDue; // pendingAmount - totalPastDues
	private Long originalAmount;
	private Long discountAmount;
	private Long advanceCredit;
	private Long adjustments;
}