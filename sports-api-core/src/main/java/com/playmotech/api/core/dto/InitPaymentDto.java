package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitPaymentDto {

	private String id;
	@NotNull(message = "Amount cannot be empty.")
	private Long amount;
	@NotNull(message = "Currency cannot be empty.")
	private Currency currency;
	@NotNull(message = "Payment Schedule cannot be empty.")
	private PaymentSchedule paymentSchedule;
	@NotEmpty(message = "Payment Installment Date cannot be empty.")
	private String paymentInstallmentDate;
	@NotEmpty(message = "Payment Mode cannot be empty.")
	private String paymentMode;
	// TODO: need to remove comment (Uncommented)
	@NotNull(message = "Payment Category cannot be empty.")
	private PaymentCategory paymentCategory;
}