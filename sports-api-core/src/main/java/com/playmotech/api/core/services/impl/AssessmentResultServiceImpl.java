package com.playmotech.api.core.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmissionParameter;
import com.playmotech.api.core.repo.AssessmentGlobalScoresViewRepository;
import com.playmotech.api.core.repo.AssessmentPlayerRegistrationRepository;
import com.playmotech.api.core.repo.AssessmentPlayerSubmissionRepository;
import com.playmotech.api.core.repo.AssessmentRepository;
import com.playmotech.api.core.repo.AssessmentScoresViewRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AssessmentResultService;
import com.playmotech.api.core.specification.AssessmentGlobalScoresViewSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.AssessmentGlobalScoresView;
import com.playmotech.api.core.views.AssessmentScoresView;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AssessmentResultServiceImpl implements AssessmentResultService {

	private final AssessmentRepository assessmentRepository;
	private final AssessmentPlayerRegistrationRepository registrationRepository;
	private final AssessmentPlayerSubmissionRepository submissionRepository;

	private final AssessmentScoresViewRepository scoresViewRepository;

	private final AssessmentGlobalScoresViewRepository globalScoresViewRepository;

	private final AcademyDomainUtil academyDomainUtil;

	@Override
	public ServiceResponse getKpis(String assessmentId) {
		try {
			// Get the assessment to verify it exists
			Assessment assessment = assessmentRepository.findById(assessmentId)
					.orElseThrow(() -> new IllegalArgumentException("Assessment not found" + assessmentId));

			// Get all registrations for this assessment
			List<AssessmentPlayerRegistration> allRegistrations = registrationRepository
					.findByAssessment_Id(assessmentId);

			// Count participants by status
			long registeredCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.REGISTERED).count();

			long completedCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.COMPLETED).count();

			long noShowCount = allRegistrations.stream().filter(reg -> reg.getPlayerStatus() == PlayerStatus.NO_SHOW)
					.count();

			long withdrawnCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.WITHDRAWN).count();

			// Total participants includes only REGISTERED and COMPLETED statuses
			int totalParticipants = (int) (registeredCount + completedCount);

			// Get all submissions for this assessment
			List<AssessmentPlayerSubmission> submissions = submissionRepository.findByAssessment_Id(assessmentId);

			// Filter only SUBMITTED submissions
			List<AssessmentPlayerSubmission> submittedSubmissions = submissions.stream()
					.filter(sub -> sub.getSubmissionStatus() == SubmissionStatus.SUBMITTED)
					.collect(Collectors.toList());

			// Count submissions with results (non-empty parameters)
			long resultsRecorded = submittedSubmissions.stream()
					.filter(sub -> sub.getSubmissionParameters() != null && !sub.getSubmissionParameters().isEmpty())
					.count();

			// Count submissions with video URLs
			long videosUploaded = submittedSubmissions.stream()
					.filter(sub -> sub.getSubmissionParameters() != null && sub.getSubmissionParameters().stream()
							.anyMatch(param -> param.getVideoUrl() != null && !param.getVideoUrl().trim().isEmpty()))
					.count();

			// Calculate completion rate (percentage of total registrations with submitted
			// submissions)
			double completionRate = totalParticipants > 0
					? (double) submittedSubmissions.size() / totalParticipants * 100
					: 0;

			// Prepare KPI data
			Map<String, Object> kpiData = new HashMap<>();
			kpiData.put("totalParticipants", totalParticipants);
			kpiData.put("registeredParticipants", registeredCount);
			kpiData.put("completedParticipants", completedCount);
			kpiData.put("noShowParticipants", noShowCount);
			kpiData.put("withdrawnParticipants", withdrawnCount);
			kpiData.put("resultsRecorded", resultsRecorded);
			kpiData.put("videosUploaded", videosUploaded);
			kpiData.put("completionRate", Math.round(completionRate * 100.0) / 100.0); // Round to 2 decimal places

			return ResponseBuilder.success(kpiData, "Assessment results fetched successfully");

		} catch (Exception e) {
			log.error("Unexpected error while fetching assessment results for assessment {}", assessmentId, e);
			return ResponseBuilder.internalServerError("Unexpected error while fetching assessment results");
		}
	}

	@Override
	public ServiceResponse getResults(String assessmentId) {
		try {
			// Get the assessment to verify it exists
			Assessment assessment = assessmentRepository.findById(assessmentId)
					.orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + assessmentId));

			// Get all registrations for this assessment
			List<AssessmentPlayerRegistration> allRegistrations = registrationRepository
					.findByAssessment_Id(assessmentId);

			// Count participants by status
			long registeredCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.REGISTERED).count();

			long completedCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.COMPLETED).count();

			long noShowCount = allRegistrations.stream().filter(reg -> reg.getPlayerStatus() == PlayerStatus.NO_SHOW)
					.count();

			long withdrawnCount = allRegistrations.stream()
					.filter(reg -> reg.getPlayerStatus() == PlayerStatus.WITHDRAWN).count();

			// Total participants includes only REGISTERED and COMPLETED statuses
			int totalParticipants = (int) (registeredCount + completedCount);

			// Get all submissions for this assessment
			List<AssessmentPlayerSubmission> submissions = submissionRepository.findByAssessment_Id(assessmentId);

			// Filter only SUBMITTED submissions
			List<AssessmentPlayerSubmission> submittedSubmissions = submissions.stream()
					.filter(sub -> sub.getSubmissionStatus() == SubmissionStatus.SUBMITTED)
					.collect(Collectors.toList());

			// Count submissions with results (non-empty parameters)
			long resultsRecorded = submittedSubmissions.stream()
					.filter(sub -> sub.getSubmissionParameters() != null && !sub.getSubmissionParameters().isEmpty())
					.count();

			// Count submissions with video URLs
			long videosUploaded = submittedSubmissions.stream()
					.filter(sub -> sub.getSubmissionParameters() != null && sub.getSubmissionParameters().stream()
							.anyMatch(param -> param.getVideoUrl() != null && !param.getVideoUrl().trim().isEmpty()))
					.count();

			// Calculate completion rate (percentage of registrations with submitted
			// submissions)
			double completionRate = totalParticipants > 0
					? (double) submittedSubmissions.size() / totalParticipants * 100
					: 0;

			// Prepare KPI data
			Map<String, Object> kpiData = new HashMap<>();
			kpiData.put("totalParticipants", totalParticipants);
			kpiData.put("registeredParticipants", registeredCount);
			kpiData.put("completedParticipants", completedCount);
			kpiData.put("noShowParticipants", noShowCount);
			kpiData.put("withdrawnParticipants", withdrawnCount);
			kpiData.put("resultsRecorded", resultsRecorded);
			kpiData.put("videosUploaded", videosUploaded);
			kpiData.put("completionRate", Math.round(completionRate * 100.0) / 100.0); // Round to 2 decimal places

			// Get leaderboard data from the view
			List<AssessmentScoresView> leaderboard = scoresViewRepository
					.findByAssessmentIdOrderByAssessmentRankAsc(assessmentId);

			// Return both KPI data and leaderboard in the response
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("kpiData", kpiData);
			responseData.put("leaderboard", leaderboard);

			return ResponseBuilder.success(responseData, "Assessment results and leaderboard fetched successfully");

		} catch (IllegalArgumentException e) {
			log.error("Invalid request: {}", e.getMessage());
			return ResponseBuilder.badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while fetching assessment leaderboard for assessment {}", assessmentId, e);
			return ResponseBuilder.internalServerError("Unexpected error while fetching assessment leaderboard");
		}
	}

	@Override
	public ServiceResponse getGlobalResults(GenericFilter filter) {
		try {
			RankLevel rankingLevel = filter.getRanking();

			List<String> orgAcademyIds = null;

			// Only filter by academy if ranking level is not GLOBAL
			if (rankingLevel != RankLevel.GLOBAL) {
				// Handle case where academyIds size is greater than 1
				if (rankingLevel != RankLevel.ORG && filter.getAcademyIds().size() > 1) {
					log.info("Academy filter contains more than one academy ID {}", filter.getAcademyIds());
					return ResponseBuilder.badRequest("Academy filter contains more than one academy.");
				}

				orgAcademyIds = academyDomainUtil.getAcademyByUrl(filter.getDomainUrl()).stream().map(Academy::getId)
						.collect(Collectors.toList());

				if (orgAcademyIds.isEmpty()) {
					log.info("No academies found for domain: {}", filter.getDomainUrl());
				}
			}

			AssessmentGlobalScoresViewSpecification spec = new AssessmentGlobalScoresViewSpecification(filter,
					orgAcademyIds);
			List<AssessmentGlobalScoresView> playerRankings;
			Page<AssessmentGlobalScoresView> pageResult = null;

			if (filter.isPageable()) {
				PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageResult = globalScoresViewRepository.findAll(spec, page);
				playerRankings = pageResult.getContent();
			} else {
				playerRankings = globalScoresViewRepository.findAll(spec);
			}

			if (playerRankings.isEmpty()) {
				log.info("No player rankings found");
				return ResponseBuilder.success(Collections.emptyList(), "No player rankings found");
			}

			if (filter.isPageable() && pageResult != null) {
				return ResponseBuilder.success(playerRankings, ApiResponse.FETCHED_LIST, pageResult.getTotalPages(),
						pageResult.getTotalElements());
			}
			return ResponseBuilder.success(playerRankings, "Player rankings fetched successfully");
		} catch (Exception e) {
			log.error("Unexpected error while fetching player rankings", e);
			return ResponseBuilder.internalServerError("Unexpected error while fetching player rankings");
		}
	}

	@Override
	public ServiceResponse calculateScores(String assessmentId) {
		try {
			// Get the assessment to verify it exists
			Assessment assessment = assessmentRepository.findById(assessmentId)
					.orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + assessmentId));

			if (assessment.getAssessmentStatus() != AssessmentStatus.CLOSED) {
				return ResponseBuilder.badRequest("Assessment is not closed.");
			}

			// Get all submissions for this assessment
			List<AssessmentPlayerSubmission> submissions = submissionRepository.findByAssessment_Id(assessmentId);

			// Filter only SUBMITTED submissions
			List<AssessmentPlayerSubmission> submittedSubmissions = submissions.stream()
					.filter(sub -> sub.getSubmissionStatus() == SubmissionStatus.SUBMITTED).toList();

			calculateScores(submittedSubmissions);

			return ResponseBuilder.success("Assessment results calculated successfully");
		} catch (IllegalArgumentException e) {
			log.error("Invalid request: {}", e.getMessage());
			return ResponseBuilder.badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while calculating results for assessment", e);
			return ResponseBuilder.internalServerError("Unexpected error while calculating results");
		}
	}

	@Override
	public ServiceResponse calculateGlobalScores() {
		try {
			// Step 1: Get all closed assessments
			List<Assessment> closedAssessments = assessmentRepository.findByAssessmentStatus(AssessmentStatus.CLOSED);
			if (closedAssessments.isEmpty()) {
				return ResponseBuilder.badRequest("No closed assessments found to process results.");
			}

			// Step 2: Extract assessment IDs
			List<String> closedAssessmentIds = closedAssessments.stream().map(Assessment::getId)
					.collect(Collectors.toList());

			// Step 3: Get all SUBMITTED submissions for closed assessments
			List<AssessmentPlayerSubmission> allSubmissions = submissionRepository
					.findByAssessment_IdInAndSubmissionStatus(closedAssessmentIds, SubmissionStatus.SUBMITTED);

			if (allSubmissions.isEmpty()) {
				return ResponseBuilder.badRequest("No submitted submissions found in closed assessments.");
			}

			// Step 4: Filter valid submissions and retain latest one per player
			Map<Object, AssessmentPlayerSubmission> latestSubmissionsMap = allSubmissions.stream()
					.filter(Objects::nonNull)
					.filter(sub -> sub.getRegistration() != null && sub.getRegistration().getPlayer() != null
							&& sub.getSubmittedOn() != null)
					.collect(
							Collectors.toMap(sub -> sub.getRegistration().getPlayer().getId(), Function.identity(),
									(existing,
											replacement) -> replacement.getSubmittedOn().toLocalDateTime()
													.isAfter(existing.getSubmittedOn().toLocalDateTime()) ? replacement
															: existing));

			List<AssessmentPlayerSubmission> latestSubmissions = new ArrayList<>(latestSubmissionsMap.values());

			log.info(
					"Processing global scores for {} latest player submissions (from {} total submissions, across {} closed assessments).",
					latestSubmissions.size(), allSubmissions.size(), closedAssessmentIds.size());

			// Step 5: Perform global score calculation
			calculateGlobalScores(latestSubmissions);

			return ResponseBuilder
					.success("Global assessment results calculated successfully using latest submissions.");

		} catch (IllegalArgumentException e) {
			log.error("Invalid request while calculating global scores: {}", e.getMessage());
			return ResponseBuilder.badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while calculating global assessment results", e);
			return ResponseBuilder.internalServerError("Unexpected error while calculating global assessment results.");
		}
	}

	/**
	 * Calculate assessment-specific scores based on AssessmentParameter comparison
	 * logic Groups by: Assessment + Parameter + ParameterType + UnitType
	 */
	private void calculateScores(List<AssessmentPlayerSubmission> submissions) {
		// Group submissions by assessment
		Map<String, List<AssessmentPlayerSubmission>> submissionsByAssessment = submissions.stream()
				.collect(Collectors.groupingBy(sub -> sub.getAssessment().getId()));
		// Process each assessment separately
		for (Map.Entry<String, List<AssessmentPlayerSubmission>> entry : submissionsByAssessment.entrySet()) {
			String assessmentId = entry.getKey();
			List<AssessmentPlayerSubmission> assessmentSubmissions = entry.getValue();

			log.info("Processing scores for assessment: {}", assessmentId);

			// Collect all parameters from all submissions in this assessment
			Map<ParameterGroupKey, List<AssessmentPlayerSubmissionParameter>> parameterGroups = new HashMap<>();

			for (AssessmentPlayerSubmission submission : assessmentSubmissions) {
				if (submission.getSubmissionParameters() == null) {
					continue;
				}

				for (AssessmentPlayerSubmissionParameter param : submission.getSubmissionParameters()) {
					// Get comparison type from AssessmentParameter (assessment-specific)
					if (param.getParameter() == null) {
						log.warn("Parameter is null for submission: {}", submission.getSubmissionId());
						continue;
					}

					ComparisonType comparisonType = param.getParameter().getComparisonType();

					ParameterGroupKey key = new ParameterGroupKey(assessmentId, param.getParameter().getParameterId(),
							param.getParameterType(), param.getUnitType(), comparisonType);

					parameterGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(param);
				}
			}

			// Calculate percentile and score for each parameter group
			for (Map.Entry<ParameterGroupKey, List<AssessmentPlayerSubmissionParameter>> groupEntry : parameterGroups
					.entrySet()) {
				List<AssessmentPlayerSubmissionParameter> parameters = groupEntry.getValue();
				ParameterGroupKey key = groupEntry.getKey();

				log.debug("Calculating scores for parameter group: {} with {} submissions", key.getParameterId(),
						parameters.size());

				calculatePercentileScores(parameters, key.getComparisonType(), false);
			}
		}

		// Save all updated submissions
		log.info("Saving {} submissions with calculated scores", submissions.size());
		submissionRepository.saveAll(submissions);
	}

	/**
	 * Calculate global scores based on AssessmentParameterConfig comparison logic
	 * Groups by: Parameter + ParameterType + UnitType (across all assessments)
	 */
	private void calculateGlobalScores(List<AssessmentPlayerSubmission> submissions) {
		log.info("Calculating global scores across all assessments");

		// Group all parameters across ALL assessments
		Map<GlobalParameterGroupKey, List<AssessmentPlayerSubmissionParameter>> globalParameterGroups = new HashMap<>();

		for (AssessmentPlayerSubmission submission : submissions) {
			if (submission.getSubmissionParameters() == null) {
				continue;
			}

			for (AssessmentPlayerSubmissionParameter param : submission.getSubmissionParameters()) {
				// Get comparison type from AssessmentParameterConfig (global)
				if (param.getParameterConfig() == null) {
					log.warn("ParameterConfig is null for submission: {}", submission.getSubmissionId());
					continue;
				}

				ComparisonType comparisonType = param.getParameterConfig().getComparisonType();

				GlobalParameterGroupKey key = new GlobalParameterGroupKey(param.getParameterConfig().getParameterId(),
						param.getParameterType(), param.getUnitType(), comparisonType);

				globalParameterGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(param);
			}
		}

		// Calculate global percentile and score for each parameter group
		for (Map.Entry<GlobalParameterGroupKey, List<AssessmentPlayerSubmissionParameter>> groupEntry : globalParameterGroups
				.entrySet()) {
			List<AssessmentPlayerSubmissionParameter> parameters = groupEntry.getValue();
			GlobalParameterGroupKey key = groupEntry.getKey();

			log.debug("Calculating global scores for parameter: {} with {} submissions", key.getParameterId(),
					parameters.size());

			calculatePercentileScores(parameters, key.getComparisonType(), true);
		}

		// Save all updated submissions
		log.info("Saving {} submissions with calculated global scores", submissions.size());
		submissionRepository.saveAll(submissions);
	}

	/**
	 * Calculate percentile-based scores for a group of parameters
	 * 
	 * @param parameters     List of parameters to calculate scores for
	 * @param comparisonType HIGHER or LOWER comparison logic
	 * @param isGlobal       true for global scores, false for assessment-specific
	 *                       scores
	 */
	private void calculatePercentileScores(List<AssessmentPlayerSubmissionParameter> parameters,
			ComparisonType comparisonType, boolean isGlobal) {
		if (parameters == null || parameters.isEmpty()) {
			return;
		}

		// Parse submitted values and create holders
		List<ParameterValueHolder> valueHolders = new ArrayList<>();
		for (AssessmentPlayerSubmissionParameter param : parameters) {
			double value = parseSubmittedValue(param.getSubmittedValue());
			valueHolders.add(new ParameterValueHolder(param, value));
		}

		// Sort based on comparison type and submittedOn
		Comparator<ParameterValueHolder> comparator = Comparator.comparing(ParameterValueHolder::getValue);

		// Add secondary sort by submittedOn (earlier submissions come first when values
		// are equal)
		comparator = comparator.thenComparing(holder -> holder.getParameter().getSubmission().getSubmittedOn() != null
				? holder.getParameter().getSubmission().getSubmittedOn().toLocalDateTime()
				: LocalDateTime.MIN);

		if (comparisonType == ComparisonType.HIGHER) {
			// Higher is better: sort ascending (lower values get lower percentiles)
			valueHolders.sort(comparator);
		} else {
			// Lower is better: sort descending (higher values get lower percentiles)
			valueHolders.sort(comparator.reversed());
		}

		// Calculate percentile and convert to points based on weight
		int totalCount = valueHolders.size();

		for (int i = 0; i < totalCount; i++) {
			ParameterValueHolder holder = valueHolders.get(i);
			AssessmentPlayerSubmissionParameter param = holder.getParameter();

			// Calculate percentile (0-100)
			// Rank 1 out of 10 = 10th percentile (worst)
			// Rank 10 out of 10 = 100th percentile (best)
			double percentile = ((double) (i + 1) / totalCount) * 100.0;

			// Get weight as a whole number (e.g., 30 for 30%)
			Double weight = param.getParameterWeight();
			if (weight == null || weight == 0.0) {
				log.warn("Parameter weight is null or zero for parameter: {}", param.getParameterName());
				weight = 0.0;
			}

			// Convert percentile to points
			// Formula: (percentile / 100) × weight
			// Example: 100th percentile with 30 weight = (100/100) × 30 = 30 points
			// Example: 50th percentile with 30 weight = (50/100) × 30 = 15 points
			double calculatedScore = (percentile / 100.0) * weight;

			// Round to 2 decimal places
			calculatedScore = Math.round(calculatedScore * 100.0) / 100.0;

			// Set the appropriate score field
			if (isGlobal) {
				param.setGlobalScore(calculatedScore);
				log.debug("Set global score {} for parameter {} (percentile: {}, weight: {})", calculatedScore,
						param.getParameterName(), percentile, weight);
			} else {
				param.setScore(calculatedScore);
				log.debug("Set score {} for parameter {} (percentile: {}, weight: {})", calculatedScore,
						param.getParameterName(), percentile, weight);
			}
		}
	}

	/**
	 * Parse submitted value to double for comparison
	 */
	private double parseSubmittedValue(String submittedValue) {
		if (submittedValue == null || submittedValue.trim().isEmpty()) {
			log.warn("Submitted value is null or empty, defaulting to 0.0");
			return 0.0;
		}

		try {
			return Double.parseDouble(submittedValue.trim());
		} catch (NumberFormatException e) {
			log.warn("Failed to parse submitted value: '{}', defaulting to 0.0", submittedValue);
			return 0.0;
		}
	}

	/**
	 * Helper class to group parameters for assessment-specific scoring Groups by:
	 * Assessment + Parameter + Type + Unit + Comparison
	 */
	@Data
	@AllArgsConstructor
	private static class ParameterGroupKey {
		private String assessmentId;
		private String parameterId;
		private ParameterType parameterType;
		private UnitType unitType;
		private ComparisonType comparisonType;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ParameterGroupKey that = (ParameterGroupKey) o;
			return Objects.equals(assessmentId, that.assessmentId) && Objects.equals(parameterId, that.parameterId)
					&& Objects.equals(parameterType, that.parameterType) && Objects.equals(unitType, that.unitType)
					&& Objects.equals(comparisonType, that.comparisonType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(assessmentId, parameterId, parameterType, unitType, comparisonType);
		}
	}

	/**
	 * Helper class to group parameters for global scoring Groups by: Parameter +
	 * Type + Unit + Comparison (across all assessments)
	 */
	@Data
	@AllArgsConstructor
	private static class GlobalParameterGroupKey {
		private String parameterId;
		private ParameterType parameterType;
		private UnitType unitType;
		private ComparisonType comparisonType;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			GlobalParameterGroupKey that = (GlobalParameterGroupKey) o;
			return Objects.equals(parameterId, that.parameterId) && Objects.equals(parameterType, that.parameterType)
					&& Objects.equals(unitType, that.unitType) && Objects.equals(comparisonType, that.comparisonType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(parameterId, parameterType, unitType, comparisonType);
		}
	}

	/**
	 * Helper class to hold parameter and its parsed value for sorting
	 */
	@Data
	@AllArgsConstructor
	private static class ParameterValueHolder {
		private AssessmentPlayerSubmissionParameter parameter;
		private double value;
	}

}
