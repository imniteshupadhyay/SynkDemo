package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;

import lombok.Data;

@Data
public class AddEditLeadDto {
	private String name;
	private String phoneNumber;
	private String emailId;
	private String leadSourceId;
	private String leadSourceReason;
	private String addressLine1;
	private String addressLine2;
	private Gender gender;
	private Sports sports;
	private AgeCategory ageCategory;
	private LocalDate dob;
}
