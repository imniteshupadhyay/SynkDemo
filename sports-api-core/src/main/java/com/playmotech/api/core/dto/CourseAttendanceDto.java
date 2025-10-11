package com.playmotech.api.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CourseAttendanceDto {
	private String courseId;
	private String courseIconUrl;
	private String courseTitle;
	private String startDate;
	private String endDate;
	private Map<String, Boolean> attendance;
}
