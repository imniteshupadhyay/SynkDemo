package com.playmotech.api.core.services.impl;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.itextpdf.io.exceptions.IOException;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.ScheduleFile;
import com.playmotech.api.core.helper.AcademyHelper;
import com.playmotech.api.core.mapper.ProgramMapper;
import com.playmotech.api.core.repo.CourseDetailsViewRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.ScheduleFileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.CourseExportDao;
import com.playmotech.api.core.response.dao.ProgramDao;
import com.playmotech.api.core.services.ProgramService;
import com.playmotech.api.core.specification.ProgramViewSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.CourseDetailsView;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

	private final CourseRepo programRepository;
	private final CourseDetailsViewRepo courseDetailsViewRepo;

	private final AcademyHelper academyHelper;
	private final AcademyDomainUtil academyDomainUtil;
	private final ScheduleFileRepo scheduleFileRepo;

	@Override
	public ServiceResponse getProgramsByAcademyId(String academyId) {
		log.info("Fetching programs for academy ID: {}", academyId);

		ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);

		if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
			return serviceResponse;
		}

		List<Course> programs = programRepository.findByAcademyIdAndInactiveFalse(academyId);

		if (programs.isEmpty()) {
			log.warn(ApiResponse.PROGRAM_NOT_FOUND.getMessage());
			return ResponseBuilder.success(ApiResponse.PROGRAM_NOT_FOUND, HttpStatus.OK);
		}

		List<ProgramDao> programDaos = ProgramMapper.mapListToDaoList(programs, null);

		log.info(ApiResponse.PROGRAM_LIST_FETCHED.getMessage());
		return ResponseBuilder.success(programDaos, ApiResponse.PROGRAM_LIST_FETCHED, HttpStatus.OK);
	}

	@Override
	public ServiceResponse getPrograms(GenericFilter filter) {
		try {
			// Get user role for current domain
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);

			// Build specification
			ProgramViewSpecification specification = new ProgramViewSpecification(filter);

			List<CourseDetailsView> programs;
			Page<CourseDetailsView> pageResult = null;

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageResult = courseDetailsViewRepo.findAll(specification, pageRequest);
				programs = pageResult.getContent();
			} else {
				programs = courseDetailsViewRepo.findAll(specification);
			}

			if (programs.isEmpty()) {
				log.info(ApiResponse.NO_RECORD_FOUND.message);
				return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND);
			}
			if (filter.isExport()) {
				try {
					List<CourseExportDao> courseExportDaos = enrichScheduleFiles(programs).stream()
							.map(ProgramMapper::mapToExportDao)
							.toList();
					byte[] excelBytes = ExcelGenerator.generateExcel(courseExportDaos, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "program_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else {
				if (filter.isPageable()) {
					return ResponseBuilder.success(enrichScheduleFiles(programs), ApiResponse.LIST_FETCHED_SUCCESSFULLY,
							pageResult.getTotalPages(), pageResult.getTotalElements());
				} else {
					return ResponseBuilder.success(enrichScheduleFiles(programs),
							ApiResponse.LIST_FETCHED_SUCCESSFULLY);
				}
			}

		} catch (Exception e) {
			log.error("Error fetching program list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

	private List<CourseDetailsView> enrichScheduleFiles(List<CourseDetailsView> courses) {
		Set<Long> fileIds = courses.stream()
				.map(CourseDetailsView::getScheduleFileId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, ScheduleFile> scheduleFileMap = scheduleFileRepo.findAllById(fileIds).stream()
				.collect(Collectors.toMap(ScheduleFile::getId, Function.identity()));

		for (CourseDetailsView course : courses) {
			if (course.getScheduleFileId() != null) {
				course.setScheduleFile(scheduleFileMap.get(course.getScheduleFileId()));
			}
		}

		return courses;
	}
}
