package com.playmotech.api.core.dao;

import java.sql.Timestamp;

import com.google.auto.value.AutoValue.Builder;

import lombok.Data;

@Data
@Builder
public class LeadsDao {
	private String id;
	private String name;
	private String phoneNumber;
	private String emailId;
	private String gender;
	private String sports;
	private String ageCategory;
	private String leadSource;
	private String leadSourceReason;
	private Timestamp createdOn;
	private String assignedCoach;
	private String assignedAcademy;
	private String status;
	private String leadAssignedBy;
	private String addressLine1;
	private String addressLine2;
	private String trialStatus;
}
