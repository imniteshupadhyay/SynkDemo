package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.dao_postgres.UserProfile;

import lombok.Data;

@Data
public class ProgramPlayerEnrollmentDto {
	private String fullName;
	private String phoneNumber;
	private Long feeAmount;
	private PaymentSchedule paymentSchedule;
	private LocalDate joiningDate;
	private LocalDate nextDueDate;
	@JsonIgnore
	private UserProfile userProfile;
}
