package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AssessmentDto;
import com.playmotech.api.core.dto.AssessmentParameterDto;
import com.playmotech.api.core.mapper.AssessmentMapper;
import com.playmotech.api.core.repo.AssessmentParameterConfigRepository;
import com.playmotech.api.core.repo.AssessmentPlayerRegistrationRepository;
import com.playmotech.api.core.repo.AssessmentPlayerSubmissionRepository;
import com.playmotech.api.core.repo.AssessmentRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.AssessmentDao;
import com.playmotech.api.core.response.dao.AssessmentParameterConfigDao;
import com.playmotech.api.core.services.AssessmentResultService;
import com.playmotech.api.core.services.AssessmentService;
import com.playmotech.api.core.specification.AssessmentSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.AssessmentValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

	private static final Map<AssessmentStatus, List<String>> ACTIONS_BY_STATUS = Map.of(AssessmentStatus.DRAFT,
			List.of("Save", "View", "Edit"), AssessmentStatus.CREATED, List.of("Publish", "View", "Edit"),
			AssessmentStatus.PUBLISHED, List.of("Close", "View Registrations", "View Progress", "View Summary", "View"),
			AssessmentStatus.CLOSED, List.of("Publish", "View Registrations", "View Progress", "View Summary", "View"));

	private final AssessmentRepository assessmentRepository;
	private final AssessmentParameterConfigRepository parameterConfigRepository;
	private final AssessmentPlayerRegistrationRepository registrationRepository;
	private final AssessmentPlayerSubmissionRepository submissionRepository;

	private final AcademyDomainUtil academyDomainUtil;
	private final AssessmentValidation assessmentValidation;
	private final AssessmentResultService resultService;

	@Override
	@Transactional
	public ServiceResponse createAssessment(AssessmentDto assessmentDto) {
		try {
			if (!assessmentValidation.validate(assessmentDto)) {
				log.warn("Validation failed for assessment");
				return ResponseBuilder.badRequest("Invalid assessment details");
			}
			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			Role role = currentUser.getRole();

			if (role != Role.ACADEMY_OWNER && role != Role.SUPER_ADMIN) {
				log.warn("User is not authorized to create assessment");
				return ResponseBuilder.unauthorized("User is not authorized to create assessment");
			}

			// Create parameters from DTO if they don't exist
			createParametersFromDto(assessmentDto);

			Assessment assessment = AssessmentMapper.mapDtoToEntity(assessmentDto, currentUser.getId());

			if (assessment == null) {
				log.error("Mapping failed: Assessment entity is null");
				return ResponseBuilder.badRequest("Failed to process assessment data");
			}

			assessmentRepository.save(assessment);
			return ResponseBuilder.success("Assessment created successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while creating assessment", ex);
			return ResponseBuilder.internalServerError("Unexpected error while creating assessment");
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateAssessment(String assessmentId, AssessmentDto assessmentDto) {
		try {
			// Get current user for audit info
			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			Role role = currentUser.getRole();
			if (role != Role.ACADEMY_OWNER && role != Role.SUPER_ADMIN) {
				log.warn("User is not authorized to update assessment");
				return ResponseBuilder.unauthorized("User is not authorized to update assessment");
			}

			if (!assessmentValidation.validateForUpdate(assessmentDto)) {
				log.warn("Validation failed for assessment update");
				return ResponseBuilder.badRequest("Invalid assessment details");
			}

			Assessment existing = assessmentRepository.findById(assessmentId).orElse(null);
			if (existing == null) {
				log.warn("Assessment not found with ID: {}", assessmentId);
				return ResponseBuilder.badRequest("Assessment not found");
			}

			// ✅ Clear existing child entities (will delete from DB due to orphanRemoval =
			// true)
			if (existing.getParameters() != null) {
				existing.getParameters().clear();
			}

			if (existing.getAcademyMappings() != null) {
				existing.getAcademyMappings().clear();
			}

			// ✅ Save the cleared state first (optional but recommended for clarity)
			assessmentRepository.save(existing);

			// Optional: if your DTO contains new parameters data not yet persisted, handle
			// that here
			createParametersFromDto(assessmentDto);

			// ✅ Map DTO to updated entity (new parameters and academy mappings)
			Assessment updated = AssessmentMapper.mapDtoToExistingEntity(assessmentDto, existing, currentUser.getId());

			if (updated == null) {
				log.error("Mapping failed: Updated Assessment entity is null");
				return ResponseBuilder.internalServerError("Failed to process assessment update data");
			}

			// ✅ Save updated entity
			assessmentRepository.save(updated);

			return ResponseBuilder.success("Assessment updated successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while updating assessment with ID {}", assessmentId, ex);
			return ResponseBuilder.internalServerError("Unexpected error while updating assessment");
		}
	}

	@Override
	public ServiceResponse getAssessmentById(String assessmentId) {
		try {
			return assessmentRepository
					.findById(assessmentId).map(assessment -> ResponseBuilder
							.success(AssessmentMapper.mapEntityToDao(assessment), "Assessment fetched successfully"))
					.orElseGet(() -> {
						log.warn("Assessment not found with ID {}", assessmentId);
						return ResponseBuilder.notFound("Assessment not found");
					});
		} catch (Exception ex) {
			log.error("Unexpected error while fetching assessment with ID {}", assessmentId, ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching assessment");
		}
	}

	@Override
	public ServiceResponse getPublishedAssessments(String domainUrl) {
		try {
			UserProfile currentUser = academyDomainUtil.getCurrentUser();
			boolean isSuperAdmin = academyDomainUtil.isSuperAdmin(currentUser.getId());

			List<AssessmentStatus> excludedStatuses = List.of(AssessmentStatus.DRAFT, AssessmentStatus.CREATED);

			List<Assessment> assessments;

			if (isSuperAdmin) {
				assessments = assessmentRepository.findByAssessmentStatusNotIn(excludedStatuses,
						Sort.by(Sort.Direction.DESC, "createdOn"));
			} else {
				List<Academy> academies = academyDomainUtil.getAcademyByUrl(domainUrl);

				if (academies.isEmpty()) {
					return ResponseBuilder.notFound("No academies found for domain: " + domainUrl);
				}

				List<String> academyIds = academies.stream().map(Academy::getId).toList();

				// Get both academy-specific and for-all-academies assessments in a single query
				assessments = assessmentRepository
						.findByAcademyMappings_Academy_IdInOrForAllAcademiesTrueAndAssessmentStatusNotIn(academyIds,
								excludedStatuses, Sort.by(Sort.Direction.DESC, "createdOn"))
						.stream().distinct().collect(Collectors.toList());
			}
			if (assessments.isEmpty()) {
				log.info("No published assessments found");
				return ResponseBuilder.success(Collections.emptyList(), "No published assessments found");
			}

			List<AssessmentDao> daos = assessments.stream().map(AssessmentMapper::mapEntityToDao).toList();

			return ResponseBuilder.success(daos, "Published assessments fetched successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while fetching published assessments", ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching published assessments");
		}
	}

	@Override
	public ServiceResponse getClosedAssessments(String domainUrl) {
		try {
			UserProfile currentUser = academyDomainUtil.getCurrentUser();
			boolean isSuperAdmin = academyDomainUtil.isSuperAdmin(currentUser.getId());

			List<AssessmentStatus> includedStatuses = List.of(AssessmentStatus.CLOSED);

			List<Assessment> assessments;

			if (isSuperAdmin) {
				assessments = assessmentRepository.findByAssessmentStatusIn(includedStatuses,
						Sort.by(Sort.Direction.DESC, "createdOn"));
			} else {
				List<Academy> academies = academyDomainUtil.getAcademyByUrl(domainUrl);

				if (academies.isEmpty()) {
					return ResponseBuilder.notFound("No academies found for domain: " + domainUrl);
				}

				List<String> academyIds = academies.stream().map(Academy::getId).toList();

				// Get both academy-specific and for-all-academies assessments in a single query
				assessments = assessmentRepository
						.findByAcademyMappings_Academy_IdInOrForAllAcademiesTrueAndAssessmentStatusIn(academyIds,
								includedStatuses, Sort.by(Sort.Direction.DESC, "createdOn"))
						.stream().distinct().collect(Collectors.toList());
			}
			if (assessments.isEmpty()) {
				log.info("No published assessments found");
				return ResponseBuilder.success(Collections.emptyList(), "No published assessments found");
			}

			if (assessments.isEmpty()) {
				log.info("No published assessments found");
				return ResponseBuilder.success(Collections.emptyList(), "No published assessments found");
			}

			List<AssessmentDao> daos = assessments.stream().map(AssessmentMapper::mapEntityToDao).toList();

			return ResponseBuilder.success(daos, "Published assessments fetched successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while fetching published assessments", ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching published assessments");
		}
	}

	@Override
	public ServiceResponse getAssessments(GenericFilter filter) {
		try {
			AssessmentSpecification spec = new AssessmentSpecification(filter);
			List<Assessment> assessments;
			Page<Assessment> pageResult = null;

			if (filter.isPageable()) {
				pageResult = assessmentRepository.findAll(spec,
						PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize()));
				assessments = pageResult.getContent();
			} else {
				assessments = assessmentRepository.findAll(spec);
			}

			if (assessments.isEmpty()) {
				log.info("No assessments found");
				return ResponseBuilder.success(Collections.emptyList(), "No assessments found");
			}

			List<AssessmentDao> daos = assessments.stream().map(AssessmentMapper::mapEntityToDao)
					.collect(Collectors.toList());

			if (filter.isPageable() && pageResult != null) {
				return ResponseBuilder.success(daos, ApiResponse.FETCHED_LIST, pageResult.getTotalPages(),
						pageResult.getTotalElements());
			}

			return ResponseBuilder.success(daos, "Assessments fetched successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while fetching assessments", ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching assessments");
		}
	}

	@Override
	@Transactional
	public ServiceResponse updateAssessmentStatus(String assessmentId, AssessmentStatus newStatus) {
		try {
			// Get current user for audit
			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			// Check permissions
			if (currentUser.getRole() != Role.ACADEMY_OWNER && currentUser.getRole() != Role.SUPER_ADMIN) {
				log.warn("User is not authorized to update assessment status");
				return ResponseBuilder.unauthorized("User is not authorized to update assessment status");
			}

			// Find the assessment
			Assessment assessment = assessmentRepository.findById(assessmentId)
					.orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + assessmentId));

			// If status is not changing, return success
			if (assessment.getAssessmentStatus() == newStatus) {
				return ResponseBuilder.success("Assessment status is already " + newStatus);
			}

			// Check if there's at least one registration when closing registration
			if (newStatus == AssessmentStatus.CLOSED) {
				long registrationCount = registrationRepository.countByAssessment_Id(assessmentId);
				if (registrationCount == 0) {
					return ResponseBuilder.badRequest(
							"Cannot close registration: No players have registered for this assessment yet");
				}

				return handleAssessmentClosure(assessment, currentUser.getId());

			}

			// For other status updates, just update the status
			assessment.setAssessmentStatus(newStatus);
			assessmentRepository.save(assessment);

			return ResponseBuilder.success("Assessment status updated to " + newStatus);

		} catch (IllegalArgumentException e) {
			log.error("Invalid request: {}", e.getMessage());
			return ResponseBuilder.badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("Error updating assessment status: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError("Error updating assessment status: " + e.getMessage());
		}
	}

	/**
	 * Handles the logic when an assessment is being closed
	 */
	private ServiceResponse handleAssessmentClosure(Assessment assessment, String updatedBy) {
		// Get all registered and completed players for this assessment
		List<PlayerStatus> statusesToCheck = Arrays.asList(PlayerStatus.REGISTERED, PlayerStatus.COMPLETED);
		List<AssessmentPlayerRegistration> registrations = registrationRepository
				.findByAssessment_IdAndPlayerStatusIn(assessment.getId(), statusesToCheck);

		// Check if all submissions are in SUBMITTED status
		List<String> playersWithUnsavedSubmissions = new ArrayList<>();

		for (AssessmentPlayerRegistration registration : registrations) {
			Optional<AssessmentPlayerSubmission> submissionOpt = submissionRepository
					.findByRegistration_RegistrationId(registration.getRegistrationId());

			if (submissionOpt.isPresent()) {
				AssessmentPlayerSubmission submission = submissionOpt.get();
				if (submission.getSubmissionStatus() == AssessmentPlayerSubmission.SubmissionStatus.SAVED) {
					playersWithUnsavedSubmissions.add(registration.getPlayer().getDisplayName());
				}
			}
		}

		if (!playersWithUnsavedSubmissions.isEmpty()) {
			String errorMessage = String.format(
					"Cannot close assessment: %d player(s) have unsaved submissions. "
							+ "Please ensure all submissions are submitted before closing the assessment.",
					playersWithUnsavedSubmissions.size());
			return ResponseBuilder.badRequest(errorMessage);
		}

		// Update assessment status to CLOSED
		assessment.setAssessmentStatus(AssessmentStatus.CLOSED);

		assessment = assessmentRepository.save(assessment);

		// Mark no-shows and calculate scores asynchronously
		markNoShowsAndCalculateScores(assessment, registrations);

		return ResponseBuilder.success("Assessment closed successfully. Processing player results...");
	}

	/**
	 * Marks no-show players and calculates scores asynchronously
	 */
	@Async
	protected void markNoShowsAndCalculateScores(Assessment assessment,
			List<AssessmentPlayerRegistration> registrations) {
		try {
			for (AssessmentPlayerRegistration registration : registrations) {
				// Check if player has a submission
				Optional<AssessmentPlayerSubmission> submissionOpt = submissionRepository
						.findByRegistration_RegistrationId(registration.getRegistrationId());

				if (submissionOpt.isEmpty()) {
					// No submission found, mark as NO_SHOW
					registration.setPlayerStatus(PlayerStatus.NO_SHOW);
					registrationRepository.save(registration);
				}
				// Note: For players with submissions, their status remains REGISTERED
				// as they've already been processed during submission
			}

			// Trigger score calculation (implementation depends on your scoring logic)
			resultService.calculateScores(assessment.getId());

		} catch (Exception e) {
			log.error("Error in async processing after assessment closure: {}", e.getMessage(), e);
		}
	}

	@Override
	public ServiceResponse getActions() {
		try {
			Map<String, List<String>> actionsMap = ACTIONS_BY_STATUS.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

			return ResponseBuilder.success(actionsMap, "Actions fetched successfully");
		} catch (Exception ex) {
			log.error("Unexpected error while fetching actions", ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching actions");
		}
	}

	@Override
	public ServiceResponse getParameterConfigs(String sport) {
		try {
			if (sport == null) {
				log.warn("Invalid parameter: sport is required");
				return ResponseBuilder.badRequest("Sport is required");
			}

			// Convert string parameters to enums
			Sports sportEnum;

			try {
				sportEnum = Sports.valueOf(sport.toUpperCase());
			} catch (IllegalArgumentException e) {
				log.warn("Invalid enum value provided. sport: {}", sport, e);
				return ResponseBuilder.badRequest("Invalid sport value");
			}

			log.debug("Fetching parameter configs for sport: {}", sport);

			List<AssessmentParameterConfig> configs = parameterConfigRepository.findBySport(sportEnum);

			if (configs == null || configs.isEmpty()) {
				log.info("No parameter configurations found for sport: {}", sport);
				return ResponseBuilder.success(Collections.emptyList(), "No parameter configurations found");
			}

			List<AssessmentParameterConfigDao> result = configs.stream().map(this::mapToDao)
					.collect(Collectors.toList());

			log.debug("Found {} parameter configurations", result.size());
			return ResponseBuilder.success(result, "Parameter configurations fetched successfully");

		} catch (Exception ex) {
			log.error("Error while fetching parameter configurations", ex);
			return ResponseBuilder
					.internalServerError("Error while fetching parameter configurations: " + ex.getMessage());
		}
	}

	/**
	 * Maps an AssessmentParameterConfig entity to a DTO.
	 */
	private AssessmentParameterConfigDao mapToDao(AssessmentParameterConfig config) {
		AssessmentParameterConfigDao dao = new AssessmentParameterConfigDao();
		BeanUtils.copyProperties(config, dao);
		return dao;
	}

	/**
	 * Creates parameters from the DTO if they don't already exist for the given
	 * sport, age category, and gender.
	 * 
	 * @param assessmentDto The assessment DTO containing the parameters to create
	 */
	private void createParametersFromDto(AssessmentDto assessmentDto) {
		if (assessmentDto == null || assessmentDto.getParameters() == null || assessmentDto.getParameters().isEmpty()) {
			log.info("No parameters provided in the DTO");
			return;
		}

		Sports sport = assessmentDto.getSport();

		if (sport == null) {
			log.warn("Cannot create parameters: sport is null in the DTO");
			return;
		}

		List<AssessmentParameterConfig> parametersToCreate = new ArrayList<>();

		for (AssessmentParameterDto paramDto : assessmentDto.getParameters()) {
			if (paramDto == null || paramDto.getParameterName() == null) {
				continue; // Skip invalid parameter DTOs
			}

			// Check if parameter with this name already exists for this sport
			boolean exists = parameterConfigRepository.findBySport(sport).stream()
					.anyMatch(p -> p.getParameterName().equalsIgnoreCase(paramDto.getParameterName().trim()));

			if (!exists) {
				// Create new parameter configuration with mapped fields
				AssessmentParameterConfig config = new AssessmentParameterConfig();
				// Map fields from DTO to config
				config.setParameterName(paramDto.getParameterName().trim());
				config.setParameterDescription(paramDto.getParameterDescription());
				config.setParameterType(paramDto.getParameterType());
				config.setUnitType(paramDto.getUnitType());
				config.setComparisonType(paramDto.getComparisonType());
				// Set the sport from the assessment
				config.setSport(sport);
				config.setDeleted(false);
				parametersToCreate.add(config);
			}
		}

		// Save all new parameters if any
		if (!parametersToCreate.isEmpty()) {
			List<AssessmentParameterConfig> savedConfigs = parameterConfigRepository.saveAll(parametersToCreate);
			log.info("Created {} new parameters for sport: {}", savedConfigs.size(), sport);
		}

		// Get all configs for this sport to map to parameters
		List<AssessmentParameterConfig> allConfigs = parameterConfigRepository.findBySport(sport);

		// Create a map of parameter name to config for quick lookup
		Map<String, AssessmentParameterConfig> configMap = allConfigs.stream()
				.collect(Collectors.toMap(config -> config.getParameterName().toLowerCase().trim(), config -> config,
						(existing, replacement) -> existing)); // In case of duplicates, keep the existing one

		// Update parameters in the DTO with the corresponding config
		for (AssessmentParameterDto paramDto : assessmentDto.getParameters()) {
			if (paramDto != null && paramDto.getParameterName() != null) {
				String paramName = paramDto.getParameterName().toLowerCase().trim();
				AssessmentParameterConfig config = configMap.get(paramName);
				if (config != null) {
					paramDto.setParameterConfigId(config.getParameterId());
				}
			}
		}
	}
}
