package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.response.dao.CourseExportDao;
import com.playmotech.api.core.response.dao.ProgramDao;
import com.playmotech.api.core.views.CourseDetailsView;

public class ProgramMapper {

	// Method to map a single Course entity to ProgramDao
	public static ProgramDao mapToDao(Course course, String userId) {
		ProgramDao programDao = new ProgramDao();
		try {
			boolean isValidCourse = userId == null || course.getCourseCoachMappings().stream()
					.anyMatch(mapping -> mapping.getCoachUserProfile() != null
							&& userId.equals(mapping.getCoachUserProfile().getId().toString()));

			if (isValidCourse) {
				BeanUtils.copyProperties(course, programDao);
				if (course.getAgeGroup() != null) {
					programDao.setAgeCategory(AgeCategory.valueOf(course.getAgeGroup()));
				} else {
					programDao.setAgeCategory(null);
				}

				return programDao;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null; // Return null if the course doesn't meet criteria
	}

	// Method to map a list of Course entities to a list of ProgramDao objects
	public static List<ProgramDao> mapListToDaoList(List<Course> courses, String userId) {
		return courses.stream().map(course -> mapToDao(course, userId)).filter(programDao -> programDao != null) // Remove
																													// null
																													// values
				.collect(Collectors.toList());
	}

	// Method to map a list of AttendanceView entities to a list of
	// AttendanceViewDao objects
	public static CourseExportDao mapToExportDao(CourseDetailsView entity) {
		CourseExportDao dao = new CourseExportDao();
		BeanUtils.copyProperties(entity, dao);

		if (entity.getScheduleFile() != null) {
			dao.setScheduleFileName(entity.getScheduleFile().getFileName());
			dao.setScheduleFileUrl(entity.getScheduleFile().getFileUrl());
		}
		return dao;
	}
}
