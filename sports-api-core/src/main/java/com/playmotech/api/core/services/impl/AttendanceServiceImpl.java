package com.playmotech.api.core.services.impl;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.itextpdf.io.exceptions.IOException;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.helper.AcademyHelper;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.AttendanceViewMapper;
import com.playmotech.api.core.repo.AttendanceViewRepository;
import com.playmotech.api.core.repo.CoachAttendanceViewRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.AttendanceTrendDao;
import com.playmotech.api.core.response.dao.AttendanceViewDao;
import com.playmotech.api.core.response.dao.CoachAttendanceViewDao;
import com.playmotech.api.core.services.AttendanceService;
import com.playmotech.api.core.specification.AttendanceSpecification;
import com.playmotech.api.core.specification.CoachAttendanceSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ColumnHeader;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.AttendanceView;
import com.playmotech.api.core.views.CoachAttendanceView;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

	private final AttendanceViewRepository attendanceViewRepository;
	private final CoachAttendanceViewRepository coachAttendanceViewRepository;
	private final UserProfileRepo userProfileRepository;
	private final AcademyHelper academyHelper;
	private final UserProfileHelper userProfileHelper;
	private final AcademyDomainUtil academyDomainUtil;

	public void exportUsersToExcel(HttpServletResponse response, GenericFilter filter) {
		try {
			AttendanceSpecification attendanceSpecification = new AttendanceSpecification(filter, userProfileHelper);
			List<AttendanceView> attendanceRecords = attendanceViewRepository.findAll(attendanceSpecification);
			ExcelGenerator.export(attendanceRecords, response, "attendances.xlsx");
		} catch (Exception e) {
			log.error("Excel export failed for attendance records...", e);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

		}
	}

	@Override
	public ServiceResponse getCoachAttendanceList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);
			CoachAttendanceSpecification attendanceSpecification = new CoachAttendanceSpecification(filter,
					userProfileHelper);
			List<CoachAttendanceView> attendanceRecords;
			Page<CoachAttendanceView> pageableContent = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = coachAttendanceViewRepository.findAll(attendanceSpecification, pageRequest);
				attendanceRecords = pageableContent.getContent();
			} else {
				attendanceRecords = coachAttendanceViewRepository.findAll(attendanceSpecification);
			}

			if (attendanceRecords.isEmpty()) {
				log.info(ApiResponse.ATTENDANCE_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.ATTENDANCE_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.ATTENDANCE_LIST_FETCHED.message);

			if (filter.isExport()) {
				try {
					List<CoachAttendanceViewDao> daos = AttendanceViewMapper.mapListToCoachDaoList(attendanceRecords);

					// Get the field names and their corresponding column headers
					Map<String, String> fieldToHeaderMap = new HashMap<>();
					List<String> fieldNames = new ArrayList<>();

					for (Field field : CoachAttendanceViewDao.class.getDeclaredFields()) {
						ColumnHeader annotation = field.getAnnotation(ColumnHeader.class);
						if (annotation != null && annotation.include()) {
							fieldNames.add(field.getName());
							fieldToHeaderMap.put(field.getName(), annotation.value());
						}
					}

					// Generate Excel with the field names (not the display names)
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, fieldNames);

					// Create response with the Excel file
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "coach_attendance_" + System.currentTimeMillis() + ".xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (Exception e) {
					log.error("Error exporting coach attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(attendanceRecords, ApiResponse.ATTENDANCE_LIST_FETCHED, HttpStatus.OK,
						pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(attendanceRecords, ApiResponse.ATTENDANCE_LIST_FETCHED, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error fetching attendance list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ATTENDANCE_LIST);
		}
	}

	@Override
	public ServiceResponse getAttendanceList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);
			AttendanceSpecification attendanceSpecification = new AttendanceSpecification(filter, userProfileHelper);
			List<AttendanceView> attendanceRecords;
			Page<AttendanceView> pageableContent = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = attendanceViewRepository.findAll(attendanceSpecification, pageRequest);
				attendanceRecords = pageableContent.getContent();
			} else {
				attendanceRecords = attendanceViewRepository.findAll(attendanceSpecification);
			}

			if (attendanceRecords.isEmpty()) {
				log.info(ApiResponse.ATTENDANCE_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.ATTENDANCE_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.ATTENDANCE_LIST_FETCHED.message);

			if (filter.isExport()) {
				try {
					List<AttendanceViewDao> daos = AttendanceViewMapper.mapListToDaoList(attendanceRecords);
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "attendance_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(attendanceRecords, ApiResponse.ATTENDANCE_LIST_FETCHED, HttpStatus.OK,
						pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(attendanceRecords, ApiResponse.ATTENDANCE_LIST_FETCHED, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error fetching attendance list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ATTENDANCE_LIST);
		}
	}

	@Override
	public ServiceResponse getAttendanceTrend(Timestamp startDateLocal, Timestamp endDateLocal, String domainUrl,
			List<String> academyIds, List<String> sports, List<String> courseIds, List<String> ageCategories) {
		try {

//			ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);
//
//			if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
//				return serviceResponse;
//			}

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			String userProfileId = userRole.equals(RoleType.SUPER_ADMIN.getRole()) ? null : currentUser.getId();

			String formattedAcademyIds = listToCommaSeparatedString(academyIds);
			String formattedCourseIds = listToCommaSeparatedString(courseIds);
			String formattedSports = listToCommaSeparatedString(sports);
			String formattedAgeCategories = listToCommaSeparatedString(ageCategories);

			List<AttendanceTrendDao> paymentTrends = attendanceViewRepository.getAttendanceTrend(startDateLocal,
					endDateLocal, userProfileId, formattedAcademyIds, formattedSports, formattedCourseIds,
					formattedAgeCategories, userRole, domainUrl);

			if (paymentTrends.isEmpty()) {
				log.info(ApiResponse.ATTENDANCE_TRENDS_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.ATTENDANCE_TRENDS_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.ATTENDANCE_TRENDS_FETCHED.message);
			return ResponseBuilder.success(paymentTrends, ApiResponse.ATTENDANCE_TRENDS_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching attendance trends", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ATTENDANCE_TRENDS);
		}
	}

	@Override
	public ServiceResponse getCourseEnrollmentDetails(Timestamp startDateLocal, Timestamp endDateLocal,
			String domainUrl, List<String> academyIds, List<String> courseIds, List<String> sports,
			List<String> ageCategories) {
		try {

//			ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);
//
//			if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
//				return serviceResponse;
//			}

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			String userProfileId = userRole.equals(RoleType.SUPER_ADMIN.getRole()) ? null : currentUser.getId();

			// Convert empty lists to NULL or comma-separated strings
			String formattedAcademyIds = listToCommaSeparatedString(academyIds);
			String formattedCourseIds = listToCommaSeparatedString(courseIds);
			String formattedSports = listToCommaSeparatedString(sports);
			String formattedAgeCategories = listToCommaSeparatedString(ageCategories);

			List<Object[]> results = attendanceViewRepository.findPlayerAddedDetails(startDateLocal, endDateLocal,
					userProfileId, formattedAcademyIds, formattedCourseIds, formattedSports, formattedAgeCategories,
					userRole, domainUrl);

			if (results.isEmpty()) {
				log.info(ApiResponse.PLAYER_ADDED_COUNT_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.PLAYER_ADDED_COUNT_NOT_FOUND, HttpStatus.OK);
			}

			// Extract the first and only result
			Object[] record = results.get(0);
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("selectedTimePeriod", record[0]); // String
			responseMap.put("totalPlayers", ((Number) record[1]).intValue()); // Integer
			responseMap.put("previousTotalPlayers", ((Number) record[2]).intValue()); // Integer

			log.info(ApiResponse.PLAYER_ADDED_COUNT_FETCHED.message);
			return ResponseBuilder.success(responseMap, ApiResponse.PLAYER_ADDED_COUNT_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching player added count details", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_PLAYER_ADDED_COUNT);
		}
	}

	@Override
	public ServiceResponse getAttendanceDetails(Timestamp startDateLocal, Timestamp endDateLocal, String domainUrl,
			List<String> academyIds, List<String> courseIds, List<String> sports, List<String> ageCategories) {
		try {

//			ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);
//
//			if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
//				return serviceResponse;
//			}

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			String userProfileId = userRole.equals(RoleType.SUPER_ADMIN.getRole()) ? null : currentUser.getId();

			// Convert empty lists to NULL or comma-separated strings
			String formattedAcademyIds = listToCommaSeparatedString(academyIds);
			String formattedCourseIds = listToCommaSeparatedString(courseIds);
			String formattedSports = listToCommaSeparatedString(sports);
			String formattedAgeCategories = listToCommaSeparatedString(ageCategories);

			List<Object[]> results = attendanceViewRepository.findAttendanceDetails(startDateLocal, endDateLocal,
					userProfileId, formattedAcademyIds, formattedCourseIds, formattedSports, formattedAgeCategories,
					userRole, domainUrl);

			if (results.isEmpty()) {
				log.info(ApiResponse.ATTEDNANCE_PERCENTAGE_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.ATTEDNANCE_PERCENTAGE_NOT_FOUND, HttpStatus.OK);
			}

			// Extract the first and only result
			Object[] record = results.get(0);
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("periodLabel", record[0]); // String
			responseMap.put("totalStudents", ((Number) record[1]).intValue()); // Integer
			responseMap.put("avgAttendance", ((Number) record[2]).doubleValue()); // Double
			responseMap.put("totalSessions", ((Number) record[3]).intValue()); // Integer
			responseMap.put("previousPeriodAvgAttendance", ((Number) record[4]).doubleValue()); // Double
			responseMap.put("previousPeriodTotalSessions", ((Number) record[5]).intValue()); // Integer

			log.info(ApiResponse.ATTEDNANCE_PERCENTAGE_FETCHED.message);
			return ResponseBuilder.success(responseMap, ApiResponse.ATTEDNANCE_PERCENTAGE_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching attendance percentage", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ATTEDNANCE_PERCENTAGE);
		}
	}

	private String listToCommaSeparatedString(List<String> list) {
		if (list == null || list.isEmpty()) {
			return null; // NULL values will be handled correctly in SQL
		}
		return String.join(",", list);
	}

}
