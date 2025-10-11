package com.playmotech.api.core.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AssessmentSubmissionDto;
import com.playmotech.api.core.mapper.AssessmentSubmissionMapper;
import com.playmotech.api.core.repo.AssessmentPlayerRegistrationRepository;
import com.playmotech.api.core.repo.AssessmentPlayerSubmissionRepository;
import com.playmotech.api.core.repo.AssessmentRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.AssessmentSubmissionDao;
import com.playmotech.api.core.services.AssessmentSubmissionService;
import com.playmotech.api.core.specification.AssessmentSubmissionSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.AssessmentSubmissionValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AssessmentSubmissionServiceImpl implements AssessmentSubmissionService {

	private final AssessmentRepository assessmentRepository;
	private final AssessmentPlayerSubmissionRepository submissionRepository;
	private final AssessmentPlayerRegistrationRepository registrationRepository;
	private final AssessmentSubmissionValidation submissionValidation;
	private final AcademyDomainUtil academyDomainUtil;
	private final UserProfileRepo userProfileRepository;

	@Override
	@Transactional
	public ServiceResponse submitAssessment(AssessmentSubmissionDto submissionDto) {
		try {
			if (!submissionValidation.validate(submissionDto)) {
				log.warn("Invalid assessment submission DTO");
				return ResponseBuilder.badRequest("Invalid assessment submission details");
			}

			// Check if the assessment exists
			Assessment assessment = assessmentRepository.findById(submissionDto.getAssessmentId())
					.orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

			switch (assessment.getAssessmentStatus()) {
			case DRAFT:
				log.warn("Submission rejected: Assessment {} is in DRAFT status", submissionDto.getAssessmentId());
				return ResponseBuilder.badRequest(
						"Cannot submit to an assessment that is still in DRAFT status. Please contact the administrator.");

			case CREATED:
				log.warn("Submission rejected: Assessment {} is in CREATED status", submissionDto.getAssessmentId());
				return ResponseBuilder.badRequest(
						"This assessment is not yet open for submissions. Please wait for it to be published.");

			case PUBLISHED:
				// Proceed with submission
				break;
			case CLOSED:
				log.warn("Submission rejected: Assessment {} is CLOSED", submissionDto.getAssessmentId());
				return ResponseBuilder
						.badRequest("This assessment has been closed. No further submissions are being accepted.");

			default:
				log.warn("Submission rejected: Assessment {} has an unsupported status: {}",
						submissionDto.getAssessmentId(), assessment.getAssessmentStatus());
				return ResponseBuilder.badRequest("This assessment is not currently accepting submissions. Status: "
						+ assessment.getAssessmentStatus());
			}

			// Validate registration exists and is associated with the assessment
			if (submissionDto.getRegistrationId() != null) {
				Optional<AssessmentPlayerRegistration> registrationOpt = registrationRepository
						.findById(submissionDto.getRegistrationId());

				if (registrationOpt.isEmpty()) {
					log.warn("Registration not found with ID: {}", submissionDto.getRegistrationId());
					return ResponseBuilder.notFound("Registration not found");
				}

				AssessmentPlayerRegistration registration = registrationOpt.get();
				if (!registration.getAssessment().getId().equals(submissionDto.getAssessmentId())) {
					log.warn("Registration {} is not associated with assessment {}", submissionDto.getRegistrationId(),
							submissionDto.getAssessmentId());
					return ResponseBuilder.badRequest("Registration is not associated with the specified assessment");
				}
			}

			// Check if a submission already exists for this registration (only for new
			// submissions)
			if (submissionDto.getSubmissionId() == null && submissionDto.getRegistrationId() != null) {
				Optional<AssessmentPlayerSubmission> existingSubmission = submissionRepository
						.findByRegistration_RegistrationId(submissionDto.getRegistrationId());

				if (existingSubmission.isPresent()) {
					SubmissionStatus existingStatus = existingSubmission.get().getSubmissionStatus();
					if (existingStatus == SubmissionStatus.SUBMITTED) {
						log.warn("A submitted assessment already exists for registration ID: {}",
								submissionDto.getRegistrationId());
						return ResponseBuilder.conflict("Assessment has already been submitted for this registration");
					} else if (!existingSubmission.get().getSubmissionId().equals(submissionDto.getSubmissionId())) {
						log.warn("A saved assessment exists for registration ID: {}",
								submissionDto.getRegistrationId());
						return ResponseBuilder.conflict("A saved assessment already exists for this registration");
					}
				}
			}

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			// Get player profile to set gender
			UserProfile player = userProfileRepository.findById(submissionDto.getPlayerId()).orElseThrow(
					() -> new RuntimeException("Player not found with id: " + submissionDto.getPlayerId()));

			// Set player's gender in the DTO
			submissionDto.setGender(player.getGender());

			AssessmentPlayerSubmission submission;

			if (submissionDto.getSubmissionId() != null) {
				submission = submissionRepository.findById(submissionDto.getSubmissionId()).orElse(null);

				if (submission == null) {
					// Treat as new submission
					submission = AssessmentSubmissionMapper.mapDtoToEntity(submissionDto, currentUser.getId());
				} else {
					// Check if already submitted
					if (submission.getSubmissionStatus() == SubmissionStatus.SUBMITTED) {
						log.warn("Submission with ID {} has already been submitted and cannot be modified",
								submissionDto.getSubmissionId());
						return ResponseBuilder
								.badRequest("This submission has already been completed and cannot be changed.");
					}

					// ✅ Clear existing child entities (will delete from DB due to orphanRemoval =
					// true)
					if (submission.getSubmissionParameters() != null) {
						submission.getSubmissionParameters().clear();
					}

					// ✅ Save the cleared state first (optional but recommended for clarity)
					AssessmentPlayerSubmission playerSubmission = submissionRepository.save(submission);

					// Edit flow: update existing entity
					AssessmentSubmissionMapper.mapToExistingEntity(submissionDto, playerSubmission,
							currentUser.getId());
				}
			} else {
				submission = AssessmentSubmissionMapper.mapDtoToEntity(submissionDto, currentUser.getId());
			}

			submission = submissionRepository.save(submission);

			// If submission is being submitted, update the registration status to COMPLETED
			if (submission.getSubmissionStatus() == SubmissionStatus.SUBMITTED
					&& submission.getRegistration() != null) {
				Optional<AssessmentPlayerRegistration> registrationOpt = registrationRepository
						.findById(submissionDto.getRegistrationId());
				if (registrationOpt.isEmpty()) {
					log.warn("Registration not found with ID: {}", submissionDto.getRegistrationId());
					return ResponseBuilder.notFound("Registration not found");
				}
				AssessmentPlayerRegistration registration = registrationOpt.get();
				if (registration.getPlayerStatus() != PlayerStatus.COMPLETED) {
					registration.setPlayerStatus(PlayerStatus.COMPLETED);
					registrationRepository.save(registration);
					log.info("Updated player status to COMPLETED for registration ID: {}",
							registration.getRegistrationId());
				}
			}

			String message = submission.getSubmissionStatus() == SubmissionStatus.SUBMITTED
					? "Assessment submitted successfully"
					: "Assessment saved successfully";

			return ResponseBuilder.success(message);

		} catch (Exception ex) {
			log.error("Unexpected error during assessment submission", ex);
			return ResponseBuilder.internalServerError("Unexpected error during assessment submission");
		}
	}

	@Override
	public ServiceResponse getSubmissionById(String submissionId) {
		try {
			Optional<AssessmentPlayerSubmission> optionalSubmission = submissionRepository.findById(submissionId);
			if (optionalSubmission.isEmpty()) {
				log.warn("Submission not found with ID {}", submissionId);
				return ResponseBuilder.notFound("Submission not found");
			}

			AssessmentSubmissionDao dao = AssessmentSubmissionMapper.mapEntityToDao(optionalSubmission.get());
			return ResponseBuilder.success(dao, "Submission fetched successfully");

		} catch (Exception ex) {
			log.error("Error fetching submission with ID {}", submissionId, ex);
			return ResponseBuilder.internalServerError("Unexpected error fetching submission");
		}
	}

	@Override
	public ServiceResponse getSubmissionByRegistrationId(String registrationId) {
		try {
			Optional<AssessmentPlayerSubmission> optionalSubmission = submissionRepository
					.findByRegistration_RegistrationId(registrationId);
			if (optionalSubmission.isEmpty()) {
				log.warn("Submission not found for registration ID {}", registrationId);
				return ResponseBuilder.success(null, "No submission found for the given registration ID");
			}

			AssessmentSubmissionDao dao = AssessmentSubmissionMapper.mapEntityToDao(optionalSubmission.get());
			return ResponseBuilder.success(dao, "Submission fetched successfully");

		} catch (Exception ex) {
			log.error("Error fetching submission for registration ID {}", registrationId, ex);
			return ResponseBuilder.internalServerError("Unexpected error fetching submission by registration ID");
		}
	}

	@Override
	public ServiceResponse getSubmissions(GenericFilter filter) {
		try {
			UserProfile currentUser = academyDomainUtil.getCurrentUser();
			List<String> academyIds = null;

			if (!academyDomainUtil.isSuperAdmin(currentUser.getId())) {
				academyIds = academyDomainUtil.getAcademyByUrl(filter.getDomainUrl()).stream().map(Academy::getId)
						.toList();

				if (academyIds.isEmpty()) {
					log.info("No academies found for domain: " + filter.getDomainUrl());
				}
			}

			AssessmentSubmissionSpecification spec = new AssessmentSubmissionSpecification(filter, academyIds);
			List<AssessmentPlayerSubmission> submissions;
			Page<AssessmentPlayerSubmission> pageResult = null;

			if (filter.isPageable()) {
				PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageResult = submissionRepository.findAll(spec, page);
				submissions = pageResult.getContent();
			} else {
				submissions = submissionRepository.findAll(spec);
			}

			if (submissions.isEmpty()) {
				log.info("No submissions found");
				return ResponseBuilder.success(Collections.emptyList(), "No submissions found");
			}

			List<AssessmentSubmissionDao> daos = submissions.stream().map(AssessmentSubmissionMapper::mapEntityToDao)
					.collect(Collectors.toList());

			if (filter.isPageable() && pageResult != null) {
				return ResponseBuilder.success(daos, ApiResponse.FETCHED_LIST, pageResult.getTotalPages(),
						pageResult.getTotalElements());
			}

			return ResponseBuilder.success(daos, "Submissions fetched successfully");

		} catch (Exception ex) {
			log.error("Unexpected error while fetching submissions", ex);
			return ResponseBuilder.internalServerError("Unexpected error while fetching submissions");
		}
	}
}
