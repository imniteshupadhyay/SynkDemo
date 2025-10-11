package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.dto.AttendanceDetailDto;
import com.playmotech.api.core.dto.CourseAttendanceDto;
import com.playmotech.api.core.dto.TraineeAttendanceDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;

public interface IAttendanceService {
	// void markAttendance(String academyId, String courseId, String date,
	// Map<String, Boolean> attendance);
	ServiceResponse markAttendance(String academyId, String courseId, String date,
			Map<String, AttendanceDetailDto> attendance);

	ServiceResponse markAttendanceV2(String academyId, String courseId, String date,
			Map<String, AttendanceDetailDto> attendance);

	List<TraineeAttendanceDto> getAttendance(String academyId, String courseId, String date) throws ResourceException;

	List<CourseAttendanceDto> getAttendanceByTrainee(String academyId, String traineeUserId) throws ResourceException;

	List<CourseAttendanceDto> getAttendanceBetweenDates(String academyId, String startDate, String endDate)
			throws ResourceException;
}
