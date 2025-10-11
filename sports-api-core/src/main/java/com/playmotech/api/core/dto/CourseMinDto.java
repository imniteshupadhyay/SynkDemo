package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.SkillLevel;

import lombok.Data;

@Data
public class CourseMinDto {
	private String id;
	private String title;
	private String description;
	private SkillLevel level;
	private String iconUrl;
}
