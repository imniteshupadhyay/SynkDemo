package com.playmotech.api.core.services;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface AttendanceService {

	ServiceResponse getAttendanceList(GenericFilter filter);

	ServiceResponse getCoachAttendanceList(GenericFilter filter);

	ServiceResponse getAttendanceDetails(Timestamp startDateLocal, Timestamp endDateLocal, String domainUrl,
			List<String> academyIds, List<String> courseIds, List<String> sports, List<String> ageCategories);

	ServiceResponse getCourseEnrollmentDetails(Timestamp startDateLocal, Timestamp endDateLocal, String domainUrl,
			List<String> academyIds, List<String> courseIds, List<String> sports, List<String> ageCategories);

	ServiceResponse getAttendanceTrend(Timestamp startDateLocal, Timestamp endDateLocal, String domainUrl,
			List<String> academyIds, List<String> sports, List<String> courseIds, List<String> ageCategories);

}
