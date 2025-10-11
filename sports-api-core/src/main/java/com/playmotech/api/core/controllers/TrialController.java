package com.playmotech.api.core.controllers;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.dto.TrialFeedbackDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.TrialService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/trials")
public class TrialController {

	private final TrialService trialService;

	@PostMapping
	public ResponseEntity<ServiceResponse> createTrial(@RequestBody TrialDto trialDto) {
		ServiceResponse response = trialService.createTrial(trialDto);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PutMapping
	public ResponseEntity<ServiceResponse> updateTrial(@RequestBody TrialDto trialDto) {
		ServiceResponse response = trialService.updateTrial(trialDto);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@DeleteMapping
	public ResponseEntity<ServiceResponse> deleteTrial(@RequestParam(name = "id", required = true) String id) {
		ServiceResponse response = trialService.deleteTrial(id);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getAllTrials(
			// @RequestHeader(name = "UserId", required = true) String userId,
			HttpServletRequest request,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "completed", defaultValue = "false", required = false) boolean completed,
			@RequestParam(name = "orderBy", defaultValue = "insertedOn", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "status", required = false) TrialStatus status,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(defaultValue = "false", required = false) boolean export) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
		}

		// Validate orderBy parameter
		String[] allowedOrderByValues = { "name", "email", "phone", "insertedOn", "status" };
		if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
			return ResponseBuilder.badRequestEntity(
					ApiResponse.INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
		}

		Timestamp startDateLocal = null;
		Timestamp endDateLocal = null;
		if (startDate != null && endDate != null) {
			try {
				startDateLocal = parseTimestamp(startDate);
				endDateLocal = parseTimestamp(endDate);
				if (startDate != null && endDate != null
						&& startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
					return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_LISTING_FILTERS);
				}
			} catch (DateTimeParseException e) {
				return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
			}
		}

		GenericFilter filter = GenericFilter.builder().userId(currentUser.getUserId()).isPageable(pageable)
				.ageCategory(ageCategories).currentPage(currentPage).pageSize(pageSize).search(search)
				.ascending(ascending).orderBy(orderBy).startDate(startDateLocal).endDate(endDateLocal)
				.notDeleted(active).export(export).userId(currentUser.getUserId()).programIds(programIds).sports(sports)
				.academyId(academyId).academyIds(academyIds)
				.trial(Trial.builder().completed(completed).status(status).build()).build();

		// String academyDomain = "";
		// if (StringUtils.hasText(academyId)) {
		// academyDomain = academyDomainUtil.getAcademyDomain(academyId);
		// } else {
		// academyDomain = request.getHeader("origin");
		// }

		ServiceResponse response = trialService.getAllTrials(filter, null);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping
	public ResponseEntity<ServiceResponse> getTrialById(@RequestParam(name = "id", required = true) String id) {
		ServiceResponse response = trialService.getTrialById(id);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping("feedback")
	public ResponseEntity<ServiceResponse> updateTrialFeedback(@RequestParam(name = "id", required = true) String id,
			@RequestBody TrialFeedbackDto dto) {

		ServiceResponse response = trialService.addTrialFeedback(id, dto);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PatchMapping("reason")
	public ResponseEntity<ServiceResponse> addPeningReason(@RequestParam(name = "id", required = true) String id,
			@RequestParam(name = "reason", required = true) String reason) {

		ServiceResponse response = trialService.addPedningReason(id, reason);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PatchMapping("schedule")
	public ResponseEntity<ServiceResponse> scheduleTrial(@RequestParam(name = "id", required = true) String id,
			@RequestParam(name = "trialDate", required = true) LocalDate trialDate,
			@RequestParam(name = "trialTime", required = true) LocalTime trialTime) {
		ServiceResponse response = trialService.updateTrialTime(id, trialDate, trialTime);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PatchMapping("retrial")
	public ResponseEntity<ServiceResponse> scheduleTrial(@RequestParam(name = "id", required = true) String id) {
		ServiceResponse response = trialService.addToPending(id);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	private Timestamp parseTimestamp(String timestampStr) throws DateTimeParseException {
		if (timestampStr == null || timestampStr.isEmpty()) {
			return null;
		}
		return Timestamp
				.valueOf(LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
	}
}
