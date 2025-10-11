package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.constants.Sports;

import lombok.Data;

@Data
public class CoachPerformanceReportDto {
	private String id;
	private UserProfileMinDto trainee;
	private String title;
	private String report;
	private UserProfileMinDto coach;
	private AcademyMinDto academy;
	private String createdOn;
	private List<String> mediaUrls;
	private PerformanceReportStatus status;
	private Sports sport;
	private CourseMinDto course;
}
