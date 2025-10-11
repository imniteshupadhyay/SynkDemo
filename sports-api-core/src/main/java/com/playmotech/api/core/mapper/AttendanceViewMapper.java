package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.response.dao.AttendanceViewDao;
import com.playmotech.api.core.response.dao.CoachAttendanceViewDao;
import com.playmotech.api.core.views.AttendanceView;
import com.playmotech.api.core.views.CoachAttendanceView;

public class AttendanceViewMapper {

	// Method to map a single AttendanceView entity to AttendanceViewDao
	public static AttendanceViewDao mapToDao(AttendanceView attendanceView) {
		AttendanceViewDao attendanceViewDao = new AttendanceViewDao();
		try {
			BeanUtils.copyProperties(attendanceView, attendanceViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return attendanceViewDao;
	}

	// Method to map a single AttendanceView entity to AttendanceViewDao
	public static CoachAttendanceViewDao mapToDao(CoachAttendanceView attendanceView) {
		CoachAttendanceViewDao attendanceViewDao = new CoachAttendanceViewDao();
		try {
			BeanUtils.copyProperties(attendanceView, attendanceViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return attendanceViewDao;
	}

	// Method to map a list of AttendanceView entities to a list of
	// AttendanceViewDao objects
	public static List<AttendanceViewDao> mapListToDaoList(List<AttendanceView> attendanceViews) {
		return attendanceViews.stream().map(AttendanceViewMapper::mapToDao).collect(Collectors.toList());
	}

	public static List<CoachAttendanceViewDao> mapListToCoachDaoList(List<CoachAttendanceView> attendanceViews) {
		return attendanceViews.stream().map(AttendanceViewMapper::mapToDao).collect(Collectors.toList());
	}
}
