package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;

import lombok.Data;

@Data
public class CourseDto {
	private String id;
	private String title;
	private String description;
	private String coachUserId;
	private SkillLevel level;
	private Long totalMaxTrainees;
	private Long minAge;
	private Long maxAge;
	private ScheduleDto schedule;
	private String iconUrl;
	private Sports sport;
	private String academyId;
	private AcademyMinDto academy;
	private Visibility visibility;
	private List<UserProfileMinDto> coaches;
	private List<String> rulesAndRegulations;
	private Map<PaymentSchedule, Long> paymentOptions;
	private String ageGroup;
	private Long registrationFee;
	private Set<ScheduleFileDto> scheduleFiles;
}
