package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.VideoAnalysis;
import com.playmotech.api.core.dao_postgres.VideoAnalytics;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dto.VideoAnalyzerDto;
import com.playmotech.api.core.response.dao.AcademyDao;
import com.playmotech.api.core.response.dao.CourseDao;
import com.playmotech.api.core.response.dao.UserProfileDao;
import com.playmotech.api.core.response.dao.VideoAnalysisDao;
import com.playmotech.api.core.response.dao.VideoAnalyticsDao;
import com.playmotech.api.core.response.dao.VideoAnalyzerDao;

public class VideoAnalyzerMapper {

	public static VideoAnalyzerDao mapEntityToDao(VideoAnalyzer entity) {

		VideoAnalyzerDao dao = new VideoAnalyzerDao();
		// Copy simple properties
		BeanUtils.copyProperties(entity, dao);

		// Handle relationships
		if (entity.getPlayer() != null) {
			UserProfileDao player = new UserProfileDao();
			player.setId(entity.getPlayer().getId());
			player.setUsername(entity.getPlayer().getUsername());
			dao.setPlayer(player);
		}
		if (entity.getCreatedBy() != null) {
			UserProfileDao coach = new UserProfileDao();
			coach.setId(entity.getCreatedBy().getId());
			coach.setUsername(entity.getCreatedBy().getUsername());
			dao.setCreatedBy(coach);
		}
		if (entity.getCourse() != null) {
			CourseDao course = new CourseDao();
			course.setId(entity.getCourse().getId());
			course.setTitle(entity.getCourse().getTitle());
			dao.setCourse(course);
		}

		if (entity.getAcademy() != null) {
			AcademyDao academyDao = new AcademyDao();
			academyDao.setId(entity.getAcademy().getId());
			academyDao.setName(entity.getAcademy().getName());
			dao.setAcademy(academyDao);
		}

		if (entity.getAnalysis() != null) {
			List<VideoAnalysisDao> analysisList = entity.getAnalysis().stream().map(analysis -> {
				return VideoAnalysisMapper.mapEntityToDao(analysis);
			}).collect(Collectors.toList());
			dao.setAnalysis(analysisList);
		}

		if (entity.getAnalytics() != null) {
			List<VideoAnalyticsDao> analyticsList = entity.getAnalytics().stream().map(analysis -> {
				return mapEntityToAnalyticsDao(analysis);
			}).collect(Collectors.toList());
			dao.setAnalytics(analyticsList);
		}

		Optional<VideoAnalytics> firstAnalytics = entity.getAnalytics() == null ? Optional.empty()
				: entity.getAnalytics().stream().findFirst();

		if (firstAnalytics.isPresent()) {
			VideoAnalytics analytics = firstAnalytics.get();
			dao.setAnalyticsSource(analytics.getAnalysisSource());
			dao.setAnalyticsStatus(analytics.getStatus() != null ? analytics.getStatus() : AnalysisStatus.PENDING);
		} else {
			dao.setAnalyticsSource(null);
			dao.setAnalyticsStatus(AnalysisStatus.PENDING);
		}

		return dao;
	}

	private static VideoAnalyticsDao mapEntityToAnalyticsDao(VideoAnalytics analysis) {
		VideoAnalyticsDao dao = new VideoAnalyticsDao();
		BeanUtils.copyProperties(analysis, dao);
		return dao;
	}

	public static VideoAnalyzer mapDtoToEntity(VideoAnalyzerDto dto) {
		VideoAnalyzer entity = new VideoAnalyzer();
		// Copy simple properties
		BeanUtils.copyProperties(dto, entity);
		entity.setDeleted(false);

		// If id is null in dto, manually set the id (backward compatibility)
		if (!StringUtils.hasText(dto.getId())) {
			entity.setId(UUID.randomUUID().toString());
		}

		// Handle relationships

		// Player relationship mapping
		if (dto.getPlayerId() != null) {
			UserProfile userProfile = new UserProfile();
			userProfile.setId(dto.getPlayerId());
			entity.setPlayer(userProfile);
		}

		// Coach relationship mapping
		if (dto.getCoachId() != null) {
			UserProfile userProfile = new UserProfile();
			userProfile.setId(dto.getCoachId());
			entity.setCreatedBy(userProfile);
		}

		// Course relationship mapping
		if (dto.getCourseId() != null) {
			Course course = new Course();
			course.setId(dto.getCourseId()); // Corrected: should map to courseId
			entity.setCourse(course);
		}

		// Academy relationship mapping
		if (dto.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(dto.getAcademyId()); // Corrected: should map to academyId
			entity.setAcademy(academy);
		}

		// Handle VideoAnalysis mapping
		if (dto.getAnalysis() != null) {
			List<VideoAnalysis> analysisList = dto.getAnalysis().stream()
					.map(analysisDto -> VideoAnalysisMapper.mapDtoToEntity(analysisDto, entity)) // Pass the parent
																									// entity to the
																									// mapper
					.collect(Collectors.toList());
			entity.setAnalysis(analysisList);
		}

		return entity;
	}

	public static VideoAnalyzer mapDtoToExistingEntity(VideoAnalyzerDto dto, VideoAnalyzer existing) {
		// Update simple fields
		if (dto.getTitle() != null) {
			existing.setTitle(dto.getTitle());
		}
		if (dto.getMediaUrl() != null) {
			existing.setMediaUrl(dto.getMediaUrl());
		}

		if (dto.getThumbnailUrl() != null) {
			existing.setThumbnailUrl(dto.getThumbnailUrl());
		}

		// Update relationships (if present in DTO)
		if (dto.getPlayerId() != null) {
			UserProfile player = new UserProfile();
			player.setId(dto.getPlayerId());
			existing.setPlayer(player);
		}
		if (dto.getCoachId() != null) {
			UserProfile coach = new UserProfile();
			coach.setId(dto.getCoachId());
			existing.setCreatedBy(coach);
		}
		if (dto.getCourseId() != null) {
			Course course = new Course();
			course.setId(dto.getCourseId());
			existing.setCourse(course);
		}
		if (dto.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(dto.getAcademyId());
			existing.setAcademy(academy);
		}

		return existing;
	}

	public static UserProfile mapIdToUser(String id) {
		UserProfile userProfile = new UserProfile();
		userProfile.setId(id);
		return userProfile;
	}

	public static Course mapIdToCourse(String id) {
		Course course = new Course();
		course.setId(id);
		return course;
	}

}