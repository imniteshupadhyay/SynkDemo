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
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AttendanceService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("web/attendance")
public class WebAttendanceController {

	private final AttendanceService attendanceService;
	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping("coach/list")
	public ResponseEntity<ServiceResponse> getCoachAttendanceList(HttpServletRequest request,
			@RequestParam(name = "orderBy", defaultValue = "recordCreatedAt", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "playerIds", required = false) List<String> playerIds,
			@RequestParam(name = "coachId", required = false) String coachId,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "export", defaultValue = "false", required = false) boolean isExport)
			throws ResourceException {

		// ✅ Validate pagination
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		// ✅ Validate orderBy using CoachAttendanceView_
		String[] allowedOrderByValues = { "coachName", "status", "academy", "sport", "attendanceDate", "program",
				"recordCreatedAt" };
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

		// ✅ Build filter object
		GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.notDeleted(active).academyIds(academyIds).playerIds(playerIds).coachId(coachId).endDate(endDateLocal)
				.startDate(startDateLocal).export(isExport).domainUrl(domainUrl).programIds(programIds).sports(sports)
				.ageCategory(ageCategories).build();

		ServiceResponse response = attendanceService.getCoachAttendanceList(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getAttendanceList(HttpServletRequest request,
			@RequestParam(name = "orderBy", defaultValue = "markedAt", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "playerIds", required = false) List<String> playerIds,
			@RequestParam(name = "coachId", required = false) String coachId,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "export", defaultValue = "false", required = false) boolean isExport)
			throws ResourceException {

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		// Validate orderBy parameter
		String[] allowedOrderByValues = { "playerName", "status", "academy", "branch", "sport", "normalizedDate",
				"program", "markedAt", "markedBy" };
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
				return ResponseBuilder.badRequestEntity(INVALID_LENGTH_OR_REGEX.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		String domainUrl = request.getHeader("origin");

		UserProfile currentUser = academyDomainUtil.getCurrentUser();

		// Build filter object
		GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.notDeleted(active).academyIds(academyIds).playerIds(playerIds).coachId(coachId).endDate(endDateLocal)
				.startDate(startDateLocal).export(isExport).domainUrl(domainUrl).programIds(programIds).sports(sports)
				.ageCategory(ageCategories).build();

		ServiceResponse response = attendanceService.getAttendanceList(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/trend")
	public ResponseEntity<ServiceResponse> getAttendanceTrend(HttpServletRequest request,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> courseIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories) {

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
				return ResponseBuilder.badRequestEntity(INVALID_LENGTH_OR_REGEX.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		// Handle null values (Spring may pass null for missing params)
		if (courseIds == null) {
			courseIds = Collections.emptyList();
		}
		if (academyIds == null) {
			academyIds = Collections.emptyList();
		}
		if (sports == null) {
			sports = Collections.emptyList();
		}
		if (ageCategories == null) {
			ageCategories = Collections.emptyList();
		}

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = attendanceService.getAttendanceTrend(startDateLocal, endDateLocal, domainUrl,
				academyIds, sports, courseIds, ageCategories);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/details")
	public ResponseEntity<ServiceResponse> getEnrollmentDetails(

			HttpServletRequest request, @RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> courseIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories) {

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
				return ResponseBuilder.badRequestEntity(INVALID_LISTING_FILTERS.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		// Handle null values (Spring may pass null for missing params)
		if (courseIds == null) {
			courseIds = Collections.emptyList();
		}
		if (academyIds == null) {
			academyIds = Collections.emptyList();
		}
		if (ageCategories == null) {
			ageCategories = Collections.emptyList();
		}
		if (sports == null) {
			sports = Collections.emptyList();
		}

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = attendanceService.getAttendanceDetails(startDateLocal, endDateLocal, domainUrl,
				academyIds, courseIds, sports, ageCategories);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/kpis")
	public ResponseEntity<ServiceResponse> getAttendanceDetails(HttpServletRequest request,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> courseIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories) {

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
				return ResponseBuilder.badRequestEntity(INVALID_LISTING_FILTERS.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		// Handle null values (Spring may pass null for missing params)
		if (courseIds == null) {
			courseIds = Collections.emptyList();
		}
		if (ageCategories == null) {
			ageCategories = Collections.emptyList();
		}
		if (academyIds == null) {
			academyIds = Collections.emptyList();
		}
		if (sports == null) {
			sports = Collections.emptyList();
		}

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = attendanceService.getCourseEnrollmentDetails(startDateLocal, endDateLocal, domainUrl,
				academyIds, courseIds, sports, ageCategories);
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
