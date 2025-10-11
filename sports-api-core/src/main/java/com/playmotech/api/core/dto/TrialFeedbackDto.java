package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class TrialFeedbackDto {
	private String id;
	private Integer technicalSkill;
	private Integer fitness;
	private Integer behaviouralSkill;
	private String performanceLevel;
	private String ageGroup;
	private String note;
	private String trialId;
	private String courseId;
	private String status;
}
