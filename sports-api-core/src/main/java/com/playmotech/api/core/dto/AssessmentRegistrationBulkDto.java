package com.playmotech.api.core.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class AssessmentRegistrationBulkDto {

	private String assessmentId;
	private LocalDate registrationDate;

	private List<AssessmentRegistrationDto> players;
}
