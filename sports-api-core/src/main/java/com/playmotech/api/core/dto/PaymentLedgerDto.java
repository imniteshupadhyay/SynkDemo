package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.dao_postgres.PaymentLedger.LedgerType;
import com.playmotech.api.core.dao_postgres.PaymentLedger.PaymentEntryStatus;
import com.playmotech.api.core.dao_postgres.PaymentLedger.PaymentEntryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLedgerDto {

	private Long id;
	private String userId;
	private String enrollmentId;
	private String academyId;
	private String programId;
	private String paymentId;
	private LocalDate effectiveDate;
	private LocalDate paymentDate;

	private LedgerType ledgerType;
	private PaymentEntryType entryType;
	private PaymentEntryStatus entryStatus;
	private PaymentCategory category;
	private Double amount;
	private Double remainingAmount;
	private Boolean isReversal;
	private Long parentEntryId;
	private Boolean isLocked;
	private String notes;
	private String description;
	private String createdById;
	private String updatedById;
	private String approvedById;
}
