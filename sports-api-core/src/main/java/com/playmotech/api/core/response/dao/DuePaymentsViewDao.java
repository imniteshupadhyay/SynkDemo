package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class DuePaymentsViewDao {

	// Basic enrollment information
	private String enrollmentId;
	private String traineeUserId;
	private String academyId;
	private String academyName;
	private String courseId;
	private String courseName;
	private String status;

	// User and contact information
	private List<String> coachIds;
	private List<String> maintainerIds;
	private String managerUserId;
	private String domainUrl;
	private String ageCategory;
	private String playerName;
	private String playerContactNumber;
	private String playerEmailId;
	private String sport;

	// Payment schedule information
	private String paymentSchedule;
	private LocalDate duesOn;
	private LocalDate joiningDate;
	private Double discountAmount;
	private Double finalDueAmount;
	private Double currentCourseDue;
	private Double totalCourseDue;
	private Boolean useForFuture;

	// Registration fee details
	private Double registrationTotalDue;
	private Double registrationTotalPending;
	private Double registrationTotalPaid;
	private Double registrationDiscountTotal;
	private Double registrationTotalCovered;

	// Course fee details
	private Double courseTotalDue;
	private Double courseTotalPending;
	private Double courseTotalPaid;
	private Double courseDiscountTotal;
	private Double courseTotalCovered;

	// Payment details (JSON strings)
	private String pendingRegistrationPaymentDetails;
	private String pendingCoursePaymentDetails;

	// Due status information
	private String registrationDueDaysStatus;
	private Integer registrationDueDaysCount;
	private String courseDueDaysStatus;
	private Integer courseDueDaysCount;
	private String paymentStatus;

}
