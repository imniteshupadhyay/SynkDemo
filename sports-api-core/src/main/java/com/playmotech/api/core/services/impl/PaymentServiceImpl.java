package com.playmotech.api.core.services.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.exceptions.IOException;
import com.playmotech.api.core.dao_postgres.PendingPaymentDueView;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.PaymentViewMapper;
import com.playmotech.api.core.repo.DuePaymentsViewRepository;
import com.playmotech.api.core.repo.PaymentViewRepository;
import com.playmotech.api.core.repo.PendingPaymentDueViewRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.DuePaymentsViewDao;
import com.playmotech.api.core.response.dao.PaymentDetailsViewDao;
import com.playmotech.api.core.response.dao.PaymentTrendDao;
import com.playmotech.api.core.response.dao.PendingPaymentDueViewDao;
import com.playmotech.api.core.services.PaymentDashboardService;
import com.playmotech.api.core.specification.DuePaymentsViewSpecification;
import com.playmotech.api.core.specification.PaymentViewSpecification;
import com.playmotech.api.core.specification.PendingPaymentDueViewSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.DuePaymentsView;
import com.playmotech.api.core.views.PaymentDetailsView;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentDashboardService {

	private final PaymentViewRepository paymentViewRepository;

//	private final EnrollmentPaymentViewRepository enrollmentPaymentViewRepository;

	private final PendingPaymentDueViewRepository paymentDueViewRepository;

	private final DuePaymentsViewRepository duePaymentsViewRepository;

	private final UserProfileHelper userProfileHelper;
	private final AcademyDomainUtil academyDomainUtil;

	@Override
	public ServiceResponse getDuesPaymentsList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);

			DuePaymentsViewSpecification spec = new DuePaymentsViewSpecification(filter, userProfileHelper);
			List<DuePaymentsView> pendingDues;
			Page<DuePaymentsView> pageableContent = null;

			if (filter.isPageable() && !filter.isExport()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = duePaymentsViewRepository.findAll(spec, pageRequest);
				pendingDues = pageableContent.getContent();
			} else {
				pendingDues = duePaymentsViewRepository.findAll(spec);
			}

			if (pendingDues.isEmpty()) {
				return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND, HttpStatus.OK);
			}

			if (filter.isExport()) {
				try {
					List<DuePaymentsViewDao> daos = PaymentViewMapper.mapDuePaymentsListToDaoList(pendingDues);
					byte[] excelBytes = ExcelGenerator.generateDueExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "payment_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(pendingDues, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK,
						pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(pendingDues, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error fetching pending payment dues", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	@Override
	public ServiceResponse getPendingPaymentDuesList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);

			PendingPaymentDueViewSpecification spec = new PendingPaymentDueViewSpecification(filter, userProfileHelper);
			List<PendingPaymentDueView> pendingDues;
			Page<PendingPaymentDueView> pageableContent = null;

			if (filter.isPageable() && !filter.isExport()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = paymentDueViewRepository.findAll(spec, pageRequest);
				pendingDues = pageableContent.getContent();
			} else {
				pendingDues = paymentDueViewRepository.findAll(spec);
			}

			if (pendingDues.isEmpty()) {
				return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND, HttpStatus.OK);
			}

			if (filter.isExport()) {
				try {
					List<PendingPaymentDueViewDao> daos = PaymentViewMapper.mapPendingListToDaoList(pendingDues);
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "payment_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(pendingDues, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK,
						pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(pendingDues, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error fetching pending payment dues", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	@Override
	public ServiceResponse getPaymentsList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);
			PaymentViewSpecification paymentViewSpecification = new PaymentViewSpecification(filter, userProfileHelper);
			List<PaymentDetailsView> paymentDetailsViews;
			Page<PaymentDetailsView> pageableContent = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = paymentViewRepository.findAll(paymentViewSpecification, pageRequest);
				paymentDetailsViews = pageableContent.getContent();
			} else {
				paymentDetailsViews = paymentViewRepository.findAll(paymentViewSpecification);
			}

			if (paymentDetailsViews.isEmpty()) {
				log.info(ApiResponse.NO_RECORD_FOUND.message);
				return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.LIST_FETCHED_SUCCESSFULLY.message);

			if (filter.isExport()) {
				try {
					List<PaymentDetailsViewDao> daos = PaymentViewMapper.mapListToDaoList(paymentDetailsViews);
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "payment_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(paymentDetailsViews, ApiResponse.LIST_FETCHED_SUCCESSFULLY,
						HttpStatus.OK, pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(paymentDetailsViews, ApiResponse.LIST_FETCHED_SUCCESSFULLY,
						HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error fetching attendance list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

//	private boolean isPaymentDateInRange(Timestamp paymentDate, Timestamp startDate, Timestamp endDate) {
//		if (paymentDate == null) {
//			return false;
//		}
//
//		if ((startDate == null || !paymentDate.before(startDate)) && (endDate == null || !paymentDate.after(endDate))) {
//			return true;
//		}
//
//		return false; // No valid dates in range
//	}

	@Override
	public ServiceResponse getPaymentTrend(String domainUrl, Timestamp startDateLocal, Timestamp endDateLocal,
			List<String> academyIds, List<String> sports, List<String> courseIds, List<String> ageCategories) {
		try {

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			String userProfileId = userRole.equals(RoleType.SUPER_ADMIN.getRole()) ? null : currentUser.getId();

			String formattedAcademyIds = listToCommaSeparatedString(academyIds);
			String formattedCourseIds = listToCommaSeparatedString(courseIds);
			String formattedSports = listToCommaSeparatedString(sports);
			String formattedAgeCategories = listToCommaSeparatedString(ageCategories);

			List<PaymentTrendDao> paymentTrends = paymentViewRepository.getPaymentTrend(startDateLocal, endDateLocal,
					userProfileId, formattedAcademyIds, formattedSports, formattedCourseIds, formattedAgeCategories,
					userRole, domainUrl);

			if (paymentTrends.isEmpty()) {
				log.info(ApiResponse.PAYMENT_TRENDS_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.PAYMENT_TRENDS_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.PAYMENT_TRENDS_FETCHED.message);
			return ResponseBuilder.success(paymentTrends, ApiResponse.PAYMENT_TRENDS_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching payment trends", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_PAYMENT_TRENDS);
		}
	}

	@Override
	public ServiceResponse getCourseEnrollmentDetails(String domainUrl, List<String> academyIds, List<String> courseIds,
			List<String> sports, List<String> ageCategories) {
		try {
			// Create a GenericFilter for using PendingPaymentDueViewSpecification
			GenericFilter filter = new GenericFilter();
			filter.setDomainUrl(domainUrl);

			// Set filter parameters from the provided lists
			if (academyIds != null && !academyIds.isEmpty()) {
				filter.setAcademyIds(academyIds);
			}

			if (courseIds != null && !courseIds.isEmpty()) {
				filter.setProgramIds(courseIds);
			}

			if (sports != null && !sports.isEmpty()) {
				filter.setSports(sports);
			}

			if (ageCategories != null && !ageCategories.isEmpty()) {
				filter.setAgeCategory(ageCategories);
			}

			// Set current user information
			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
			filter.setUserRole(userRole);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			if (!userRole.equals(RoleType.SUPER_ADMIN.getRole())) {
				filter.setUserId(currentUser.getId());
			}

			// Create specification and fetch data
			DuePaymentsViewSpecification spec = new DuePaymentsViewSpecification(filter, userProfileHelper);
			List<DuePaymentsView> paymentDetailsViews = duePaymentsViewRepository.findAll(spec);
			log.info("Fetched {} payment details records", paymentDetailsViews.size());

			if (paymentDetailsViews.isEmpty()) {
				log.info(ApiResponse.COURSE_ENROLLMENT_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.COURSE_ENROLLMENT_NOT_FOUND, HttpStatus.OK);
			}

			LocalDate today = LocalDate.now();

			// ✅ Calculate total course dues where duesOn <= today
			long totalCourseOutstandingSum = paymentDetailsViews.stream().mapToLong(view -> {
				long coursePending = view.getCourseTotalPending() != null ? view.getCourseTotalPending().longValue()
						: 0L;
				long currentDue = 0L;

				if (view.getCurrentCourseDue() != null && view.getDuesOn() != null
						&& !view.getDuesOn().isAfter(today)) {
					currentDue = view.getCurrentCourseDue().longValue();
				}

				return coursePending + currentDue;
			}).sum();

			// ✅ Calculate total registration dues where joiningDate <= today
			long totalRegistrationOutstandingSum = paymentDetailsViews.stream()
					.filter(view -> view.getRegistrationTotalPending() != null && view.getJoiningDate() != null
							&& !view.getJoiningDate().isAfter(today))
					.mapToLong(view -> view.getRegistrationTotalPending().longValue()).sum();

			log.info("Course dues (duesOn <= today): {}", totalCourseOutstandingSum);
			log.info("Registration dues (joiningDate <= today): {}", totalRegistrationOutstandingSum);

			// Create response map
			Map<String, Object> paymentKpiMap = new HashMap<>();
			paymentKpiMap.put("pendingCourseAmount", totalCourseOutstandingSum);
			paymentKpiMap.put("pendingRegistrationAmount", totalRegistrationOutstandingSum);

			log.info(ApiResponse.COURSE_ENROLLMENT_FETCHED.message);
			return ResponseBuilder.success(paymentKpiMap, ApiResponse.COURSE_ENROLLMENT_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching course enrollment details", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_COURSE_ENROLLMENT);
		}
	}

	// @Override
	// public ServiceResponse getCourseEnrollmentDetails(String domainUrl,
	// List<String> academyIds, List<String> courseIds,
	// List<String> sports, List<String> ageCategories) {
	// try {
	//
	// String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
	//
	// UserProfile currentUser = academyDomainUtil.getCurrentUser();
	//
	// String userProfileId = userRole.equals(RoleType.SUPER_ADMIN.getRole()) ? null
	// : currentUser.getId();
	//
	// // Convert empty lists to NULL or comma-separated strings
	// String formattedAcademyIds = listToCommaSeparatedString(academyIds);
	// String formattedCourseIds = listToCommaSeparatedString(courseIds);
	// String formattedSports = listToCommaSeparatedString(sports);
	// String formattedAgeCategories = listToCommaSeparatedString(ageCategories);
	//
	// List<Object[]> results =
	// paymentViewRepository.findCourseEnrollmentDetails(userProfileId,
	// formattedAcademyIds, formattedCourseIds, formattedSports,
	// formattedAgeCategories, userRole,
	// domainUrl);
	//
	// if (results.isEmpty()) {
	// log.info(ApiResponse.COURSE_ENROLLMENT_NOT_FOUND.message);
	// return ResponseBuilder.success(ApiResponse.COURSE_ENROLLMENT_NOT_FOUND,
	// HttpStatus.OK);
	// }
	//
	// List<CourseEnrollmentDetailsDao> enrollmentDetails = results.stream().map(obj
	// -> {
	// CourseEnrollmentDetailsDao dto = new CourseEnrollmentDetailsDao();
	// dto.setEnrollmentId((String) obj[0]);
	// dto.setTraineeUserId((String) obj[1]);
	// dto.setAcademyId((String) obj[2]);
	// dto.setCourseId((String) obj[3]);
	// dto.setSport((String) obj[4]);
	// dto.setPaymentSchedule((String) obj[5]);
	//
	// dto.setTotalCourseFeeCollected((Long) obj[6]);
	// dto.setTotalRegistrationFeeCollected((Long) obj[7]);
	// dto.setPendingCourseFee((Long) obj[8]);
	// dto.setPendingRegistrationFee((Long) obj[9]);
	//
	// return dto;
	// }).toList();
	//
	// Long totalCourseOutstandingSum = enrollmentDetails.stream()
	// .mapToLong(CourseEnrollmentDetailsDao::getPendingCourseFee).sum();
	//
	// Long totalRegistrationOutstandingSum = enrollmentDetails.stream()
	// .mapToLong(CourseEnrollmentDetailsDao::getPendingRegistrationFee).sum();
	//
	// Map<String, Object> paymentKpiMap = new HashMap<>();
	// paymentKpiMap.put("pendingCourseAmount", totalCourseOutstandingSum);
	// paymentKpiMap.put("pendingRegistrationAmount",
	// totalRegistrationOutstandingSum);
	// paymentKpiMap.put("enrollmentDetails", enrollmentDetails);
	//
	// log.info(ApiResponse.COURSE_ENROLLMENT_FETCHED.message);
	// return ResponseBuilder.success(paymentKpiMap,
	// ApiResponse.COURSE_ENROLLMENT_FETCHED, HttpStatus.OK);
	// } catch (Exception e) {
	// log.error("Error fetching course enrollment details", e);
	// return
	// ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_COURSE_ENROLLMENT);
	// }
	// }

	private String listToCommaSeparatedString(List<String> list) {
		if (list == null || list.isEmpty()) {
			return null; // NULL values will be handled correctly in SQL
		}
		return String.join(",", list);
	}

//	private String formatAsPostgresArray(List<String> list) {
//		if (list == null || list.isEmpty()) {
//			return null;
//		}
//		return "{" + String.join(",", list) + "}";
//	}

	private static final DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public ServiceResponse getPaymentsKpis(GenericFilter filter) {
		try {
			log.info("Fetching payments KPIs for filter: {}", filter);

			// Get user role
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);
			log.info("Resolved user role: {}", userRole);

			// Build spec and fetch data
			DuePaymentsViewSpecification spec = new DuePaymentsViewSpecification(filter, userProfileHelper);
			List<DuePaymentsView> paymentDetailsViews = duePaymentsViewRepository.findAll(spec);
			log.info("Fetched {} payment details records", paymentDetailsViews.size());

			if (paymentDetailsViews.isEmpty()) {
				log.info(ApiResponse.PAYMENTS_KPI_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.PAYMENTS_KPI_NOT_FOUND, HttpStatus.OK);
			}

			// Extract optional LocalDateTime range
			LocalDateTime startDateTime = (filter.getStartDate() != null)
					? filter.getStartDate().toLocalDateTime().withNano(0)
					: null;
			LocalDateTime endDateTime = (filter.getEndDate() != null)
					? filter.getEndDate().toLocalDateTime().withNano(0)
					: null;
			log.info("Start date: {}", startDateTime);
			log.info("End date: {}", endDateTime);

			// Sum registration fee
			BigDecimal totalRegistrationFeePaid = paymentDetailsViews.stream()
					.map(DuePaymentsView::getPaidRegistrationPaymentDetails)
					.peek(json -> log.info("Registration JSON: {}", json)).filter(Objects::nonNull)
					.map(json -> sumJsonValuesWithinDateRange(json, startDateTime, endDateTime))
					.peek(amount -> log.info("Partial registration amount: {}", amount))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			log.info("Total registration fee paid: {}", totalRegistrationFeePaid);

			// Sum course fee
			BigDecimal totalCourseFeePaid = paymentDetailsViews.stream()
					.map(DuePaymentsView::getPaidCoursePaymentDetails).peek(json -> log.info("Course JSON: {}", json))
					.filter(Objects::nonNull)
					.map(json -> sumJsonValuesWithinDateRange(json, startDateTime, endDateTime))
					.peek(amount -> log.info("Partial course amount: {}", amount))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			log.info("Total course fee paid: {}", totalCourseFeePaid);

			// Prepare result
			Map<String, BigDecimal> paymentKpiMap = new HashMap<>();
			paymentKpiMap.put("totalRegistrationFeePaid", totalRegistrationFeePaid);
			paymentKpiMap.put("totalCourseFeePaid", totalCourseFeePaid);

			log.info(ApiResponse.PAYMENTS_KPI_FETCHED.message);
			return ResponseBuilder.success(paymentKpiMap, ApiResponse.PAYMENTS_KPI_FETCHED, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error fetching payments KPIs", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_PAYMENTS_KPI);
		}
	}

	private BigDecimal sumJsonValuesWithinDateRange(String json, LocalDateTime startDateTime,
			LocalDateTime endDateTime) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, BigDecimal> map = mapper.readValue(json, new TypeReference<>() {
			});

			return map.entrySet().stream()
					.peek(entry -> log.info("Processing entry: {} -> {}", entry.getKey(), entry.getValue()))
					.filter(entry -> {
						try {
							LocalDateTime entryDateTime = LocalDateTime.parse(entry.getKey(), CUSTOM_FORMATTER);
							boolean isAfterOrEqualStart = (startDateTime == null)
									|| !entryDateTime.isBefore(startDateTime);
							boolean isBeforeOrEqualEnd = (endDateTime == null) || !entryDateTime.isAfter(endDateTime);
							boolean isIncluded = isAfterOrEqualStart && isBeforeOrEqualEnd;
							log.info("Entry datetime: {}, included: {}", entryDateTime, isIncluded);
							return isIncluded;
						} catch (DateTimeParseException e) {
							log.warn("Invalid date-time format in key: {}", entry.getKey(), e);
							return false;
						}
					}).map(Map.Entry::getValue).filter(Objects::nonNull)
					.peek(val -> log.info("Value considered for sum: {}", val))
					.reduce(BigDecimal.ZERO, BigDecimal::add);

		} catch (Exception e) {
			log.warn("Failed to parse or sum JSON payment details: {}", json, e);
			return BigDecimal.ZERO;
		}
	}

//	private BigDecimal sumJsonValues(String json) {
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			Map<String, BigDecimal> map = mapper.readValue(json, new TypeReference<>() {
//			});
//			return map.values().stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
//		} catch (Exception e) {
//			log.warn("Failed to parse payment details JSON: {}", json, e);
//			return BigDecimal.ZERO;
//		}
//	}

}
