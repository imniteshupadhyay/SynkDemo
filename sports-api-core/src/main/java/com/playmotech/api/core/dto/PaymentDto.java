package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.PaymentStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentDto {
	private String id;
	@NotEmpty(message = "External Transaction Id cannot be empty.")
	private String externalTransactionId;
	@NotEmpty(message = "Payment Mode cannot be empty.")
	private String paymentMode;
	@NotEmpty(message = "Transaction Time cannot be empty.")
	private String transactionTime;
	@NotNull(message = "Payment Status cannot be empty.")
	private PaymentStatus paymentStatus;
	@NotNull(message = "Amount cannot be empty.")
	private Long amount;
	@NotNull(message = "Currency cannot be empty.")
	private Currency currency;
	@NotNull(message = "Payment Schedule cannot be empty.")
	private PaymentSchedule paymentSchedule;
	@NotEmpty(message = "Payment Installment Date cannot be empty.")
	private String paymentInstallmentDate;
	private Map<String, String> extraArgs;
	private String receiptId;
	private String receiptUrl;
}