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
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("web/payments")
public class WebPaymentController {

	private final com.playmotech.api.core.services.PaymentDashboardService paymentService;
	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping("dues")
	public ResponseEntity<ServiceResponse> getDuesPaymentsList(HttpServletRequest request,
			@RequestParam(name = "orderBy", defaultValue = "markedAt", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "playerIds", required = false) List<String> playerIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "coachId", required = false) String coachId,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "export", defaultValue = "false", required = false) boolean isExport,
			@RequestParam(name = "paymentCategory", required = false) String paymentCategory) {

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		String domainUrl = request.getHeader("origin");

		UserProfile currentUser = academyDomainUtil.getCurrentUser();

		// Validate orderBy parameter
		String[] allowedOrderByValues = { "playerName", "academyName", "sport", "paymentSchedule", "courseName",
				"playerEmailId", "playerContactNumber", "registrationTotalPending", "registrationTotalPaid",
				"courseTotalPending", "finalDueAmount", "totalCourseDue", "currentCourseDue", "courseTotalPaid",
				"registrationDiscountTotal", "courseDiscountTotal", "registrationDueDaysCount", "courseDueDaysCount" };

		if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
			return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
		}

		// Build filter object
		GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.notDeleted(active).academyIds(academyIds).ageCategory(ageCategories).paymentCategory(paymentCategory)
				.domainUrl(domainUrl).programIds(programIds).export(isExport).sports(sports).build();

		ServiceResponse response = paymentService.getDuesPaymentsList(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getPaymentsList(HttpServletRequest request,
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
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "coachId", required = false) String coachId,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "export", defaultValue = "false", required = false) boolean isExport,
			@RequestParam(name = "paymentCategory", required = false) String paymentCategory,
			@RequestParam(name = "dueList", defaultValue = "false", required = false) boolean isDue) {

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		String domainUrl = request.getHeader("origin");

		UserProfile currentUser = academyDomainUtil.getCurrentUser();

		if (!isDue) {

			// Validate orderBy parameter
			String[] allowedOrderByValues = { "receiptId", "amount", "paymentMode", "paymentStatus",
					"invoiceGeneratedBy", "playerName", "status", "academy", "branch", "sport", "program",
					"createdAt", };
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
							.badRequestEntity(INVALID_LENGTH_OR_REGEX.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
				}
			}

			// Build filter object
			GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
					.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
					.notDeleted(active).academyIds(academyIds).playerIds(playerIds).coachId(coachId)
					.ageCategory(ageCategories).endDate(endDateLocal).paymentCategory(paymentCategory)
					.startDate(startDateLocal).domainUrl(domainUrl).programIds(programIds).export(isExport)
					.sports(sports).build();

			ServiceResponse response = paymentService.getPaymentsList(filter);
			return new ResponseEntity<>(response, response.getHttpStatus());
		} else {
			// Validate orderBy parameter
			String[] allowedOrderByValues = { "playerName", "academyName", "sport", "pendingCourseFee",
					"paymentSchedule", "upcomingAmount", "pendingRegistrationFee", "enrollmentId", "courseName",
					"playerEmailId", "playerContactNumber", "dueDaysCount" };

			if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
				return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
			}

			// Build filter object
			GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
					.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
					.notDeleted(active).academyIds(academyIds).ageCategory(ageCategories)
					.paymentCategory(paymentCategory).domainUrl(domainUrl).programIds(programIds).export(isExport)
					.sports(sports).build();

			ServiceResponse response = paymentService.getPendingPaymentDuesList(filter);
			return new ResponseEntity<>(response, response.getHttpStatus());
		}
	}

	@GetMapping("kpis")
	public ResponseEntity<ServiceResponse> getPaymentsKpis(HttpServletRequest request,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate) {

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

		// Build filter object with new date parameters
		GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).academyIds(academyIds)
				.ageCategory(ageCategories).domainUrl(domainUrl).programIds(programIds).sports(sports)
				.startDate(startDateLocal).endDate(endDateLocal).build();

		ServiceResponse response = paymentService.getPaymentsKpis(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/trend")
	public ResponseEntity<ServiceResponse> getPaymentTrend(HttpServletRequest request,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
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
		if (programIds == null) {
			programIds = Collections.emptyList();
		}
		if (sports == null) {
			sports = Collections.emptyList();
		}
		if (academyIds == null) {
			academyIds = Collections.emptyList();
		}
		if (ageCategories == null) {
			ageCategories = Collections.emptyList();
		}

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = paymentService.getPaymentTrend(domainUrl, startDateLocal, endDateLocal, academyIds,
				sports, programIds, ageCategories);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/details")
	public ResponseEntity<ServiceResponse> getEnrollmentDetails(HttpServletRequest request,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> courseIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories) {

		// Handle null values (Spring may pass null for missing params)
		if (courseIds == null) {
			courseIds = Collections.emptyList();
		}
		if (sports == null) {
			sports = Collections.emptyList();
		}
		if (academyIds == null) {
			academyIds = Collections.emptyList();
		}
		if (ageCategories == null) {
			ageCategories = Collections.emptyList();
		}

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = paymentService.getCourseEnrollmentDetails(domainUrl, academyIds, courseIds, sports,
				ageCategories);
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