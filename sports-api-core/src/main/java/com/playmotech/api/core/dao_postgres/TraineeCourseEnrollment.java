package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.time.LocalDate;

import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trainee_course_enrollments")
@Data
public class TraineeCourseEnrollment {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@ManyToOne
	@JoinColumn(name = "trainee_user_id", referencedColumnName = "id")
	private UserProfile traineeUserProfile;

	@Column(name = "joining_date")
	private LocalDate joiningDate;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private Status status;

	@Column(name = "payment_schedule")
	@Enumerated(value = EnumType.STRING)
	private PaymentSchedule paymentSchedule;

	@Column(name = "amount")
	private Long amount;

	@Column(name = "discount_amount", columnDefinition = "BIGINT DEFAULT 0")
	private Long discountAmount;

	@Column(name = "dues_on")
	private LocalDate duesOn;

	@Column(name = "final_due_amount", columnDefinition = "BIGINT DEFAULT 0")
	private Long finalDueAmount;

	@Column(name = "use_for_future", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean useForFuture;

}