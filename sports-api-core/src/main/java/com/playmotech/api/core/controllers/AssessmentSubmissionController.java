package com.playmotech.api.core.controllers;

import static com.playmotech.api.core.response.ApiResponse.INVALID_LENGTH_OR_REGEX;
import static com.playmotech.api.core.response.ApiResponse.INVALID_LISTING_FILTERS;
import static com.playmotech.api.core.response.ApiResponse.INVALID_REQUEST;
import static com.playmotech.api.core.response.ResponseBuilder.badRequestEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AssessmentSubmissionDto;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AssessmentSubmissionService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("assessments/submissions")
public class AssessmentSubmissionController {

	private final AcademyDomainUtil academyDomainUtil;

	private final AssessmentSubmissionService submissionService;

	@PostMapping
	public ResponseEntity<ServiceResponse> submitAssessment(@RequestBody AssessmentSubmissionDto submissionDto) {
		ServiceResponse response = submissionService.submitAssessment(submissionDto);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getSubmissions(HttpServletRequest request,
			@RequestParam(name = "assessmentId") String assessmentId,
			@RequestParam(name = "orderBy", defaultValue = "createdOn", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "gender", required = false) List<String> genders,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "submissionStatus", required = false) List<String> submissionStatus) {

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		// Allowed orderBy fields
		String[] allowedOrderByValues = { "createdOn", "sport", "ageCategory", "gender" };

		// Validate orderBy parameter
		if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
			return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
		}

		Timestamp startDateLocal = null;
		Timestamp endDateLocal = null;

		if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
			try {
				startDateLocal = parseTimestamp(startDate);
				endDateLocal = parseTimestamp(endDate);
				if (startDateLocal == null || endDateLocal == null
						|| startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
					return ResponseBuilder.badRequestEntity(INVALID_LISTING_FILTERS.message);
				}
			} catch (DateTimeParseException e) {
				return ResponseBuilder
						.badRequestEntity(INVALID_LENGTH_OR_REGEX.message + " yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		String domainUrl = request.getHeader("origin");
		UserProfile currentUser = academyDomainUtil.getCurrentUser();

		GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.notDeleted(active).academyIds(academyIds).endDate(endDateLocal).startDate(startDateLocal)
				.domainUrl(domainUrl).sports(sports).ageCategory(ageCategories).gender(genders)
				.assessmentPlayerSubmission(AssessmentPlayerSubmission.builder().status(submissionStatus)
						.assessmentId(assessmentId).build())
				.build();

		ServiceResponse response = submissionService.getSubmissions(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("view")
	public ResponseEntity<ServiceResponse> getSubmissionById(@RequestParam String submissionId) {
		ServiceResponse response = submissionService.getSubmissionById(submissionId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping
	public ResponseEntity<ServiceResponse> getRegistrationById(@RequestParam String registrationId) {
		ServiceResponse response = submissionService.getSubmissionByRegistrationId(registrationId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	private static Timestamp parseTimestamp(String timestampStr) {
		if (timestampStr == null || timestampStr.isEmpty()) {
			return null;
		}
		try {
			return Timestamp.valueOf(
					LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
