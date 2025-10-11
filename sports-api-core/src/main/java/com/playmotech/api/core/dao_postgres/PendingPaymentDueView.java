package com.playmotech.api.core.dao_postgres;

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
@Table(name = "course_enrollment_details_view")
public class PendingPaymentDueView {

	@Id
	@Column(name = "enrollment_id")
	private String enrollmentId;

	@Column(name = "trainee_user_id")
	private String traineeUserId;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "academy_name")
	private String academyName;

	@Column(name = "course_id")
	private String courseId;

	@Column(name = "course_name")
	private String courseName;

	@Column(name = "player_name")
	private String playerName;

	@Column(name = "player_contact_number")
	private String playerContactNumber;

	@Column(name = "player_email_id")
	private String playerEmailId;

	@Column(name = "sport")
	private String sport;

	@Column(name = "payment_schedule")
	private String paymentSchedule;

	@Column(name = "total_course_fee_collected")
	private BigDecimal totalCourseFeeCollected;

	@Column(name = "total_registration_fee_collected")
	private BigDecimal totalRegistrationFeeCollected;

	@Column(name = "pending_course_fee")
	private BigDecimal pendingCourseFee;

	@Column(name = "pending_registration_fee")
	private BigDecimal pendingRegistrationFee;

	@Column(name = "pending_registration_payment_details")
	private String pendingRegistrationPaymentDetails; // JSON as String

	@Column(name = "pending_course_payment_details")
	private String pendingCoursePaymentDetails; // JSON as String

	@Column(name = "upcoming_due_date")
	private LocalDateTime upcomingDueDate;

	@Column(name = "upcoming_amount")
	private BigDecimal upcomingAmount;

	@Column(name = "due_days_count")
	private Integer dueDaysCount;

	@Column(name = "coach_ids")
	private List<String> coachIds;

	@Column(name = "maintainer_ids")
	private List<String> maintainerIds;

	@Column(name = "manager_id")
	private String managerId;

	@Column(name = "domain_url")
	private String domainUrl;

	@Column(name = "age_category")
	private String ageCategory;
}