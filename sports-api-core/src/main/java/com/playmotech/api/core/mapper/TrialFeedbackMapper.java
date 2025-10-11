package com.playmotech.api.core.mapper;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.TrialFeedback;
import com.playmotech.api.core.dao_postgres.TrialFeedback.PerformanceLevel;
import com.playmotech.api.core.dto.TrialFeedbackDto;
import com.playmotech.api.core.response.dao.CourseDao;
import com.playmotech.api.core.response.dao.TrialFeedbackDao;

public class TrialFeedbackMapper {

	public static TrialFeedbackDao mapEntityToDao(TrialFeedback entity) {
		TrialFeedbackDao dao = new TrialFeedbackDao();
		BeanUtils.copyProperties(entity, dao);

		dao.setPerformanceLevel(entity.getPerformanceLevel().name());

		dao.setAgeGroup(entity.getAgeGroup().name());
		if (entity.getCourse() != null) {
			CourseDao course = new CourseDao();
			course.setId(entity.getCourse().getId());
			course.setTitle(entity.getCourse().getTitle());
			dao.setCourse(course);
		}

		return dao;
	}

	public static TrialFeedback mapDtoToEntity(TrialFeedbackDto dto, Trial trail) {
		TrialFeedback entity = new TrialFeedback();
		BeanUtils.copyProperties(dto, entity);

		entity.setPerformanceLevel(PerformanceLevel.valueOf(dto.getPerformanceLevel().toUpperCase()));
		entity.setAgeGroup(AgeCategory.valueOf(dto.getAgeGroup().toUpperCase()));
		entity.setTrial(trail);

		if (dto.getCourseId() != null) {
			Course course = new Course();
			course.setId(dto.getCourseId());
			entity.setCourse(course);
		}

		return entity;
	}

	public static TrialFeedback mapDtoToExistingEntity(TrialFeedbackDto dto, TrialFeedback existing) {
		BeanUtils.copyProperties(dto, existing);

		if (dto.getPerformanceLevel() != null) {
			existing.setPerformanceLevel(PerformanceLevel.valueOf(dto.getPerformanceLevel()));
		}

		if (dto.getCourseId() != null) {
			Course course = new Course();
			course.setId(dto.getCourseId());
			existing.setCourse(course);
		}

		return existing;
	}
}
