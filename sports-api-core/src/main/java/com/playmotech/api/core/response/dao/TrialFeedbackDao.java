package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TrialFeedbackDao {
	private String id;
	private Integer technicalSkill;
	private Integer fitness;
	private Integer behaviouralSkill;
	private String performanceLevel;
	private String ageGroup;
	private String note;
	private CourseDao course;
	private LocalDateTime insertedOn;
}
