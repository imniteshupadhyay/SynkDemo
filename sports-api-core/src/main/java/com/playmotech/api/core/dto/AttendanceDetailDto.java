package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class AttendanceDetailDto {
	private String startTime;
	private String endTime;
	private boolean attendance;
}
