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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.BulkType;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.UploadStatus;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.BulkHistoryService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/bulk")
@RequiredArgsConstructor
public class WebBulkUploadHistoryController {

	private final BulkHistoryService bulkHistoryService;

	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping("history")
	public ResponseEntity<ServiceResponse> getHistory(HttpServletRequest request,
			@RequestParam(name = "orderBy", defaultValue = "insertedOn", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "type", required = false) List<BulkType> bulkTypes,
			@RequestParam(name = "status", required = false) List<UploadStatus> uploadStatuses) {
		try {

			// Validate pagination parameters
			if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
				return badRequestEntity(INVALID_REQUEST);
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

			// Validate orderBy parameter
			String[] allowedOrderByValues = { "type", "status", "fileName", "totalRecords", "successCount",
					"failureCount", "startedAt", "completedAt", "insertedOn" };
			if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
				return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
			}

			// Validate bulkTypes parameter
			if (bulkTypes != null && !bulkTypes.isEmpty()) {
				for (BulkType bulkType : bulkTypes) {
					if (!Arrays.asList(BulkType.values()).contains(bulkType)) {
						return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + "Valid bulk types: "
								+ Arrays.toString(BulkType.values()));
					}
				}
			}

			// Validate uploadStatuses parameter
			if (uploadStatuses != null && !uploadStatuses.isEmpty()) {
				for (UploadStatus uploadStatus : uploadStatuses) {
					if (!Arrays.asList(UploadStatus.values()).contains(uploadStatus)) {
						return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + "Valid upload statuses: "
								+ Arrays.toString(UploadStatus.values()));
					}
				}
			}

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			// Build filter object
			GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).isPageable(pageable)
					.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
					.startDate(startDateLocal).endDate(endDateLocal).notDeleted(active).academyId(academyId)
					.academyIds(academyIds).programIds(programIds)
					.uploadHistory(
							BulkUploadHistory.builder().bulkTypes(bulkTypes).uploadStatuses(uploadStatuses).build())
					.domainUrl(academyDomain).build();

			ServiceResponse response = bulkHistoryService.getHistory(filter);
			return ResponseEntity.status(response.getHttpStatus()).body(response);

		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * API endpoint to check bulk upload status
	 */
	@GetMapping
	public ResponseEntity<ServiceResponse> getBulkUploadStatus(@RequestParam Long historyId) {
		ServiceResponse response = bulkHistoryService.getHistoryById(historyId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);

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