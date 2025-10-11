package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;

import lombok.Data;

@Data
public class TrialDao {
	private String id;
	private String name;
	private String address;
	private LocalDate trailDate;
	private LocalDate dob;
	private LocalTime trailTime;
	private Gender gender;
	private TrialStatus status;
	private Sports sports;
	private String email;
	private String phone;
	private String reason;
	private List<TrialFeedbackDao> feedback;
	private UserProfileDao coach;
	private AcademyDao academy;
	private UserProfileDao createdBy;
	private UserProfileDao updatedBy;
	private boolean completed;
	private LocalDateTime insertedOn;
	private LocalDateTime updatedOn;
}
