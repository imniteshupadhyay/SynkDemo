package com.playmotech.api.core.views;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "payment_details_view")
public class PaymentDetailsView {

	@Id
	@Column(name = "id")
	private String id;

	@Column(name = "receipt_id")
	private String receiptId;

	@Column(name = "receipt_url")
	private String receiptUrl;

	@Column(name = "currency")
	private String currency;

	@Column(name = "amount")
	private BigDecimal amount;

	@Column(name = "payment_mode")
	private String paymentMode;

	@Column(name = "payment_status")
	private String paymentStatus;

	@Column(name = "academy")
	private String academy;

	@Column(name = "branch")
	private String branch;

	@Column(name = "program")
	private String program;

	@Column(name = "sport")
	private String sport;

	@Column(name = "payment_initiated_by_user_id")
	private String paymentInitiatedByUserId;

	@Column(name = "invoice_generated_by")
	private String invoiceGeneratedBy;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "transaction_time")
	private LocalDateTime transactionTime;

	@Column(name = "player_id")
	private String playerId;

	@Column(name = "player_name")
	private String playerName;

	@Column(name = "program_min_age")
	private Integer programMinAge;

	@Column(name = "program_max_age")
	private Integer programMaxAge;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "branch_id")
	private String branchId;

	@Column(name = "program_id")
	private String programId;

	@Column(name = "age_category")
	private String ageCategory;

	@Column(name = "enroll_id")
	private String enrollId;

	@Column(name = "coach_ids")
	private List<String> coachIds;

	@Column(name = "maintainer_ids")
	private List<String> maintainerIds;

	@Column(name = "manager_id")
	private String managerId;

	@Column(name = "domain_url")
	private String domainUrl;

	@Column(name = "payment_category")
	private String paymentCategory;

}
