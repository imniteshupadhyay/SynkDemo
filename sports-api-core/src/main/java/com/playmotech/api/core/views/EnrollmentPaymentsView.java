package com.playmotech.api.core.views;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "enrollment_payments_view")
public class EnrollmentPaymentsView {

	@Id
	@Column(name = "id")
	private String enrollmentPaymentId;

	@Column(name = "enrollment_id")
	private String enrollmentId;

	@Column(name = "trainee_user_id")
	private String traineeUserId;

	@Column(name = "course_id")
	private String courseId;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "branch_id")
	private String branchId;

	@Column(name = "sport")
	private String sport;

	@Column(name = "ageCategory")
	private String ageCategory;

	@Column(name = "program_min_age")
	private Integer programMinAge;

	@Column(name = "program_max_age")
	private Integer programMaxAge;

	@Column(name = "total_expected_payment", nullable = false)
	private BigDecimal totalExpectedPayment;

	@Column(name = "total_paid", nullable = false)
	private BigDecimal totalPaid;

	@Column(name = "pending_amount", nullable = false)
	private BigDecimal pendingAmount;

	@Column(name = "payment_dates", columnDefinition = "json")
	private String paymentDates;

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

	@Column(name = "created_at")
	private Timestamp createdAt;
}
