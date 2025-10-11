package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.playmotech.api.core.constants.PaymentSchedule;

import lombok.Data;

@Data
public class TraineeCourseEnrollmentDto {
	private String id;
	private String traineeUserId;
	private PaymentSchedule paymentSchedule;
	private UserProfileDto userProfile;
	private Long amount;
	private LocalDate joiningDate;
	private LocalDate dueDate;
	private Long discountAmount;
	private Boolean useForFuture;
	private LocalDate duesOn;
	private Long finalDueAmount;

}
