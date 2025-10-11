package com.playmotech.api.core.controllers;

import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TraineePerformanceReportDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.utils.TimeStampParse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

/**
 * Created By: deep.patel
 **/

@RestController
@RequestMapping("/trainees")
@CrossOrigin("*")
@AllArgsConstructor
public class TraineeController extends BaseController {

	private final ITraineeService traineeService;

	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping(value = "/academies", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<AcademyDto>>> getMyAcademies() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			List<AcademyDto> academyDtos = traineeService.getMyAcademies(currentUser.getUserId());
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<AcademyDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(academyDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<AcademyDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/performance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineePerformanceReportDto>>> getPerformanceReport(
			@RequestParam(required = false, name = "courseId") String courseId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			List<TraineePerformanceReportDto> traineePerformanceDtos = traineeService
					.getTraineePerformances(currentUser.getUserId(), courseId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePerformanceReportDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(traineePerformanceDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineePerformanceReportDto>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("list")
	public ResponseEntity<?> getAllTrainees(HttpServletRequest request,
			@RequestParam(defaultValue = "id", required = false) String orderBy,
			@RequestParam(defaultValue = "", required = false) String search,
			@RequestParam(defaultValue = "", required = false) String startDate,
			@RequestParam(defaultValue = "", required = false) String endDate,
			@RequestParam(defaultValue = "", required = false) String academyId,
			@RequestParam(defaultValue = "", required = false) String sport,
			@RequestParam(defaultValue = "", required = false) String name,
			@RequestParam(defaultValue = "", required = false) String phone,
			@RequestParam(defaultValue = "", required = false) String email,
			@RequestParam(defaultValue = "1", required = false) Short currentPage,
			@RequestParam(defaultValue = "10", required = false) Short pageSize,
			@RequestParam(defaultValue = "", required = false) List<String> status,
			@RequestParam(defaultValue = "", required = false) List<String> sports,
			@RequestParam(defaultValue = "", required = false) List<String> academyIds,
			@RequestParam(defaultValue = "", required = false) List<String> programIds,
			@RequestParam(defaultValue = "", required = false) List<String> ageCategories,
			@RequestParam(defaultValue = "", required = false) List<String> sources,
			@RequestParam(defaultValue = "true", required = false) boolean active,
			@RequestParam(defaultValue = "true", required = false) boolean ascending,
			@RequestParam(defaultValue = "false", required = false) boolean pageable,
			@RequestParam(defaultValue = "false", required = false) boolean export) {
		try {
			Timestamp startDateLocal = null;
			Timestamp endDateLocal = null;

			if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
				try {
					startDateLocal = TimeStampParse.parseTimestamp(startDate);
					endDateLocal = TimeStampParse.parseTimestamp(endDate);

					if (startDateLocal == null || endDateLocal == null) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(Response.<PaginatedResponse<?>>builder()
										.status(HttpStatus.BAD_REQUEST.value()).message("Invalid listing filters")
										.build());
					}
				} catch (DateTimeParseException e) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(Response.<PaginatedResponse<?>>builder().status(HttpStatus.BAD_REQUEST.value())
									.message("Invalid listing filters").build());
				}
			}

			GenericFilter filter = GenericFilter.builder().orderBy(orderBy).isPageable(pageable)
					.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
					.academyIds(academyIds).programIds(programIds).startDate(startDateLocal).endDate(endDateLocal)
					.sports(sports).ageCategory(ageCategories).notDeleted(active).export(export).build();

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}

			ServiceResponse paginatedTrainess = traineeService.getAllTrainess(filter, academyDomain);

			return new ResponseEntity<>(paginatedTrainess, paginatedTrainess.getHttpStatus());
		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
