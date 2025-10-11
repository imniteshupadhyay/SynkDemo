package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class ToggleTraineeStatusDto {
	private List<String> traineeUserIds;
	private Status status;
}
