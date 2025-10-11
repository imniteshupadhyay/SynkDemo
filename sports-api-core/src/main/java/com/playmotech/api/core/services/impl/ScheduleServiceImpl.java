package com.playmotech.api.core.services.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.DaywiseActivity;
import com.playmotech.api.core.dao_postgres.DaywiseActivityMapping;
import com.playmotech.api.core.dao_postgres.NewSchedule;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.ScheduleFile;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.dto.Mail;
import com.playmotech.api.core.dto.NewScheduleDto;
import com.playmotech.api.core.dto.ScheduleFileUploadRequest;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.mapper.ScheduleMapper;
import com.playmotech.api.core.repo.ActivityRepository;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.DaywiseActivityMappingRepo;
import com.playmotech.api.core.repo.DaywiseActivityRepo;
import com.playmotech.api.core.repo.NewScheduleRepo;
import com.playmotech.api.core.repo.OrgRepo;
import com.playmotech.api.core.repo.ScheduleFileRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.NewScheduleDao;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.ScheduleService;
import com.playmotech.api.core.specification.ScheduleSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.ScheduleValidation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

	private final NewScheduleRepo scheduleRepository;
	private final DaywiseActivityRepo daywiseActivityRepo;
	private final DaywiseActivityMappingRepo daywiseActivityMappingRepo;
	private final ActivityRepository activityRepo;
	private final ScheduleValidation validationUtil;
	private final AcademyDomainUtil academyDomainUtil;
	private final OrgRepo orgRepo;
	private final UserProfileRepo userProfileRepository;
	private final IStorageService storageService;
	private final CourseRepo courseRepo;
	private final ScheduleFileRepo scheduleFileRepo;
	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	private final IPushNotificationService pushNotificationService;
	private final IMailService mailService;

	@Value("${storage.courses-media-bucket}")
	private String coursesMediaBucket;
	@Value("${courses-media-base-url}")
	private String coursesMediaBaseUrl;

	@Override
	public ServiceResponse getScheduleList(GenericFilter filter, String domainUrl) {
		try {
			log.info(ApiResponse.START_GET_SCHEDULES_LIST.getMessage());

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
			filter.setUserRole(userRole);
			filter.setDomainUrl(domainUrl);
			ScheduleSpecification specification = new ScheduleSpecification(filter);
			List<NewSchedule> scheduleList;
			Page<NewSchedule> pageableContent = null;

			if (filter.isPageable()) {
				PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = scheduleRepository.findAll(specification, page);
				scheduleList = pageableContent.getContent();
			} else {
				scheduleList = scheduleRepository.findAll(specification);
			}

			if (scheduleList.isEmpty()) {
				log.info(ApiResponse.SCHEDULE_NOT_FOUND.getMessage());
				return ResponseBuilder.success(ApiResponse.SCHEDULE_NOT_FOUND, HttpStatus.OK);
			}

			List<NewScheduleDao> scheduleListDao = scheduleList.stream().map(ScheduleMapper::mapEntityToDao)
					.collect(Collectors.toList());

			if (filter.isPageable()) {
				return ResponseBuilder.success(scheduleListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK,
						pageableContent.getTotalPages(), pageableContent.getTotalElements());
			}
			return ResponseBuilder.success(scheduleListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK);
		} catch (Exception e) {
			log.error(ApiResponse.ERROR_FETCHING_LIST.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	@Override
	public ServiceResponse getSchedule(Long id) {
		try {
			log.info(ApiResponse.START_GET_SCHEDULE.getMessage());

			if (!validationUtil.isValidId(id)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}
			Optional<NewSchedule> scheduleOptional = scheduleRepository.findByIdAndDeletedIsFalse(id);

			if (scheduleOptional.isEmpty()) {
				log.info(ApiResponse.SCHEDULE_NOT_FOUND.getMessage());
				return ResponseBuilder.notFound(ApiResponse.SCHEDULE_NOT_FOUND);
			}

			NewScheduleDao dao = ScheduleMapper.mapEntityToDao(scheduleOptional.get());
			return ResponseBuilder.success(dao, ApiResponse.SCHEDULE_FETCHED);

		} catch (Exception e) {
			log.error(ApiResponse.ERROR_SCHEDULE_FETCHED.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse addSchedule(NewScheduleDto scheduleDto, String academyDomain) {
		try {

			UserProfile loginUser = getCurrentUser();

			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);

			if (org.isEmpty()) {
				return ResponseBuilder.forbidden("Organisation not found for domain: " + academyDomain);
			}

			log.info(ApiResponse.START_ADD_SCHEDULE.getMessage());

			// Validate DTO
			if (!validationUtil.validate(scheduleDto)) {
				return ResponseBuilder.error(ApiResponse.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
			}

			if (scheduleRepository.existsByNameAndOrganisationIdAndDeletedFalse(scheduleDto.getName(),
					org.get().getId())) {
				return ResponseBuilder.conflict(ApiResponse.DUPLICATE_SCHEDULE_NAME);
			}

			// Map DTO to entity using existing mapper
			NewSchedule schedule = ScheduleMapper.mapDtoToEntity(scheduleDto);

			schedule.setCreatedBy(loginUser);
			schedule.setOrganisation(org.get());

			// Save the schedule with its related entities in one go
			// The cascading will handle saving child entities
			scheduleRepository.save(schedule);

			return ResponseBuilder.success(ApiResponse.SCHEDULE_ADDED, HttpStatus.CREATED);

		} catch (Exception e) {
			log.error(ApiResponse.EXCEPTION_IN_ADD_SCHEDULE.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateSchedule(NewScheduleDto scheduleDto, String academyDomain) {
		try {

			UserProfile loginUser = getCurrentUser();

			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);

			if (org.isEmpty()) {
				return ResponseBuilder.forbidden("Organisation not found for domain: " + academyDomain);
			}

			log.info(ApiResponse.START_UPDATE_SCHEDULE.getMessage());

			// Validate inputs
			if (!validationUtil.isValidId(scheduleDto.getId()) || !validationUtil.validate(scheduleDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			if (scheduleRepository.existsByNameAndOrganisationIdAndDeletedFalseAndIdIsNot(scheduleDto.getName(),
					org.get().getId(), scheduleDto.getId())) {
				return ResponseBuilder.conflict(ApiResponse.DUPLICATE_SCHEDULE_NAME);
			}

			// Fetch existing schedule
			Optional<NewSchedule> scheduleOptional = scheduleRepository.findByIdAndDeletedIsFalse(scheduleDto.getId());
			if (scheduleOptional.isEmpty()) {
				return ResponseBuilder.error(ApiResponse.SCHEDULE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			NewSchedule existing = scheduleOptional.get();

			// Handle daywise activities if present
			if (scheduleDto.getDaywiseActivities() != null && !scheduleDto.getDaywiseActivities().isEmpty()) {
				updateDaywiseActivities(existing, scheduleDto.getDaywiseActivities());
			}

			// Update basic details using BeanUtils instead of setting each property
			// individually
			BeanUtils.copyProperties(scheduleDto, existing);
			existing.setUpdatedBy(loginUser);

			// Save the updated schedule
			scheduleRepository.save(existing);
			return ResponseBuilder.success(ApiResponse.SCHEDULE_UPDATED, HttpStatus.OK);

		} catch (Exception e) {
			log.error(ApiResponse.EXCEPTION_IN_UPDATE_SCHEDULE.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Helper method to update daywise activities for a schedule
	 */
	private void updateDaywiseActivities(NewSchedule schedule, List<DaywiseActivityDto> activityDtos) {
		// Track IDs of activities to keep
		Set<Long> updatedActivityIds = new HashSet<>();
		List<DaywiseActivity> existingActivities = daywiseActivityRepo.findByScheduleId(schedule.getId());

		// Create map of existing activities for quick lookup
		Map<Long, DaywiseActivity> existingActivityMap = existingActivities.stream()
				.collect(Collectors.toMap(DaywiseActivity::getId, Function.identity(), (e1, e2) -> e1));

		// Process each activity DTO
		for (DaywiseActivityDto activityDto : activityDtos) {
			DaywiseActivity daywiseActivity;

			// Check if updating existing or creating new
			if (activityDto.getId() != null && existingActivityMap.containsKey(activityDto.getId())) {
				// Update existing activity
				daywiseActivity = existingActivityMap.get(activityDto.getId());
				updatedActivityIds.add(daywiseActivity.getId());

				// Update activity fields
				daywiseActivity.setStartTime(activityDto.getStartTime());
				daywiseActivity.setEndTime(activityDto.getEndTime());
				daywiseActivity.setActivityDate(activityDto.getActivityDate());
			} else {
				// Create new activity
				daywiseActivity = new DaywiseActivity();
				daywiseActivity.setSchedule(schedule);
				daywiseActivity.setStartTime(activityDto.getStartTime());
				daywiseActivity.setEndTime(activityDto.getEndTime());
				daywiseActivity.setActivityDate(activityDto.getActivityDate());
			}

			// Save the activity to get its ID for mappings
			daywiseActivity = daywiseActivityRepo.save(daywiseActivity);

			// Handle activity mappings if present
			if (activityDto.getActivityMappings() != null && !activityDto.getActivityMappings().isEmpty()) {
				updateActivityMappings(daywiseActivity, activityDto.getActivityMappings());
			}
		}

		// Delete activities that weren't updated
		existingActivities.stream().filter(activity -> !updatedActivityIds.contains(activity.getId()))
				.forEach(daywiseActivityRepo::delete);
	}

	/**
	 * Helper method to update activity mappings for a daywise activity
	 */
	private void updateActivityMappings(DaywiseActivity daywiseActivity, List<DaywiseActivityMappingDto> mappingDtos) {
		// Track IDs of mappings to keep
		Set<Long> updatedMappingIds = new HashSet<>();

		// Get existing mappings
		List<DaywiseActivityMapping> existingMappings = daywiseActivityMappingRepo
				.findByDaywiseActivityId(daywiseActivity.getId());

		// Create map of existing mappings for quick lookup
		Map<Long, DaywiseActivityMapping> existingMappingMap = existingMappings.stream()
				.collect(Collectors.toMap(DaywiseActivityMapping::getId, Function.identity(), (e1, e2) -> e1));

		// Process each mapping DTO
		for (DaywiseActivityMappingDto mappingDto : mappingDtos) {
			// Check if activityIds are valid
			if (mappingDto.getActivityIds() == null || mappingDto.getActivityIds().isEmpty()) {
				continue; // Skip invalid activity mapping
			}

			// Validate all activities exist
			List<Activity> activities = new ArrayList<>();
			for (Long activityId : mappingDto.getActivityIds()) {
				Optional<Activity> activityOptional = activityRepo.findById(activityId);
				if (activityOptional.isPresent()) {
					activities.add(activityOptional.get());
				}
			}

			if (activities.isEmpty()) {
				continue; // Skip if no valid activities found
			}

			DaywiseActivityMapping mapping;

			// Check if updating existing or creating new
			if (mappingDto.getId() != null && existingMappingMap.containsKey(mappingDto.getId())) {
				// Update existing mapping
				mapping = existingMappingMap.get(mappingDto.getId());
				updatedMappingIds.add(mapping.getId());

				// Update mapping fields
				// For existing mappings, we need to update the activities
				// This requires changes to the entity model to support multiple activities
				updateMappingActivities(mapping, activities);
				mapping.setStartTime(mappingDto.getStartTime());
				mapping.setEndTime(mappingDto.getEndTime());
			} else {
				// Create new mapping
				mapping = new DaywiseActivityMapping();
				mapping.setDaywiseActivity(daywiseActivity);
				// Set multiple activities
				setMappingActivities(mapping, activities);
				mapping.setStartTime(mappingDto.getStartTime());
				mapping.setEndTime(mappingDto.getEndTime());
			}

			daywiseActivityMappingRepo.save(mapping);
		}

		// Delete mappings that weren't updated
		existingMappings.stream().filter(mapping -> !updatedMappingIds.contains(mapping.getId()))
				.forEach(daywiseActivityMappingRepo::delete);
	}

	/**
	 * Helper method to update activities for a mapping
	 * Note: This assumes DaywiseActivityMapping has been modified to support
	 * multiple activities
	 */
	private void updateMappingActivities(DaywiseActivityMapping mapping, List<Activity> activities) {
		// First clear existing relationships
		mapping.getActivities().clear();
		// Then add new relationships
		mapping.getActivities().addAll(activities);
	}

	/**
	 * Helper method to set activities for a new mapping
	 * Note: This assumes DaywiseActivityMapping has been modified to support
	 * multiple activities
	 */
	private void setMappingActivities(DaywiseActivityMapping mapping, List<Activity> activities) {

		// a many-to-many relationship table:
		mapping.setActivities(new ArrayList<>(activities));
	}

	@Override
	@Transactional
	public ServiceResponse deleteSchedule(Long id) {
		try {
			log.info(ApiResponse.START_DELETE_SCHEDULE.getMessage());

			// Get current user
			UserProfile loginUser = getCurrentUser();

			if (!validationUtil.isValidId(id)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			Optional<NewSchedule> scheduleOptional = scheduleRepository.findById(id);
			if (scheduleOptional.isEmpty()) {
				return ResponseBuilder.error(ApiResponse.SCHEDULE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			NewSchedule schedule = scheduleOptional.get();
			schedule.setDeleted(true);
			schedule.setUpdatedBy(loginUser);
			scheduleRepository.save(schedule);

			return ResponseBuilder.success(ApiResponse.SCHEDULE_DELETED, HttpStatus.OK);

		} catch (Exception e) {
			log.error(ApiResponse.EXCEPTION_IN_DELETE_SCHEDULE.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse addDaywiseActivity(Long scheduleId, DaywiseActivityDto activityDto, String academyDomain) {
		try {
			log.info("Starting to add daywise activity");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate inputs using validation utility
			if (!validationUtil.validateAddDaywiseActivity(activityDto, scheduleId)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the schedule
			Optional<NewSchedule> scheduleOptional = scheduleRepository.findByIdAndDeletedIsFalse(scheduleId);
			if (scheduleOptional.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.SCHEDULE_NOT_FOUND);
			}

			NewSchedule schedule = scheduleOptional.get();

			// For new activities, we'll check if they fall outside current schedule
			// boundaries
			// If so, we'll expand the schedule boundaries instead of returning validation
			// errors
			boolean boundariesNeedUpdate = false;

			// Check if activity date is outside schedule date range
			if (activityDto.getActivityDate().isBefore(schedule.getStartDate())
					|| activityDto.getActivityDate().isAfter(schedule.getEndDate())) {
				log.info("Activity date {} is outside schedule date range [{} to {}]", activityDto.getActivityDate(),
						schedule.getStartDate(), schedule.getEndDate());
				boundariesNeedUpdate = true;
			}

			// Check if activity time is outside schedule time boundaries
			if ((schedule.getStartTime() != null && activityDto.getStartTime().isBefore(schedule.getStartTime()))
					|| (schedule.getEndTime() != null && activityDto.getEndTime().isAfter(schedule.getEndTime()))) {
				log.info("Activity time [{} to {}] is outside schedule time boundaries [{} to {}]",
						activityDto.getStartTime(), activityDto.getEndTime(), schedule.getStartTime(),
						schedule.getEndTime());
				boundariesNeedUpdate = true;
			}

			// Verify domain access
			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);
			if (org.isEmpty() || !org.get().getId().equals(schedule.getOrganisation().getId())) {
				return ResponseBuilder.forbidden("You don't have access to this schedule");
			}

			// Create new daywise activity
			DaywiseActivity daywiseActivity = new DaywiseActivity();
			daywiseActivity.setSchedule(schedule);
			daywiseActivity.setStartTime(activityDto.getStartTime());
			daywiseActivity.setEndTime(activityDto.getEndTime());
			daywiseActivity.setActivityDate(activityDto.getActivityDate());

			// Save the activity to get its ID for mappings
			daywiseActivity = daywiseActivityRepo.save(daywiseActivity);

			// Handle activity mappings if present
			if (activityDto.getActivityMappings() != null && !activityDto.getActivityMappings().isEmpty()) {
				// Additional validation: Validate mappings in detail
				if (!validationUtil.validateActivityMappingsDetailed(activityDto.getActivityMappings())) {
					// Rollback the saved activity since mappings are invalid
					daywiseActivityRepo.delete(daywiseActivity);
					return ResponseBuilder.badRequest("Invalid activity mappings");
				}

				for (DaywiseActivityMappingDto mappingDto : activityDto.getActivityMappings()) {
					// Validate activity exists
					List<Activity> activityOptional = activityRepo.findByIdIn(mappingDto.getActivityIds());
					if (activityOptional.isEmpty()) {
						continue; // Skip invalid activity
					}

					// Create new mapping
					DaywiseActivityMapping mapping = new DaywiseActivityMapping();
					mapping.setDaywiseActivity(daywiseActivity);
					mapping.setActivities(activityOptional);
					mapping.setStartTime(mappingDto.getStartTime());
					mapping.setEndTime(mappingDto.getEndTime());

					daywiseActivityMappingRepo.save(mapping);
				}
			}

			// If boundaries need update, update schedule boundaries
			if (boundariesNeedUpdate) {
				log.info("Updating schedule boundaries after adding new activity");
				schedule = updateScheduleBoundaries(schedule, loginUser);
			} else {
				schedule.setUpdatedBy(loginUser);
				scheduleRepository.save(schedule);
			}

			return ResponseBuilder.success(daywiseActivity.getId(), ApiResponse.DAY_ACTIVITY_ADDED, HttpStatus.CREATED);

		} catch (Exception e) {
			log.error("Exception in adding daywise activity: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateDaywiseActivity(DaywiseActivityDto activityDto, String academyDomain) {
		try {
			log.info("Starting to update daywise activity");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate inputs using validation utility
			if (!validationUtil.validateUpdateDaywiseActivity(activityDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the daywise activity
			Optional<DaywiseActivity> activityOptional = daywiseActivityRepo.findById(activityDto.getId());
			if (activityOptional.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.DAY_ACTIVITY_NOT_FOUND);
			}

			DaywiseActivity existingActivity = activityOptional.get();
			NewSchedule schedule = existingActivity.getSchedule();

			// For updating activities, we'll check if they fall outside current schedule
			// boundaries
			// If so, we'll expand the schedule boundaries instead of returning validation
			// errors
			boolean boundariesNeedUpdate = false;

			// Check if activity date is outside schedule date range
			if (activityDto.getActivityDate().isBefore(schedule.getStartDate())
					|| activityDto.getActivityDate().isAfter(schedule.getEndDate())) {
				log.info("Updated activity date {} is outside schedule date range [{} to {}]",
						activityDto.getActivityDate(), schedule.getStartDate(), schedule.getEndDate());
				boundariesNeedUpdate = true;
			}

			// Check if activity time is outside schedule time boundaries
			if ((schedule.getStartTime() != null && activityDto.getStartTime().isBefore(schedule.getStartTime()))
					|| (schedule.getEndTime() != null && activityDto.getEndTime().isAfter(schedule.getEndTime()))) {
				log.info("Updated activity time [{} to {}] is outside schedule time boundaries [{} to {}]",
						activityDto.getStartTime(), activityDto.getEndTime(), schedule.getStartTime(),
						schedule.getEndTime());
				boundariesNeedUpdate = true;
			}

			// Verify domain access
			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);
			if (org.isEmpty() || !org.get().getId().equals(schedule.getOrganisation().getId())) {
				return ResponseBuilder.forbidden("You don't have access to this activity");
			}

			// Update the activity details
			existingActivity.setStartTime(activityDto.getStartTime());
			existingActivity.setEndTime(activityDto.getEndTime());
			existingActivity.setActivityDate(activityDto.getActivityDate());

			// Save updated activity
			daywiseActivityRepo.save(existingActivity);

			// Handle activity mappings if present
			if (activityDto.getActivityMappings() != null && !activityDto.getActivityMappings().isEmpty()) {
				// Additional validation: Validate mappings in detail
				if (!validationUtil.validateActivityMappingsDetailed(activityDto.getActivityMappings())) {
					return ResponseBuilder.badRequest("Invalid activity mappings");
				}

				// Track IDs of mappings to keep
				Set<Long> updatedMappingIds = new HashSet<>();

				// Get existing mappings
				List<DaywiseActivityMapping> existingMappings = daywiseActivityMappingRepo
						.findByDaywiseActivityId(existingActivity.getId());

				// Create map of existing mappings for quick lookup
				Map<Long, DaywiseActivityMapping> existingMappingMap = existingMappings.stream()
						.collect(Collectors.toMap(DaywiseActivityMapping::getId, Function.identity(), (e1, e2) -> e1));

				// Process each mapping DTO
				for (DaywiseActivityMappingDto mappingDto : activityDto.getActivityMappings()) {
					// Validate activity exists
					List<Activity> activityOpt = activityRepo.findByIdIn(mappingDto.getActivityIds());
					if (activityOpt.isEmpty()) { // Fixed the variable name here
						continue; // Skip invalid activity
					}

					DaywiseActivityMapping mapping;

					// Check if updating existing or creating new
					if (mappingDto.getId() != null && existingMappingMap.containsKey(mappingDto.getId())) {
						// Update existing mapping
						mapping = existingMappingMap.get(mappingDto.getId());
						updatedMappingIds.add(mapping.getId());

						// Update mapping fields
						mapping.setActivities(activityOpt);
						mapping.setStartTime(mappingDto.getStartTime());
						mapping.setEndTime(mappingDto.getEndTime());
					} else {
						// Create new mapping
						mapping = new DaywiseActivityMapping();
						mapping.setDaywiseActivity(existingActivity);
						mapping.setActivities(activityOpt);
						mapping.setStartTime(mappingDto.getStartTime());
						mapping.setEndTime(mappingDto.getEndTime());
					}

					daywiseActivityMappingRepo.save(mapping);
				}

				// Delete mappings that weren't updated
				existingMappings.stream().filter(mapping -> !updatedMappingIds.contains(mapping.getId()))
						.forEach(daywiseActivityMappingRepo::delete);
			} else {
				// If no mappings in request, delete all existing mappings
				List<DaywiseActivityMapping> existingMappings = daywiseActivityMappingRepo
						.findByDaywiseActivityId(existingActivity.getId());
				daywiseActivityMappingRepo.deleteAll(existingMappings);
			}

			// If boundaries need update, update schedule boundaries
			if (boundariesNeedUpdate) {
				log.info("Updating schedule boundaries after updating activity");
				schedule = updateScheduleBoundaries(schedule, loginUser);
			} else {
				schedule.setUpdatedBy(loginUser);
				scheduleRepository.save(schedule);
			}

			return ResponseBuilder.success(ApiResponse.DAY_ACTIVITY_UPDATED);

		} catch (Exception e) {
			log.error("Exception in updating daywise activity: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse deleteDaywiseActivity(Long id) {
		try {
			log.info("Starting to delete daywise activity");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate ID
			if (!validationUtil.isValidId(id)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the daywise activity
			Optional<DaywiseActivity> activityOptional = daywiseActivityRepo.findById(id);
			if (activityOptional.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.DAY_ACTIVITY_NOT_FOUND);
			}

			DaywiseActivity activity = activityOptional.get();
			NewSchedule schedule = activity.getSchedule();

			// First delete all related mappings
			List<DaywiseActivityMapping> mappings = daywiseActivityMappingRepo.findByDaywiseActivityId(id);
			daywiseActivityMappingRepo.deleteAll(mappings);

			// Then delete the activity itself
			daywiseActivityRepo.delete(activity);

			// After deletion, update schedule boundaries since the min/max times or dates
			// might have changed
			log.info("Updating schedule boundaries after deleting activity");
			updateScheduleBoundaries(schedule, loginUser);

			return ResponseBuilder.success(ApiResponse.DAY_ACTIVITY_DELETED);

		} catch (Exception e) {
			log.error("Exception in deleting daywise activity: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse addDaywiseActivityMapping(Long daywiseActivityId, DaywiseActivityMappingDto mappingDto,
			String academyDomain) {
		try {
			log.info("Starting to add daywise activity mapping");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate inputs
			if (!validationUtil.isValidId(daywiseActivityId) || mappingDto == null ||
					mappingDto.getActivityIds() == null || mappingDto.getActivityIds().isEmpty() ||
					mappingDto.getStartTime() == null || mappingDto.getEndTime() == null ||
					mappingDto.getStartTime().isAfter(mappingDto.getEndTime())) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the daywise activity
			Optional<DaywiseActivity> daywiseActivityOptional = daywiseActivityRepo.findById(daywiseActivityId);
			if (daywiseActivityOptional.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.DAY_ACTIVITY_NOT_FOUND);
			}

			DaywiseActivity daywiseActivity = daywiseActivityOptional.get();
			NewSchedule schedule = daywiseActivity.getSchedule();

			// Verify domain access
			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);
			if (org.isEmpty() || !org.get().getId().equals(schedule.getOrganisation().getId())) {
				return ResponseBuilder.forbidden("You don't have access to this activity");
			}

			// Validate all activities exist
			List<Activity> activities = new ArrayList<>();
			for (Long activityId : mappingDto.getActivityIds()) {
				Optional<Activity> activityOptional = activityRepo.findById(activityId);
				if (activityOptional.isPresent()) {
					activities.add(activityOptional.get());
				}
			}

			if (activities.isEmpty()) {
				return ResponseBuilder.notFound("No valid activities found");
			}

			// For new mappings, we'll check if they fall outside current activity
			// boundaries
			// If so, we'll expand the activity boundaries instead of returning validation
			// errors
			boolean boundariesNeedUpdate = false;

			// Check if mapping time is outside activity time boundaries
			if (mappingDto.getStartTime().isBefore(daywiseActivity.getStartTime()) ||
					mappingDto.getEndTime().isAfter(daywiseActivity.getEndTime())) {
				log.debug("Mapping time [{} to {}] is outside activity time boundaries [{} to {}]",
						mappingDto.getStartTime(), mappingDto.getEndTime(),
						daywiseActivity.getStartTime(), daywiseActivity.getEndTime());
				boundariesNeedUpdate = true;
			}

			// Check for time overlaps with existing mappings
			List<DaywiseActivityMapping> existingMappings = daywiseActivityMappingRepo
					.findByDaywiseActivityId(daywiseActivityId);
			for (DaywiseActivityMapping existingMapping : existingMappings) {
				// Check for time overlap
				if (!(mappingDto.getEndTime().isBefore(existingMapping.getStartTime()) ||
						mappingDto.getStartTime().isAfter(existingMapping.getEndTime()) ||
						mappingDto.getEndTime().equals(existingMapping.getStartTime()) ||
						mappingDto.getStartTime().equals(existingMapping.getEndTime()))) {
					return ResponseBuilder.badRequest("Time range overlaps with existing mapping");
				}
			}

			// Create new mapping
			DaywiseActivityMapping mapping = new DaywiseActivityMapping();
			mapping.setDaywiseActivity(daywiseActivity);
			// Set multiple activities
			setMappingActivities(mapping, activities);
			mapping.setStartTime(mappingDto.getStartTime());
			mapping.setEndTime(mappingDto.getEndTime());

			// Save the mapping
			mapping = daywiseActivityMappingRepo.save(mapping);

			// If boundaries need update, update activity boundaries
			if (boundariesNeedUpdate) {
				log.info("Updating daywise activity boundaries after adding new mapping");
				daywiseActivity = updateDaywiseActivityBoundaries(daywiseActivity, loginUser);
			}

			return ResponseBuilder.success(ApiResponse.DAY_TIMESLOT_ADDED);

		} catch (Exception e) {
			log.error("Exception in adding daywise activity mapping: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateDaywiseActivityMapping(DaywiseActivityMappingDto mappingDto, String academyDomain) {
		try {
			log.info("Starting to update daywise activity mapping");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate inputs
			if (!validationUtil.isValidId(mappingDto.getId()) ||
					mappingDto.getActivityIds() == null || mappingDto.getActivityIds().isEmpty() ||
					mappingDto.getStartTime() == null || mappingDto.getEndTime() == null ||
					mappingDto.getStartTime().isAfter(mappingDto.getEndTime())) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the mapping
			Optional<DaywiseActivityMapping> mappingOptional = daywiseActivityMappingRepo.findById(mappingDto.getId());
			if (mappingOptional.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.DAY_TIMESLOT_NOT_FOUND);
			}

			DaywiseActivityMapping existingMapping = mappingOptional.get();
			DaywiseActivity daywiseActivity = existingMapping.getDaywiseActivity();
			NewSchedule schedule = daywiseActivity.getSchedule();

			// Verify domain access
			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);
			if (org.isEmpty() || !org.get().getId().equals(schedule.getOrganisation().getId())) {
				return ResponseBuilder.forbidden("You don't have access to this mapping");
			}

			// Validate all activities exist
			List<Activity> activities = new ArrayList<>();
			for (Long activityId : mappingDto.getActivityIds()) {
				Optional<Activity> activityOptional = activityRepo.findById(activityId);
				if (activityOptional.isPresent()) {
					activities.add(activityOptional.get());
				}
			}

			if (activities.isEmpty()) {
				return ResponseBuilder.notFound("No valid activities found");
			}

			// For updating mappings, we'll check if they fall outside current activity
			// boundaries
			// If so, we'll expand the activity boundaries instead of returning validation
			// errors
			boolean boundariesNeedUpdate = false;

			// Check if mapping time is outside activity time boundaries
			if (mappingDto.getStartTime().isBefore(daywiseActivity.getStartTime()) ||
					mappingDto.getEndTime().isAfter(daywiseActivity.getEndTime())) {
				log.info("Updated mapping time [{} to {}] is outside activity time boundaries [{} to {}]",
						mappingDto.getStartTime(), mappingDto.getEndTime(),
						daywiseActivity.getStartTime(), daywiseActivity.getEndTime());
				boundariesNeedUpdate = true;
			}

			// Check for time overlaps with other existing mappings
			List<DaywiseActivityMapping> otherMappings = daywiseActivityMappingRepo
					.findByDaywiseActivityId(daywiseActivity.getId())
					.stream()
					.filter(m -> !m.getId().equals(existingMapping.getId()))
					.collect(Collectors.toList());

			for (DaywiseActivityMapping otherMapping : otherMappings) {
				// Check for time overlap, allowing adjacent time slots
				if (!(mappingDto.getEndTime().isBefore(otherMapping.getStartTime()) ||
						mappingDto.getStartTime().isAfter(otherMapping.getEndTime()) ||
						mappingDto.getEndTime().equals(otherMapping.getStartTime()) ||
						mappingDto.getStartTime().equals(otherMapping.getEndTime()))) {
					return ResponseBuilder.badRequest("Time range overlaps with another mapping");
				}
			}

			// Update mapping
			// Update activities list
			updateMappingActivities(existingMapping, activities);
			existingMapping.setStartTime(mappingDto.getStartTime());
			existingMapping.setEndTime(mappingDto.getEndTime());

			// Save the updated mapping
			daywiseActivityMappingRepo.save(existingMapping);

			// If boundaries need update, update activity boundaries
			if (boundariesNeedUpdate) {
				log.info("Updating daywise activity boundaries after updating mapping");
				daywiseActivity = updateDaywiseActivityBoundaries(daywiseActivity, loginUser);
			}

			return ResponseBuilder.success(ApiResponse.DAY_TIMESLOT_UPDATED);

		} catch (Exception e) {
			log.error("Exception in updating daywise activity mapping: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse deleteDaywiseActivityMapping(Long id) {
		try {
			log.info("Starting to delete daywise activity mapping");

			// Get current user
			UserProfile loginUser = getCurrentUser();

			// Validate ID
			if (!validationUtil.isValidId(id)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Find the mapping
			Optional<DaywiseActivityMapping> mappingOptional = daywiseActivityMappingRepo.findById(id);
			if (mappingOptional.isEmpty()) {
				return ResponseBuilder.notFound("Activity mapping not found");
			}

			// Store reference to the daywise activity before deletion
			DaywiseActivityMapping mapping = mappingOptional.get();
			DaywiseActivity daywiseActivity = mapping.getDaywiseActivity();

			// Delete the mapping
			daywiseActivityMappingRepo.delete(mapping);

			// After deletion, update daywise activity boundaries since the min/max times
			// might have changed
			log.info("Updating daywise activity boundaries after deleting mapping");
			updateDaywiseActivityBoundaries(daywiseActivity, loginUser);

			return ResponseBuilder.success(ApiResponse.DAY_TIMESLOT_DELETED);

		} catch (Exception e) {
			log.error("Exception in deleting daywise activity mapping: " + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Helper method to update schedule time and date boundaries based on its
	 * daywise activities
	 * 
	 * @param schedule  The schedule to update
	 * @param loginUser
	 * @return The updated schedule
	 */
	private NewSchedule updateScheduleBoundaries(NewSchedule schedule, UserProfile loginUser) {
		log.info("Updating schedule boundaries for schedule ID: {}", schedule.getId());

		// Fetch all daywise activities for this schedule
		List<DaywiseActivity> allActivities = daywiseActivityRepo.findByScheduleId(schedule.getId());

		if (allActivities.isEmpty()) {
			log.info("No activities found for schedule - keeping original boundaries");
			return schedule;
		}

		// Initialize with the first activity's values (will be updated in the loop)
		LocalDate newStartDate = allActivities.get(0).getActivityDate();
		LocalDate newEndDate = allActivities.get(0).getActivityDate();
		LocalTime newStartTime = allActivities.get(0).getStartTime();
		LocalTime newEndTime = allActivities.get(0).getEndTime();

		// Find the earliest date, latest date, earliest start time, and latest end time
		for (DaywiseActivity activity : allActivities) {
			// Update date boundaries
			if (activity.getActivityDate().isBefore(newStartDate)) {
				newStartDate = activity.getActivityDate();
			}
			if (activity.getActivityDate().isAfter(newEndDate)) {
				newEndDate = activity.getActivityDate();
			}

			// Update time boundaries
			if (activity.getStartTime().isBefore(newStartTime)) {
				newStartTime = activity.getStartTime();
			}
			if (activity.getEndTime().isAfter(newEndTime)) {
				newEndTime = activity.getEndTime();
			}
		}

		// Check if any boundaries need to be updated
		boolean boundariesChanged = false;

		if (!newStartDate.equals(schedule.getStartDate())) {
			log.info("Updating schedule start date from {} to {}", schedule.getStartDate(), newStartDate);
			schedule.setStartDate(newStartDate);
			boundariesChanged = true;
		}

		if (!newEndDate.equals(schedule.getEndDate())) {
			log.info("Updating schedule end date from {} to {}", schedule.getEndDate(), newEndDate);
			schedule.setEndDate(newEndDate);
			boundariesChanged = true;
		}

		if (!newStartTime.equals(schedule.getStartTime())) {
			log.info("Updating schedule start time from {} to {}", schedule.getStartTime(), newStartTime);
			schedule.setStartTime(newStartTime);
			boundariesChanged = true;
		}

		if (!newEndTime.equals(schedule.getEndTime())) {
			log.info("Updating schedule end time from {} to {}", schedule.getEndTime(), newEndTime);
			schedule.setEndTime(newEndTime);
			boundariesChanged = true;
		}

		if (boundariesChanged) {
			// Save the updated schedule
			schedule = scheduleRepository.save(schedule);
			log.info("Schedule boundaries updated successfully");
		} else {
			log.info("No schedule boundary changes needed");
		}
		schedule.setUpdatedBy(loginUser);
		return scheduleRepository.save(schedule);
	}

	/**
	 * Helper method to update daywise activity time boundaries based on its
	 * mappings
	 * 
	 * @param daywiseActivity The daywise activity to update
	 * @param loginUser       The user performing the action
	 * @return The updated daywise activity
	 */
	private DaywiseActivity updateDaywiseActivityBoundaries(DaywiseActivity daywiseActivity, UserProfile loginUser) {
		log.info("Updating daywise activity boundaries for activity ID: {}", daywiseActivity.getId());

		// Fetch all mappings for this daywise activity
		List<DaywiseActivityMapping> allMappings = daywiseActivityMappingRepo
				.findByDaywiseActivityId(daywiseActivity.getId());

		if (allMappings.isEmpty()) {
			log.info("No mappings found for daywise activity - keeping original boundaries");
			return daywiseActivity;
		}

		// Initialize with the first mapping's values (will be updated in the loop)
		LocalTime newStartTime = allMappings.get(0).getStartTime();
		LocalTime newEndTime = allMappings.get(0).getEndTime();

		// Find the earliest start time and latest end time among mappings
		for (DaywiseActivityMapping mapping : allMappings) {
			// Update time boundaries
			if (mapping.getStartTime().isBefore(newStartTime)) {
				newStartTime = mapping.getStartTime();
			}
			if (mapping.getEndTime().isAfter(newEndTime)) {
				newEndTime = mapping.getEndTime();
			}
		}

		// Check if any boundaries need to be updated
		boolean boundariesChanged = false;

		if (!newStartTime.equals(daywiseActivity.getStartTime())) {
			log.info("Updating daywise activity start time from {} to {}", daywiseActivity.getStartTime(),
					newStartTime);
			daywiseActivity.setStartTime(newStartTime);
			boundariesChanged = true;
		}

		if (!newEndTime.equals(daywiseActivity.getEndTime())) {
			log.info("Updating daywise activity end time from {} to {}", daywiseActivity.getEndTime(), newEndTime);
			daywiseActivity.setEndTime(newEndTime);
			boundariesChanged = true;
		}

		if (boundariesChanged) {
			// Save the updated daywise activity
			daywiseActivity = daywiseActivityRepo.save(daywiseActivity);
			log.info("Daywise activity boundaries updated successfully");

			// Now check if we need to update schedule boundaries too
			NewSchedule schedule = daywiseActivity.getSchedule();
			boolean scheduleBoundariesNeedUpdate = false;

			// Check if activity time is outside schedule time boundaries
			if (schedule.getStartTime() != null && newStartTime.isBefore(schedule.getStartTime())) {
				scheduleBoundariesNeedUpdate = true;
			}
			if (schedule.getEndTime() != null && newEndTime.isAfter(schedule.getEndTime())) {
				scheduleBoundariesNeedUpdate = true;
			}

			// If schedule boundaries need update, update them
			if (scheduleBoundariesNeedUpdate) {
				log.info("Need to update schedule boundaries after daywise activity time change");
				updateScheduleBoundaries(schedule, loginUser);
			} else {
				schedule.setUpdatedBy(loginUser);
				scheduleRepository.save(schedule);
			}
		} else {
			log.info("No daywise activity boundary changes needed");
		}

		return daywiseActivity;
	}

	@Override
	public ServiceResponse uploadScheduleFile(ScheduleFileUploadRequest request, String academyDomain) {
		try {
			// Get current logged in user
			UserProfile loginUser = getCurrentUser();
			// Validate file
			log.info("Received schedule file upload request for domain: {}", academyDomain);
			if (request == null || request.getFile() == null || request.getFile().getContent() == null
					|| request.getFile().getContent().length == 0) {
				return ResponseBuilder.badRequest("File cannot be empty");
			}

			// Validate mapping
			log.info("Program mapping received: {}", request.getProgramMapping());
			Map<String, List<String>> programMapping = request.getProgramMapping();
			if (programMapping == null || programMapping.isEmpty()) {
				return ResponseBuilder.badRequest("Program mapping cannot be empty");
			}

			String originalFilename = request.getFile().getOriginalFilename();
			String ext = "";
			String fileNameWithoutExtension = "";

			if (originalFilename != null && originalFilename.contains(".")) {
				int dotIndex = originalFilename.lastIndexOf('.');
				ext = originalFilename.substring(dotIndex); // includes dot
				fileNameWithoutExtension = originalFilename.substring(0, dotIndex);
			} else {
				fileNameWithoutExtension = originalFilename != null ? originalFilename : "Schedule";
			}

			// Create timestamp string, e.g. "20250702T154530Z"
			String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
					.withZone(ZoneId.of("UTC"))
					.format(Instant.now());

			// Build key with filename, timestamp and extension
			String key = "schedules/" + fileNameWithoutExtension + "-" + timestamp + ext;

			log.info("Uploading schedule file to bucket: {}, key: {}", coursesMediaBucket, key);

			// Upload the file once
			storageService.upload(coursesMediaBucket, key, request.getFile().getContent(),
					request.getFile().getContentType());

			String fileUrl = coursesMediaBaseUrl + key;
			log.info("File successfully uploaded. Accessible URL: {}", fileUrl);

			// Persist ScheduleFile entity
			ScheduleFile scheduleFile = ScheduleFile.builder()
					.fileName(originalFilename)
					.fileUrl(fileUrl)
					.createdBy(loginUser.getId())
					.build();
			scheduleFile = scheduleFileRepo.save(scheduleFile);

			List<Course> courses = new ArrayList<>();
			// Persist file URL to each course (program) referenced
			for (Map.Entry<String, List<String>> entry : programMapping.entrySet()) {
				String academyId = entry.getKey();
				List<String> courseIds = entry.getValue();
				if (courseIds == null || courseIds.isEmpty()) {
					log.warn("No course IDs provided for academy {}, skipping", academyId);
					continue;
				}
				courses = courseRepo.findByAcademy_IdAndIdIn(academyId, courseIds);
				for (Course c : courses) {
					c.setScheduleFile(scheduleFile);
				}
				log.debug("Persisting schedule file for {} courses in academy {}", courses.size(), academyId);
				courseRepo.saveAll(courses);
			}
			sendPushNotificationAndEmailsToPlayers(courses, fileUrl);

			Map<String, Object> result = new HashMap<>();
			result.put("scheduleFile", scheduleFile);
			result.put("programMapping", programMapping);

			return ResponseBuilder.success(result, ApiResponse.FILE_UPLOADED_SUCCESSFULLY);
		} catch (Exception ex) {
			log.error("Error uploading schedule file", ex);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	private void sendPushNotificationAndEmailsToPlayers(List<Course> courses, String fileUrl) {
		try {
			List<String> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
			List<TraineeCourseEnrollment> playersEnrolledInCourses = traineeCourseEnrollmentRepo
					.findByCourse_IdIn(courseIds);

			// Group enrollments by course
			Map<String, List<TraineeCourseEnrollment>> enrollmentsByCourse = playersEnrolledInCourses.stream()
					.collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getId()));

			// Process notifications for each course
			for (Course course : courses) {
				List<TraineeCourseEnrollment> courseEnrollments = enrollmentsByCourse.get(course.getId());

				if (courseEnrollments != null && !courseEnrollments.isEmpty()) {
					sendPushNotificationsForCourse(course, courseEnrollments);
					sendEmailsForCourse(course, courseEnrollments, fileUrl);
				}
			}
		} catch (Exception e) {
			log.error("Error while prepping to send notification & email to the users");
		}
	}

	private void sendEmailsForCourse(Course course, List<TraineeCourseEnrollment> courseEnrollments, String fileUrl) {
		List<String> userEmails = courseEnrollments.stream()
				.map(TraineeCourseEnrollment::getTraineeUserProfile)
				.filter(Objects::nonNull)
				.map(UserProfile::getEmailId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());

		try {
			for (String emailId : userEmails) {
				List<String> attachments = new ArrayList<>();
				Mail mail = new Mail();
				mail.setSubject("New Schedule Added - " + course.getTitle());
				mail.setMessage(buildCourseScheduleMessage(course));
				mail.setRecipient(emailId);
				mail.setSender(null);
				mail.setModel(null);
				mail.setTemplateName(null);

				if (StringUtils.hasText(fileUrl)) {
					attachments.add(fileUrl);
					mail.setAttachments(attachments);
				}

				mailService.sendMailWithTemplate(mail);
			}
		} catch (Exception e) {
			log.error("Unexpected error sending email: {}", e.getMessage());
		}
	}

	private String buildCourseScheduleMessage(Course course) {
		StringBuilder message = new StringBuilder();
		message.append("Dear User,\n\n");
		message.append("A new schedule has been added for your course: ").append(course.getTitle()).append("\n\n");
		message.append("Please find the updated schedule attached to this email. ");
		message.append(
				"We recommend reviewing the new schedule carefully and marking the important dates in your calendar.\n\n");
		return message.toString();
	}

	private void sendPushNotificationsForCourse(Course course, List<TraineeCourseEnrollment> courseEnrollments) {
		List<String> userIds = courseEnrollments.stream().map(TraineeCourseEnrollment::getTraineeUserProfile)
				.map(UserProfile::getId)
				.collect(Collectors.toList());
		List<String> androidFcmPushTokens = courseEnrollments.stream()
				.map(TraineeCourseEnrollment::getTraineeUserProfile)
				.filter(Objects::nonNull)
				.map(UserProfile::getAndroidFcmPushToken)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());

		androidFcmPushTokens.forEach(token -> {
			Map<String, String> extraArgs = new HashMap<>();
			extraArgs.put("academyId", course.getAcademy().getId());
			extraArgs.put("courseId", course.getId());
			pushNotificationService.sendMessageToPushToken(
					token,
					NotificationType.LIVE_NOTIFICATION,
					"New schedule added for " + course.getTitle(),
					"Check out the updated schedule for your course: " + course.getTitle(),
					"PROGRAM_DETAILS",
					CtaType.SCREEN,
					extraArgs);
			pushNotificationService.addNotification(userIds,
					"Check out the updated schedule for your course: " + course.getTitle(), CtaType.SCREEN,
					"PROGRAM_DETAILS",
					extraArgs);
		});
	}

	private UserProfile getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail userDetail = (UserDetail) authentication.getPrincipal();
		return userProfileRepository.findById(userDetail.getUserId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
