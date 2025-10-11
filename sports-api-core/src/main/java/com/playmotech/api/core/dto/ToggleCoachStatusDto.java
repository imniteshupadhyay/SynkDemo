package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class ToggleCoachStatusDto {
	private List<String> coachUserIds;
	private Status status;
}
