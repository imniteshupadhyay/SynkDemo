package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.constants.Sports;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitCoachPerformanceReportRequestDto {
	private String academyId;
	@NotNull(message = "title is required.")
	@NotEmpty(message = "title is required.")
	private String title;
	private String courseId;
	private String report;
	@NotNull(message = "status is required.")
	private PerformanceReportStatus status;
	// @NotNull(message = "sport is required.")
	private Sports sport;
}
