package com.playmotech.api.core.response.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PendingPaymentDueViewDao {

	private String enrollmentId;
	private String playerName;
	private String playerContactNumber;
	private String courseName;
	private String academyName;
	private String sport;
	private String paymentSchedule;
	private BigDecimal totalCourseFeeCollected;
	private BigDecimal totalRegistrationFeeCollected;
	private BigDecimal pendingCourseFee;
	private BigDecimal pendingRegistrationFee;
	private LocalDateTime upcomingDueDate;
	private BigDecimal upcomingAmount;
	private Integer dueDaysCount;
	private String ageCategory;
	private String pendingRegistrationPaymentDetails;
	private String pendingCoursePaymentDetails;
}
