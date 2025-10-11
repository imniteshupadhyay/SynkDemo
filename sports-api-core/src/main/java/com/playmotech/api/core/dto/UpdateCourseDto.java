package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCourseDto {
	private String title;
	private String description;
	private SkillLevel level;
	private Long totalMaxTrainees;
	private Long minAge;
	private Long maxAge;
	private Sports sport;
	private List<String> coachUserIds;
	private Visibility visibility;
	private ScheduleDto schedule;
	private Map<PaymentSchedule, Long> paymentOptions;
	private String ageGroup;
	private Long registrationFee;
	private Set<ScheduleFileDto> scheduleFiles;
}
