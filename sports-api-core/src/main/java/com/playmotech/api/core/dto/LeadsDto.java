package com.playmotech.api.core.dto;

import java.sql.Timestamp;
import java.time.LocalDate;

import com.google.auto.value.AutoValue.Builder;
import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.LeadStatus;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.LeadSource;

import lombok.Data;

@Data
@Builder
public class LeadsDto {
	private String id;
	private String name;
	private String phoneNumber;
	private String emailId;
	private Gender gender;
	private Sports sports;
	private AgeCategory ageCategory;
	private LeadSource leadSource;
	private String leadSourceReason;
	private Timestamp createdOn;
	private String assignedCoach;
	private String assignedAcademy;
	private LeadStatus leadStatus;
	private String leadAssignedBy;
	private String addressLine1;
	private String addressLine2;
	private String trialStatus;
	private LocalDate dob;
}
