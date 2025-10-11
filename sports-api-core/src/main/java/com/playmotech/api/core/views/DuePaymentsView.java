package com.playmotech.api.core.views;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "trainee_payment_status_view")
public class DuePaymentsView {

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

	@Column(name = "coach_ids")
	private List<String> coachIds;

	@Column(name = "maintainer_ids")
	private List<String> maintainerIds;

	@Column(name = "manager_user_id")
	private String managerUserId;

	@Column(name = "domain_url")
	private String domainUrl;

	@Column(name = "age_category")
	private String ageCategory;

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

	@Column(name = "dues_on")
	private LocalDate duesOn;

	@Column(name = "joining_date")
	private LocalDate joiningDate;

	@Column(name = "status")
	private String status;

	@Column(name = "discount_amount")
	private Double discountAmount;

	@Column(name = "final_due_amount")
	private Double finalDueAmount;

	@Column(name = "current_course_due")
	private Double currentCourseDue;

	@Column(name = "total_course_due")
	private Double totalCourseDue;

	@Column(name = "use_for_future")
	private Boolean useForFuture;

	// Registration fee fields
	@Column(name = "registration_total_due")
	private Double registrationTotalDue;

	@Column(name = "registration_total_pending")
	private Double registrationTotalPending;

	@Column(name = "registration_total_paid")
	private Double registrationTotalPaid;

	@Column(name = "registration_discount_total")
	private Double registrationDiscountTotal;

	@Column(name = "registration_total_covered")
	private Double registrationTotalCovered;

	// Course fee fields
	@Column(name = "course_total_due")
	private Double courseTotalDue;

	@Column(name = "course_total_pending")
	private Double courseTotalPending;

	@Column(name = "course_total_paid")
	private Double courseTotalPaid;

	@Column(name = "course_discount_total")
	private Double courseDiscountTotal;

	@Column(name = "course_total_covered")
	private Double courseTotalCovered;

	// Payment details as JSON strings
	@Column(name = "pending_registration_payment_details")
	private String pendingRegistrationPaymentDetails;

	@Column(name = "pending_course_payment_details")
	private String pendingCoursePaymentDetails;

	// Paid payment details as JSON strings
	@Column(name = "paid_registration_payment_details")
	private String paidRegistrationPaymentDetails;

	@Column(name = "paid_course_payment_details")
	private String paidCoursePaymentDetails;

	// Due status fields
	@Column(name = "registration_due_days_status")
	private String registrationDueDaysStatus;

	@Column(name = "registration_due_days_count")
	private Integer registrationDueDaysCount;

	@Column(name = "course_due_days_status")
	private String courseDueDaysStatus;

	@Column(name = "course_due_days_count")
	private Integer courseDueDaysCount;

	@Column(name = "payment_status")
	private String paymentStatus;
}