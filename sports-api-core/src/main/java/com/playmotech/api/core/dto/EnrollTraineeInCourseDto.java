package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.Status;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollTraineeInCourseDto {

	private String id;

	private String programId;
	private Status programStatus;

	@NotNull(message = "traineeUserId cannot be null")
	@NotEmpty(message = "traineeUserId cannot be empty")
	private String traineeUserId;
	@NotNull(message = "paymentSchedule cannot be null")
	private PaymentSchedule paymentSchedule;
	@NotNull(message = "amount cannot be null")
	private Long amount;
	@NotNull(message = "joining date cannot be null")
	private LocalDate joiningDate;
	@NotNull(message = "due date cannot be null")
	private LocalDate dueDate;

	private Long overallDiscountAmount;

	private Long scheduleDiscountAmount;

	private Boolean useDiscountForFuture;

	private Boolean updateDues;

}
