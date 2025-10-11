package com.playmotech.api.core.response.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentDetailsViewDao {

	private String receiptId;

	private String currency;

	private BigDecimal amount;

	private String paymentMode;

	private String paymentStatus;

	private String academy;

//	private String branch;

	private String program;

	private String sport;

//	private String paymentInitiatedByUserId;

	private String invoiceGeneratedBy;

	private LocalDateTime createdAt;

	private LocalDateTime transactionTime;

	private String playerName;

	private String ageCategory;

//	private String enrollId;

	private String paymentCategory;

}
