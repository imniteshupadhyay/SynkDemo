package com.playmotech.api.core.services.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;
import com.playmotech.api.core.dao_postgres.TrialFeedback;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.dto.TrialFeedbackDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.mapper.TrialFeedbackMapper;
import com.playmotech.api.core.mapper.TrialMapper;
import com.playmotech.api.core.repo.LeadRepo;
import com.playmotech.api.core.repo.TrialRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.TrialDao;
import com.playmotech.api.core.response.dao.TrialExportDao;
import com.playmotech.api.core.services.TrialService;
import com.playmotech.api.core.specification.TrialSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.TrialValidation;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class TrialServiceImpl implements TrialService {

	private final TrialRepository trialRepository;
	private final TrialValidation trialValidation;
	private final AcademyDomainUtil academyDomainUtil;
	private final UserProfileRepo userProfileRepo;

	private final LeadRepo leadRepo;

	@Override
	@Transactional
	public ServiceResponse createTrial(TrialDto trialDto) {
		try {
			UserProfile loginUser = getCurrentUser();
			if (!trialValidation.validateCreate(trialDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_TRIAL_DATA);
			}

			Trial trial = TrialMapper.mapDtoToEntity(trialDto);
			trial.setCreatedBy(loginUser);

			trialRepository.save(trial);

			updateLeadTrialStatus(trialDto.getLeadId(), trial.getStatus().toString());
			return ResponseBuilder.success(ApiResponse.TRIAL_CREATED, HttpStatus.CREATED);

		} catch (Exception e) {
			log.error("Error creating trial: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_CREATING_TRIAL);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateTrial(TrialDto trialDto) {
		try {

			if (!trialValidation.validateUpdate(trialDto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_TRIAL_DATA);
			}

			Trial existingTrial = trialRepository
					.findByIdAndDeletedFalseAndStatus(trialDto.getId(), TrialStatus.PENDING)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			Trial updatedTrial = TrialMapper.mapDtoToExistingEntity(trialDto, existingTrial);
			updatedTrial.setUpdatedBy(getCurrentUser());

			updateLeadTrialStatus(trialDto.getLeadId(), updatedTrial.getStatus().toString());

			trialRepository.save(updatedTrial);
			return ResponseBuilder.success(TrialMapper.mapEntityToDao(updatedTrial), ApiResponse.TRIAL_UPDATED,
					HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating trial: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL);
		}
	}

	@Override
	@Transactional
	public ServiceResponse deleteTrial(String id) {
		try {
			UserProfile loginUser = getCurrentUser();
			Trial trial = trialRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			trial.setDeleted(true);
			trial.setUpdatedBy(loginUser);
			trialRepository.save(trial);

			return ResponseBuilder.success(ApiResponse.TRIAL_DELETED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error deleting trial: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_DELETING_TRIAL);
		}
	}

	@Override
	public ServiceResponse getAllTrials(GenericFilter filter, String domainUrl) {
		try {
			log.info(ApiResponse.START_GET_TRIAL_LIST.getMessage());

			// String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
			// Fetch user profile to determine role
			UserProfile user = userProfileRepo.findById(filter.getUserId())
					.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "User not found"));

			String role;
			if (user.getRole() == Role.SUPER_ADMIN) {
				role = Role.SUPER_ADMIN.name();
			} else if (user.getRole() == Role.USER) {
				// Determine based on userType
				role = (user.getUserType() == UserType.PLAYER) ? UserType.PLAYER.name() : UserType.COACH.name();
			} else {
				role = user.getRole().name();
			}
			filter.setUserRole(role);

			TrialSpecification specification = new TrialSpecification(filter);
			List<Trial> trialList;
			Page<Trial> pageableContent = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = trialRepository.findAll(specification, page);
				trialList = pageableContent.getContent();
			} else {
				trialList = trialRepository.findAll(specification);
			}

			if (trialList.isEmpty()) {
				log.info(ApiResponse.TRIAL_NOT_FOUND.getMessage());
				return ResponseBuilder.success(ApiResponse.TRIAL_NOT_FOUND, HttpStatus.OK);
			}

			if (filter.isExport()) {

				try {
					List<TrialExportDao> trialExportDaos = trialList.stream().map(TrialMapper::mapEntityToExportDao)
							.toList();
					byte[] excelBytes = ExcelGenerator.generateExcel(trialExportDaos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "trial_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else {
				List<TrialDao> trialDaos = trialList.stream().map(TrialMapper::mapEntityToDao).toList();
				if (filter.isPageable()) {
					return ResponseBuilder.success(trialDaos, ApiResponse.FETCHED_LIST, HttpStatus.OK,
							pageableContent.getTotalPages(), pageableContent.getTotalElements());
				}
				return ResponseBuilder.success(trialDaos, ApiResponse.FETCHED_LIST, HttpStatus.OK);
			}

		} catch (Exception e) {
			log.error(ApiResponse.ERROR_FETCHING_LIST.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	@Override
	public ServiceResponse getTrialById(String id) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			return ResponseBuilder.success(TrialMapper.mapEntityToDao(trial), ApiResponse.TRIAL_FETCHED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error fetching trial: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_TRIAL);
		}
	}

	// @Override
	// @Transactional
	// public ServiceResponse updateTrialStatus(String id, Trial.TrialStatus status)
	// {
	// try {
	// Trial trial = trialRepository.findByIdAndDeletedFalse(id)
	// .orElseThrow(() -> new EntityNotFoundException("Trial not found"));
	//
	// trial.setStatus(status);
	// trial.setUpdatedBy(getCurrentUser());
	// trialRepository.save(trial);
	//
	// return ResponseBuilder.success(TrialMapper.mapEntityToDao(trial),
	// ApiResponse.TRIAL_STATUS_UPDATED, HttpStatus.OK);
	//
	// } catch (EntityNotFoundException e) {
	// return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
	// } catch (Exception e) {
	// log.error("Error updating trial status: {}", e.getMessage());
	// return
	// ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL_STATUS);
	// }
	// }

	@Override
	@Transactional
	public ServiceResponse markAsCompleted(String id) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			trial.setCompleted(true);
			trial.setUpdatedBy(getCurrentUser());
			trialRepository.save(trial);

			return ResponseBuilder.success(TrialMapper.mapEntityToDao(trial), ApiResponse.TRIAL_COMPLETED,
					HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error marking trial as completed: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL);
		}
	}

	private UserProfile getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail userDetail = (UserDetail) authentication.getPrincipal();
		return userProfileRepo.findById(userDetail.getUserId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Override
	public ServiceResponse addPedningReason(String id, String reason) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			UserProfile loginUser = getCurrentUser();
			if (!trialValidation.isValidReason(reason) || trial.isCompleted()) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_TRIAL_DATA);
			}

			// Set the new feedback to the trial
			trial.setReason(reason);
			trial.setStatus(TrialStatus.PENDING);
			trial.setUpdatedBy(loginUser);

			// Save the updated trial with new feedback
			trialRepository.save(trial);

			return ResponseBuilder.success(ApiResponse.TRIAL_UPDATED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating trial status: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL);
		}
	}

	@Override
	public ServiceResponse addToPending(String id) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalseAndStatus(id, TrialStatus.REJECTED)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			UserProfile loginUser = getCurrentUser();

			trial.setCompleted(false);
			trial.setStatus(TrialStatus.PENDING);
			trial.setUpdatedBy(loginUser);

			// Save the updated trial with new feedback
			trialRepository.save(trial);

			return ResponseBuilder.success(ApiResponse.TRIAL_UPDATED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating trial status: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL);
		}
	}

	@Override
	public ServiceResponse addTrialFeedback(String id, TrialFeedbackDto dto) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalse(id)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			UserProfile loginUser = getCurrentUser();
			if (!trialValidation.validateFeedback(dto)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_TRIAL_FEEDBACK_DATA);
			}

			// Check if feedback already exists, and remove it if so
			List<TrialFeedback> existingFeedback = trial.getFeedback();
			// if (existingFeedback != null) {
			// // Remove the previous feedback from the trial
			// trial.setFeedback(null);
			// trialFeedbackRepo.delete(existingFeedback); // Assuming you have a repository
			// for feedback
			// }

			// Map the new DTO to an entity
			TrialFeedback feedback = TrialFeedbackMapper.mapDtoToEntity(dto, trial);

			existingFeedback.add(feedback);

			// Set the new feedback to the trial
			trial.setFeedback(existingFeedback);

			// Set trial status and other properties
			trial.setStatus(TrialStatus.valueOf(dto.getStatus().toUpperCase()));
			trial.setUpdatedBy(loginUser);
			trial.setCompleted(true);

			// Save the updated trial with new feedback
			trialRepository.save(trial);

			updateLeadTrialStatus(trial.getLeadId(), trial.getStatus().toString());

			return ResponseBuilder.success(TrialMapper.mapEntityToDao(trial), ApiResponse.TRIAL_FEEDBACK_UPDATED,
					HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating trial status: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL_FEEDBAK);
		}
	}

	@Override
	public ServiceResponse updateTrialTime(String id, LocalDate trialDate, LocalTime trialTime) {
		try {
			Trial trial = trialRepository.findByIdAndDeletedFalseAndStatus(id, TrialStatus.PENDING)
					.orElseThrow(() -> new EntityNotFoundException("Trial not found"));

			UserProfile loginUser = getCurrentUser();
			if (!trialValidation.isValidDate(trialDate) && !trialValidation.isValidTime(trialTime)) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_TRIAL_DATA);
			}

			// Set the new feedback to the trial
			trial.setTrailDate(trialDate);
			trial.setTrailTime(trialTime);
			trial.setStatus(TrialStatus.SCHEDULED);

			trial.setUpdatedBy(loginUser);

			// Save the updated trial with new feedback
			trialRepository.save(trial);

			if (trial.getLeadId() != null) {
				updateLeadTrialStatus(trial.getLeadId(), trial.getStatus().toString());
			}

			return ResponseBuilder.success(ApiResponse.TRIAL_UPDATED, HttpStatus.OK);

		} catch (EntityNotFoundException e) {
			return ResponseBuilder.notFound(ApiResponse.TRIAL_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error updating trial status: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_UPDATING_TRIAL);
		}
	}

	@Override
	public List<Trial> findTrialsByLeadIds(List<String> leadIds) {
		return trialRepository.findByLeadIdIn(leadIds);
	}

	@Override
	public void saveAllTrials(List<Trial> trials) {
		trialRepository.saveAll(trials);
	}

	private void updateLeadTrialStatus(String leadId, String trialStatus) {
		if (leadId != null) {
			leadRepo.findById(leadId).ifPresent(leads -> {
				leads.setTrialStatus(trialStatus);
				leadRepo.save(leads);
			});
		}
	}

}
