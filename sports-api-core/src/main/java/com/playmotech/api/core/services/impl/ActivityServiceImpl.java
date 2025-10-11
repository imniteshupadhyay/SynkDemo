package com.playmotech.api.core.services.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.ActivityDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.mapper.ActivityMapper;
import com.playmotech.api.core.repo.ActivityRepository;
import com.playmotech.api.core.repo.OrgRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.ActivityDao;
import com.playmotech.api.core.response.dao.ActivityExportDao;
import com.playmotech.api.core.services.ActivityService;
import com.playmotech.api.core.specification.ActivitySpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.ActivityValidation;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ActivityServiceImpl implements ActivityService {

	private final ActivityRepository activityRepository;
	private final ActivityValidation activityValidation;
	private final UserProfileRepo userProfileRepository;
	private final AcademyDomainUtil academyDomainUtil;
	private final OrgRepo orgRepo;

	@Override
	@Transactional
	public ServiceResponse createActivity(ActivityDto activityDto, String academyDomain) {
		try {
			UserProfile loginUser = getCurrentUser();

			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);

			if (org.isEmpty()) {
				return ResponseBuilder.forbidden("Organisation not found for domain: " + academyDomain);
			}

			if (!activityValidation.validateCreate(activityDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_ACTIVITY_DATA);
			}
			if (activityRepository.existsByNameAndOrganisationIdAndDeletedFalse(activityDto.getName(),
					org.get().getId())) {
				return ResponseBuilder.conflict(ApiResponse.DUPLICATE_ACTIVITY_NAME);
			}
			Activity activity = ActivityMapper.mapDtoToEntity(activityDto);
			activity.setCreatedBy(loginUser);
			activity.setOrganisation(org.get());

			activityRepository.save(activity);

			return ResponseBuilder.success(ApiResponse.ACTIVITY_CREATED, HttpStatus.CREATED);

		} catch (Exception e) {
			log.error("Error creating activity: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_CREATING_ACTIVITY);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateActivity(ActivityDto activityDto, String academyDomain) {
		try {

			Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(academyDomain);

			if (org.isEmpty()) {
				return ResponseBuilder.forbidden("Organisation not found for domain: " + academyDomain);
			}

			if (!activityValidation.validateUpdate(activityDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_ACTIVITY_DATA);
			}
			if (activityRepository.existsByNameAndOrganisationIdAndDeletedFalseAndIdIsNot(activityDto.getName(),
					org.get().getId(), activityDto.getId())) {
				return ResponseBuilder.conflict(ApiResponse.DUPLICATE_ACTIVITY_NAME);
			}
			Activity existingActivity = activityRepository.findByIdAndDeletedFalse(activityDto.getId())
					.orElseThrow(() -> new EntityNotFoundException("Activity not found"));

			Activity updatedActivity = ActivityMapper.mapDtoToExistingEntity(activityDto, existingActivity);
			updatedActivity.setUpdatedBy(getCurrentUser());
			updatedActivity.setOrganisation(org.get());

			activityRepository.save(updatedActivity);
			return ResponseBuilder.success(ApiResponse.ACTIVITY_UPDATED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.ACTIVITY_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating activity: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_ACTIVITY);
		}
	}

	@Override
	@Transactional
	public ServiceResponse deleteActivity(Long id) {
		try {
			UserProfile loginUser = getCurrentUser();
			Activity activity = activityRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Activity not found"));

			activity.setDeleted(true);
			activity.setUpdatedBy(loginUser);
			activityRepository.save(activity);

			return ResponseBuilder.success(ApiResponse.ACTIVITY_DELETED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.ACTIVITY_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error deleting activity: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_DELETING_ACTIVITY);
		}
	}

	@Override
	public ServiceResponse getAllActivities(GenericFilter filter, String domainUrl) {
		try {
			log.info(ApiResponse.START_GET_ACTIVITY_LIST.getMessage());

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
			filter.setUserRole(userRole);

			ActivitySpecification specification = new ActivitySpecification(filter);
			List<Activity> activityList;
			Page<Activity> pageableContent = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = activityRepository.findAll(specification, page);
				activityList = pageableContent.getContent();
			} else {
				activityList = activityRepository.findAll(specification);
			}

			if (activityList.isEmpty()) {
				log.info(ApiResponse.ACTIVITY_NOT_FOUND.getMessage());
				return ResponseBuilder.success(ApiResponse.ACTIVITY_NOT_FOUND, HttpStatus.OK);
			}

			if (filter.isExport()) {
				try {
					List<ActivityExportDao> activityExportDaos = activityList.stream()
							.map(ActivityMapper::mapEntityToExportDao).toList();
					byte[] excelBytes = ExcelGenerator.generateExcel(activityExportDaos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "activity_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting activity data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else {
				List<ActivityDao> activityDaos = activityList.stream().map(ActivityMapper::mapEntityToDao).toList();
				if (filter.isPageable()) {
					return ResponseBuilder.success(activityDaos, ApiResponse.FETCHED_LIST, HttpStatus.OK,
							pageableContent.getTotalPages(), pageableContent.getTotalElements());
				}
				return ResponseBuilder.success(activityDaos, ApiResponse.FETCHED_LIST, HttpStatus.OK);
			}

		} catch (Exception e) {
			log.error(ApiResponse.ERROR_FETCHING_LIST.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	@Override
	public ServiceResponse getActivityById(Long id) {
		try {
			Activity activity = activityRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Activity not found"));

			return ResponseBuilder.success(ActivityMapper.mapEntityToDao(activity), ApiResponse.ACTIVITY_FETCHED,
					HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.ACTIVITY_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error fetching activity: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ACTIVITY);
		}
	}

	private UserProfile getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail userDetail = (UserDetail) authentication.getPrincipal();
		return userProfileRepository.findById(userDetail.getUserId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}