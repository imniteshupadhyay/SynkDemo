package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class TraineeDetailsDto {
	private String traineeUserId;
	private Status status;
	private UserProfileDto userProfile;
}
