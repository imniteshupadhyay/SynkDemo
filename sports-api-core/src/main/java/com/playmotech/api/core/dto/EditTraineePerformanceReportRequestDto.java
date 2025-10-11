package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.PerformanceReportStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EditTraineePerformanceReportRequestDto {
	@NotNull(message = "report is required.")
	private String report;
	@NotNull(message = "title is required.")
	@NotEmpty(message = "title is required.")
	private String title;
	@NotNull(message = "status is required.")
	private PerformanceReportStatus status;
}
