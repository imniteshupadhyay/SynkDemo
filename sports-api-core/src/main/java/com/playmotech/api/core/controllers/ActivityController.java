package com.playmotech.api.core.controllers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dto.ActivityDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ActivityService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/activities")
public class ActivityController {

	private final ActivityService activityService;
	private final AcademyDomainUtil academyDomainUtil;

	@PostMapping
	public ResponseEntity<ServiceResponse> createActivity(HttpServletRequest request,
			@RequestBody ActivityDto activityDto,
			@RequestParam(name = "academyId", required = false) String academyId) {
		try {
			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}
			ServiceResponse response = activityService.createActivity(activityDto, academyDomain);
			return new ResponseEntity<>(response, response.getHttpStatus());
		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping
	public ResponseEntity<ServiceResponse> updateActivity(HttpServletRequest request,
			@RequestBody ActivityDto activityDto,
			@RequestParam(name = "academyId", required = false) String academyId) {
		try {
			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}
			ServiceResponse response = activityService.updateActivity(activityDto, academyDomain);
			return new ResponseEntity<>(response, response.getHttpStatus());
		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping
	public ResponseEntity<ServiceResponse> deleteActivity(@RequestParam(name = "id", required = true) Long id) {
		ServiceResponse response = activityService.deleteActivity(id);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getAllActivities(HttpServletRequest request,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "orderBy", defaultValue = "insertedOn", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "category", required = false) String category,
			@RequestParam(name = "subCategory", required = false) String subCategory,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(defaultValue = "false", required = false) boolean export) {
		try {

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			// Validate pagination parameters
			if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
				return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
			}

			// Validate orderBy parameter
			String[] allowedOrderByValues = { "name", "category", "subcategory", "insertedOn" };
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

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}

			GenericFilter filter = GenericFilter.builder().userId(currentUser.getUserId()).isPageable(pageable)
					.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
					.startDate(startDateLocal).endDate(endDateLocal).notDeleted(active).export(export)
					.userId(currentUser.getUserId()).domainUrl(academyDomain)
					.activity(Activity.builder().category(category).subcategory(subCategory).build()).build();

			ServiceResponse response = activityService.getAllActivities(filter, academyDomain);
			return new ResponseEntity<>(response, response.getHttpStatus());

		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping
	public ResponseEntity<ServiceResponse> getActivityById(@RequestParam(name = "id", required = true) Long id) {
		ServiceResponse response = activityService.getActivityById(id);
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