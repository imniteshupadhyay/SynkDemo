package com.playmotech.api.core.response.dao;

import lombok.Data;

@Data
public class CourseExportDao {
	private String courseId;

	private String title;

	private String description;

	private String sport;

	private String ageCategory;

	private String domainUrl;

	private String scheduleType;

	private String startDate;

	private String endDate;

	private String createdOn;

	private String scheduleFileName;

	private String scheduleFileUrl;

}
