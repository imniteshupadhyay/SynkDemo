package com.playmotech.api.core.mapper;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.playmotech.api.core.dao_postgres.DaywiseActivity;
import com.playmotech.api.core.dao_postgres.DaywiseActivityMapping;
import com.playmotech.api.core.dao_postgres.NewSchedule;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.dto.NewScheduleDto;
import com.playmotech.api.core.response.dao.DaywiseActivityDao;
import com.playmotech.api.core.response.dao.NewScheduleDao;
import com.playmotech.api.core.response.dao.UserProfileDao;

public class ScheduleMapper {

	public static NewScheduleDao mapEntityToDao(NewSchedule entity) {
		NewScheduleDao dao = new NewScheduleDao();

		// Copy simple properties
		BeanUtils.copyProperties(entity, dao);

		// Map DaywiseActivities
		if (entity.getDaywiseActivities() != null && !entity.getDaywiseActivities().isEmpty()) {
		    List<DaywiseActivityDao> daywiseActivityDaos = entity.getDaywiseActivities().stream()
		        .sorted(Comparator.comparing(daywiseActivity -> daywiseActivity.getActivityDate()))
		        .map(DaywiseActivityMapper::mapEntityToDao)
		        .collect(Collectors.toList());
		    dao.setDaywiseActivities(daywiseActivityDaos);
		}

		if (entity.getCreatedBy() != null) {
			dao.setCreatedBy(mapUserToDao(entity.getCreatedBy()));
		}

		return dao;
	}

	public static NewSchedule mapDtoToEntity(NewScheduleDto dto) {
		NewSchedule entity = new NewSchedule();

		// Copy simple properties
		BeanUtils.copyProperties(dto, entity, "id", "daywiseActivities", "course");
		entity.setDeleted(false);

		// Map DaywiseActivities with proper bidirectional relationship setup
		if (dto.getDaywiseActivities() != null && !dto.getDaywiseActivities().isEmpty()) {
			List<DaywiseActivity> activities = new ArrayList<>();

			for (DaywiseActivityDto activityDto : dto.getDaywiseActivities()) {
				DaywiseActivity activity = new DaywiseActivity();
				activity.setStartTime(activityDto.getStartTime());
				activity.setEndTime(activityDto.getEndTime());
				activity.setActivityDate(activityDto.getActivityDate());
				activity.setSchedule(entity); // Set the parent reference

				// Handle activity mappings
				if (activityDto.getActivityMappings() != null && !activityDto.getActivityMappings().isEmpty()) {
					List<DaywiseActivityMapping> mappings = new ArrayList<>();

					for (DaywiseActivityMappingDto mappingDto : activityDto.getActivityMappings()) {
						DaywiseActivityMapping mapping = new DaywiseActivityMapping();
						mapping.setStartTime(mappingDto.getStartTime());
						mapping.setEndTime(mappingDto.getEndTime());
						mapping.setDaywiseActivity(activity); // Set the parent reference

						if (mappingDto.getActivityIds() != null) {
							mapping.setActivities(ActivityMapper.mapIdsToEntities(mappingDto.getActivityIds()));
						}

						mappings.add(mapping);
					}

					activity.setActivityMappings(mappings);
				}

				activities.add(activity);
			}

			entity.setDaywiseActivities(activities);
		}

		return entity;
	}

	public static NewSchedule mapDtoToExistingEntity(NewScheduleDto dto, NewSchedule existing) {
		// Update simple fields using BeanUtils for non-null properties
		BeanUtils.copyProperties(dto, existing, getNullPropertyNames(dto));

		// Update daywise activities if provided
		if (dto.getDaywiseActivities() != null && !dto.getDaywiseActivities().isEmpty()) {
			// Clear existing activities to avoid duplicates (since we're using
			// orphanRemoval=true)
			existing.getDaywiseActivities().clear();

			// Add new activities from DTO
			for (DaywiseActivityDto activityDto : dto.getDaywiseActivities()) {
				DaywiseActivity activity = new DaywiseActivity();
				activity.setStartTime(activityDto.getStartTime());
				activity.setEndTime(activityDto.getEndTime());
				activity.setActivityDate(activityDto.getActivityDate());
				activity.setSchedule(existing);

				// Handle activity mappings
				if (activityDto.getActivityMappings() != null && !activityDto.getActivityMappings().isEmpty()) {
					List<DaywiseActivityMapping> mappings = new ArrayList<>();

					for (DaywiseActivityMappingDto mappingDto : activityDto.getActivityMappings()) {
						DaywiseActivityMapping mapping = new DaywiseActivityMapping();
						mapping.setStartTime(mappingDto.getStartTime());
						mapping.setEndTime(mappingDto.getEndTime());
						mapping.setDaywiseActivity(activity);
						if (mappingDto.getActivityIds() != null) {
							mapping.setActivities(ActivityMapper.mapIdsToEntities(mappingDto.getActivityIds()));
						}

						mappings.add(mapping);
					}

					activity.setActivityMappings(mappings);
				}

				existing.getDaywiseActivities().add(activity);
			}
		}

		return existing;
	}

	public static UserProfile mapIdToUser(String id) {
		UserProfile userProfile = new UserProfile();
		userProfile.setId(id);
		return userProfile;
	}

	public static UserProfileDao mapUserToDao(UserProfile entity) {
		UserProfileDao user = new UserProfileDao();
		user.setId(entity.getId());
		user.setUsername(entity.getUsername());
		user.setDisplayName(entity.getDisplayName());
		return user;
	}

	// Helper method to get null property names from a bean
	private static String[] getNullPropertyNames(Object source) {
		final BeanWrapper src = new BeanWrapperImpl(source);
		PropertyDescriptor[] pds = src.getPropertyDescriptors();

		Set<String> emptyNames = new HashSet<>();
		for (PropertyDescriptor pd : pds) {
			Object srcValue = src.getPropertyValue(pd.getName());
			if (srcValue == null) {
				emptyNames.add(pd.getName());
			}
		}

		String[] result = new String[emptyNames.size()];
		return emptyNames.toArray(result);
	}
}