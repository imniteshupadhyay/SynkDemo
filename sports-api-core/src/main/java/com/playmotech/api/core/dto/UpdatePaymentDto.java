package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentStatus;

import lombok.Data;

@Data
public class UpdatePaymentDto {
	private String externalTransactionId;
	private String paymentMode;
	private String transactionTime;
	private PaymentStatus paymentStatus;
	private Map<String, String> extraArgs;
	// @NotNull(message = "Payment Category cannot be empty.")
	private PaymentCategory paymentCategory;
}