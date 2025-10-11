package com.playmotech.api.core.services.impl;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.itextpdf.io.exceptions.IOException;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.PerformanceViewMapper;
import com.playmotech.api.core.repo.CoachPerformanceViewRepository;
import com.playmotech.api.core.repo.TraineePerformanceViewRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.CoachPerformanceReportViewDao;
import com.playmotech.api.core.response.dao.TraineePerformanceReportViewDao;
import com.playmotech.api.core.services.PerformanceService;
import com.playmotech.api.core.specification.CoachPerformanceSpecification;
import com.playmotech.api.core.specification.TraineePerformanceSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.CoachPerformanceReportView;
import com.playmotech.api.core.views.TraineePerformanceReportView;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

	private final CoachPerformanceViewRepository coachPerformanceViewRepository;
	private final TraineePerformanceViewRepository traineePerformanceViewRepository;
	private final UserProfileHelper userProfileHelper;
	private final AcademyDomainUtil academyDomainUtil;

	@Override
	public ServiceResponse getTraineePerformanceList(GenericFilter filter) {
		try {
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);
			TraineePerformanceSpecification specification = new TraineePerformanceSpecification(filter,
					userProfileHelper);

			Page<TraineePerformanceReportView> pageableContent = null;
			List<TraineePerformanceReportView> traineeRecords;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = traineePerformanceViewRepository.findAll(specification, pageRequest);
				traineeRecords = pageableContent.getContent();
			} else {
				traineeRecords = traineePerformanceViewRepository.findAll(specification);
			}

			if (traineeRecords.isEmpty()) {
				log.info(ApiResponse.PERFORMANCE_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.PERFORMANCE_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.PERFORMANCE_LIST_FETCHED.message);
			if (filter.isExport()) {
				try {
					List<TraineePerformanceReportViewDao> daos = PerformanceViewMapper.mapListToDaoList(traineeRecords);
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "performance_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(traineeRecords, ApiResponse.PERFORMANCE_LIST_FETCHED,
						HttpStatus.OK, pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(traineeRecords, ApiResponse.PERFORMANCE_LIST_FETCHED,
						HttpStatus.OK);
			}

		} catch (Exception e) {
			log.error("Error fetching trainee performance list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_PERFORMANCE_LIST);
		}
	}

	@Override
	public ServiceResponse getCoachPerformanceList(GenericFilter filter) {
		try {

			if (Boolean.FALSE.equals(filter.isApp())) {
				String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
				filter.setUserRole(userRole);
			}

			CoachPerformanceSpecification specification = new CoachPerformanceSpecification(filter);

			Page<CoachPerformanceReportView> pageableContent = null;
			List<CoachPerformanceReportView> coachRecords;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = coachPerformanceViewRepository.findAll(specification, pageRequest);
				coachRecords = pageableContent.getContent();
			} else {
				coachRecords = coachPerformanceViewRepository.findAll(specification);
			}

			if (coachRecords.isEmpty()) {
				log.info(ApiResponse.PERFORMANCE_NOT_FOUND.message);
				return ResponseBuilder.success(ApiResponse.PERFORMANCE_NOT_FOUND, HttpStatus.OK);
			}

			log.info(ApiResponse.PERFORMANCE_LIST_FETCHED.message);
			if (filter.isExport()) {
				try {
					List<CoachPerformanceReportViewDao> daos = PerformanceViewMapper
							.mapListToCoachDaoList(coachRecords);
					byte[] excelBytes = ExcelGenerator.generateExcel(daos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "performance_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else if (filter.isPageable()) {
				return ResponseBuilder.success(coachRecords, ApiResponse.PERFORMANCE_LIST_FETCHED,
						HttpStatus.OK, pageableContent.getTotalPages(), pageableContent.getTotalElements());
			} else {
				return ResponseBuilder.success(coachRecords, ApiResponse.PERFORMANCE_LIST_FETCHED,
						HttpStatus.OK);
			}

		} catch (Exception e) {
			log.error("Error fetching trainee performance list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_PERFORMANCE_LIST);
		}
	}
}
