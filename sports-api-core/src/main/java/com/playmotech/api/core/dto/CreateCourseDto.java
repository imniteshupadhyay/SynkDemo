package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCourseDto {
	@NotEmpty(message = "Title cannot be empty")
	private String title;
	@NotEmpty(message = "description cannot be empty.")
	private String description;
	@NotNull(message = "Coach list cannot be empty")
	private List<String> coachUserIds;
	private String coachUserId;
	@NotNull(message = "level is required.")
	private SkillLevel level;
	private Long totalMaxTrainees;
	private Long minAge;
	private Long maxAge;
	@NotNull(message = "schedule is required.")
	private ScheduleDto schedule;
	@NotNull(message = "sport is required.")
	private Sports sport;
	private String academyId;
	@NotNull(message = "visibility is required.")
	private Visibility visibility;
	private Map<PaymentSchedule, Long> paymentOptions;
	private String ageGroup;
	private Long registrationFee;
	private Set<ScheduleFileDto> scheduleFiles;

}
