package com.playmotech.api.core.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;

import lombok.Data;

@Data
public class TrialDto {
	private String id;
	private String name;
	private String address;
	private String leadId;
	private LocalDate trailDate;
	private LocalDate dob;
	private LocalTime trailTime;
	private String gender;
	private TrialStatus status;
	private String sports;
	private String email;
	private String phone;
	private String coachId;
	private String academyId;
	private boolean completed;
}
