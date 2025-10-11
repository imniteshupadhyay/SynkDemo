package com.playmotech.api.core.response.dao;

import java.time.LocalDate;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.dao_postgres.PaymentLedger.LedgerType;
import com.playmotech.api.core.dao_postgres.PaymentLedger.PaymentEntryStatus;

import lombok.Data;

@Data
public class PaymentLedgerDao {

	private Long id;
	private String userId;
	private String enrollmentId;
	private String academyId;
	private String courseId;
	private LocalDate effectiveDate;
	private LedgerType ledgerType;
	private PaymentEntryStatus entryStatus;
	private PaymentCategory category;
	private Double amount;
	private Double remainingAmount;
	private String notes;
	private String description;

	// Additional fields for easier access to related entity names/details
	private String userName;
	private String academyName;
	private String courseName;

}
