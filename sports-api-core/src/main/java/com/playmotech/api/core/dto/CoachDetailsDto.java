package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class CoachDetailsDto {
	private String coachUserId;
	private Status status;
	private String designation;
	private Integer experienceInMonths;
	private UserProfileDto userProfile;
	private Long roleId;
}
