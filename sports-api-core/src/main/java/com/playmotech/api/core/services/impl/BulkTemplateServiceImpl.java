package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CoachDetailsDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.TraineeDetailsDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.UserExportDetails;
import com.playmotech.api.core.services.BulkTemplateService;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.MiscellaneousService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.utils.excel.CoachExcelTemplateGenerator;
import com.playmotech.api.core.utils.excel.PlayerExcelTemplateGenerator;
import com.playmotech.api.core.utils.excel.ProgramCoachExcelTemplateGenerator;
import com.playmotech.api.core.utils.excel.ProgramExcelTemplateGenerator;
import com.playmotech.api.core.utils.excel.ProgramPlayerEnrollmentTemplateGenerator;
import com.playmotech.api.core.utils.excel.ProgramPlayerExcelTemplateGenerator;
import com.playmotech.api.core.views.TraineeView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BulkTemplateServiceImpl implements BulkTemplateService {

	private final ICourseService courseService;
	private final ICoachService coachService;
	private final ITraineeService traineeService;

	private final MiscellaneousService miscellaneousService;

	private final AcademyDomainUtil academyDomainUtil;

	private final CoachAcademyMappingRepo coachAcademyMappingRepo;

	private final RolesRepo rolesRepo;

	private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;

	private final AcademyRepo academyRepo;

	@Override
	public ServiceResponse downloadCoachTemplate(String path, String academyId, boolean edit) {
		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found"));
		String cleanAcademyName = academy.getName().trim().replaceAll("\\s+", "_").toLowerCase();

		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("dd_MMM_yyyy_hh_mm_ss_a", Locale.ENGLISH));

		String fileName = edit ? String.format("%s_coach_template_with_data_%s.xlsx", cleanAcademyName, timestamp)
				: String.format("%s_coach_template_%s.xlsx", cleanAcademyName, timestamp);
		if (edit) {
			List<UserExportDetails> existingCoaches = getCoachesByAcademyId(academyId);
			return generateExcel(path, fileName,
					CoachExcelTemplateGenerator.generateCoachTemplateWithData(existingCoaches), true);
		} else {
			return generateExcel(path, fileName, CoachExcelTemplateGenerator.generateCoachTemplate(), true);
		}
	}

	// Helper method to get all coaches with their details for a specific academy
	public List<UserExportDetails> getCoachesByAcademyId(String academyId) {

		List<Roles> roles = rolesRepo.findAll();

		// Fetch only active coach mappings for the given academy
		List<CoachAcademyMapping> coachMappings = coachAcademyMappingRepo.findByAcademy_Id(academyId);

		return coachMappings.stream().map(CoachAcademyMapping::getCoachUserProfile).filter(Objects::nonNull)
				.map(userProfile -> {
					UserExportDetails details = toUserExportDetails(userProfile, UserType.COACH);

					// Get corresponding mapping for this user
					CoachAcademyMapping mapping = coachMappings.stream()
							.filter(m -> m.getCoachUserProfile().getId().equals(userProfile.getId())).findFirst()
							.orElse(null);

					if (mapping != null) {
						details.setDesignation(mapping.getDesignation());
						details.setExperienceInMonths(mapping.getExperienceInMonths());
						details.setUserType(UserType.COACH);

						// Match roleId to Roles entity and then to Role enum
						Long roleId = mapping.getRoleId(); // Assuming this exists in mapping
						Optional<Roles> matchedRole = roles.stream().filter(r -> r.getId().equals(roleId)).findFirst();

						matchedRole.ifPresent(roleEntity -> {
							try {
								Role roleEnum = Role.valueOf(roleEntity.getRoleName().toUpperCase().replace(" ", "_"));
								details.setRole(roleEnum);
							} catch (IllegalArgumentException ex) {
//								log.warn("Invalid role name '{}' found for user ID {}", roleEntity.getRoleName(),
//										userProfile.getId());
							}
						});
					}

					return details;
				})
				// ❌ Filter out if role is ACADEMY_OWNER
				.filter(details -> details.getRole() != Role.ACADEMY_OWNER).collect(Collectors.toList());
	}

	// Dynamic helper method to convert UserProfile to UserExportDetails
	private UserExportDetails toUserExportDetails(UserProfile userProfile, UserType userType) {
		UserExportDetails details = new UserExportDetails();

		details.setId(userProfile.getId());
		details.setDisplayName(userProfile.getDisplayName());
		details.setDob(userProfile.getDob() != null ? userProfile.getDob().toString() : null);
		details.setEmailId(userProfile.getEmailId());
		details.setPhoneNumber(userProfile.getPhoneNumber());
		details.setGender(userProfile.getGender());
		details.setAddressLine1(userProfile.getAddressLine1());
		details.setAddressLine2(userProfile.getAddressLine2());
		details.setPincode(userProfile.getPincode());
		details.setCity(userProfile.getCity());
		details.setState(userProfile.getState());
		details.setCountry(userProfile.getCountry());
		details.setUserType(userType);

		return details;
	}

	@Override
	public ServiceResponse downloadPlayerTemplate(String path, String academyId, boolean edit) {
		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found"));
		String cleanAcademyName = academy.getName().trim().replaceAll("\\s+", "_").toLowerCase();

		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("dd_MMM_yyyy_hh_mm_ss_a", Locale.ENGLISH));

		String fileName = edit ? String.format("%s_player_template_with_data_%s.xlsx", cleanAcademyName, timestamp)
				: String.format("%s_player_template_%s.xlsx", cleanAcademyName, timestamp);
		if (edit) {
			List<UserExportDetails> existingPlayers = getPlayersByAcademyId(academyId);
			return generateExcel(path, fileName,
					PlayerExcelTemplateGenerator.generatePlayerTemplateWithData(existingPlayers), true);
		} else {
			return generateExcel(path, fileName, PlayerExcelTemplateGenerator.generatePlayerTemplate(), true);
		}
	}

	// Helper method to get all players with their details for a specific academy
	public List<UserExportDetails> getPlayersByAcademyId(String academyId) {
		// Fetch only active trainee mappings for the given academy
		List<TraineeAcademyMapping> traineeMappings = traineeAcademyMappingRepo.findByAcademy_Id(academyId);

		return traineeMappings.stream().map(TraineeAcademyMapping::getTraineeUserProfile).filter(Objects::nonNull)
				.map(userProfile -> {
					UserExportDetails details = toUserExportDetails(userProfile, UserType.PLAYER);
					// Set player-specific details
					details.setUserType(UserType.PLAYER);

					return details;
				}).collect(Collectors.toList());
	}

	@Override
	public ServiceResponse downloadProgramTemplate(String academyId, String path) {

		ServiceResponse response;

		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found"));
		String cleanAcademyName = academy.getName().trim().replaceAll("\\s+", "_").toLowerCase();

		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("dd_MMM_yyyy_hh_mm_ss_a", Locale.ENGLISH));

		String fileName = String.format("%s_program_template_%s.xlsx", cleanAcademyName, timestamp);

		List<Sports> sportsList;

		if (academyId != null && !academyId.isBlank()) {
			response = miscellaneousService.getSportsByAcademy(academyId);
			if (response.getHttpStatus().is2xxSuccessful() && response.getBody() != null) {
				sportsList = (List<Sports>) response.getBody();
				if (sportsList.isEmpty()) {
					// Fallback to all sports if academy has no sports configured
					response = miscellaneousService.getAllSports();
					sportsList = (List<Sports>) response.getBody();
				}
			} else {
				// Fallback to all sports if there was an error
				response = miscellaneousService.getAllSports();
				sportsList = (List<Sports>) response.getBody();
			}
		} else {
			response = miscellaneousService.getAllSports();
			sportsList = (List<Sports>) response.getBody();
		}

		return generateExcel(path, fileName, ProgramExcelTemplateGenerator.generateProgramTemplate(sportsList), true);
	}

	@Override
	public ServiceResponse downloadProgramEnrollmentTemplate(String academyId, String path) {

		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found"));
		String cleanAcademyName = academy.getName().trim().replaceAll("\\s+", "_").toLowerCase();

		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("dd_MMM_yyyy_hh_mm_ss_a", Locale.ENGLISH));

		String fileName = String.format("%s_program_enrollment_template_%s.xlsx", cleanAcademyName, timestamp);

		return generateExcel(path, fileName,
				ProgramPlayerEnrollmentTemplateGenerator.generateProgramPlayerEnrollmentTemplate(), true);
	}

	// Overloaded methods without path parameter (Base64 default)
	public ServiceResponse downloadCoachTemplate() {
		return generateExcel(null, "coach_template.xlsx", CoachExcelTemplateGenerator.generateCoachTemplate(), true);
	}

	public ServiceResponse downloadPlayerTemplate() {
		return generateExcel(null, "player_template.xlsx", PlayerExcelTemplateGenerator.generatePlayerTemplate(), true);
	}

	public ServiceResponse downloadProgramTemplate(String academyId) {
		ServiceResponse response;
		List<Sports> sportsList;

		if (academyId != null && !academyId.isBlank()) {
			response = miscellaneousService.getSportsByAcademy(academyId);
			if (response.getHttpStatus().is2xxSuccessful() && response.getBody() != null) {
				sportsList = (List<Sports>) response.getBody();
				if (sportsList.isEmpty()) {
					// Fallback to all sports if academy has no sports configured
					response = miscellaneousService.getAllSports();
					sportsList = (List<Sports>) response.getBody();
				}
			} else {
				// Fallback to all sports if there was an error
				response = miscellaneousService.getAllSports();
				sportsList = (List<Sports>) response.getBody();
			}
		} else {
			response = miscellaneousService.getAllSports();
			sportsList = (List<Sports>) response.getBody();
		}

		return generateExcel(null, "program_template.xlsx",
				ProgramExcelTemplateGenerator.generateProgramTemplate(sportsList), true);
	}

	public ServiceResponse downloadProgramEnrollmentTemplate(String academyId) {
		return generateExcel(null, "program_enrollment_template.xlsx",
				ProgramPlayerEnrollmentTemplateGenerator.generateProgramPlayerEnrollmentTemplate(), true);
	}

	@Override
	public ServiceResponse downloadProgramPlayerTemplate(String userId, String academyId, String path) {
		try {

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = "";
			}

			// Fetch courses for the academy
			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);

			// Fetch trainees for the academy
			List<TraineeDetailsDto> trainees = traineeService.getTraineesByAcademyIdAndNameAndPhoneNumber(academyId,
					null, null, null);

			GenericFilter filter = GenericFilter.builder().academyId(academyId).build();

			ServiceResponse serviceResponse = traineeService.getAllTrainess(filter, academyDomain);

			List<TraineeView> traineesList = (List<TraineeView>) serviceResponse.getBody();

			// Handle empty data to prevent template errors
			if (coursesDto.isEmpty() || traineesList.isEmpty()) {
				return ResponseBuilder.error("No courses or players found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			// Generate the template with the actual data
			Workbook workbook = ProgramPlayerExcelTemplateGenerator.generateProgramPlayerTemplate(coursesDto,
					traineesList);

			return generateExcel(path, "program_player_template.xlsx", workbook, true);
		} catch (ResourceException e) {
			e.printStackTrace();
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Overloaded method without path parameter
	public ServiceResponse downloadProgramPlayerTemplate(String userId, String academyId) {
		try {
			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = "";
			}

			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);

			List<TraineeDetailsDto> trainees = traineeService.getTraineesByAcademyIdAndNameAndPhoneNumber(academyId,
					null, null, null);

			GenericFilter filter = GenericFilter.builder().academyId(academyId).build();
			ServiceResponse serviceResponse = traineeService.getAllTrainess(filter, academyDomain);
			List<TraineeView> traineesList = (List<TraineeView>) serviceResponse.getBody();

			if (coursesDto.isEmpty() || traineesList.isEmpty()) {
				return ResponseBuilder.error("No courses or players found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			Workbook workbook = ProgramPlayerExcelTemplateGenerator.generateProgramPlayerTemplate(coursesDto,
					traineesList);

			return generateExcel(null, "program_player_template.xlsx", workbook, true);
		} catch (ResourceException e) {
			e.printStackTrace();
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse downloadProgramCoachTemplate(String userId, String academyId, String path) {
		try {
			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);
			List<CoachDetailsDto> coaches = coachService.getCoachesByAcademyIdAndNameAndPhoneNumber(academyId, null,
					null, null, false);

			// Handle empty data to prevent template errors
			if (coursesDto.isEmpty() || coaches.isEmpty()) {
				return ResponseBuilder.error("No courses or coaches found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			Workbook workbook = ProgramCoachExcelTemplateGenerator.generateProgramCoachTemplate(coursesDto, coaches);
			return generateExcel(path, "program_coach_template.xlsx", workbook, true);
		} catch (ResourceException e) {
			// Log the exception properly
			return ResponseBuilder.error("Error generating template: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Overloaded method without path parameter
	public ServiceResponse downloadProgramCoachTemplate(String userId, String academyId) {
		try {
			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);
			List<CoachDetailsDto> coaches = coachService.getCoachesByAcademyIdAndNameAndPhoneNumber(academyId, null,
					null, null, false);

			if (coursesDto.isEmpty() || coaches.isEmpty()) {
				return ResponseBuilder.error("No courses or coaches found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			Workbook workbook = ProgramCoachExcelTemplateGenerator.generateProgramCoachTemplate(coursesDto, coaches);
			return generateExcel(null, "program_coach_template.xlsx", workbook, true);
		} catch (ResourceException e) {
			return ResponseBuilder.error("Error generating template: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Overloaded method with default Base64 encoding
	private ServiceResponse generateExcel(String path, String fileName, Workbook workbook) {
		return generateExcel(path, fileName, workbook, true);
	}

	// Main method with Base64 option
	private ServiceResponse generateExcel(String path, String fileName, Workbook workbook, boolean useBase64) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			workbook.write(bos);
			workbook.close();

			if (useBase64) {
				// Base64 encoding logic (default) - path is ignored
				byte[] fileBytes = bos.toByteArray();
				String base64Encoded = Base64.getEncoder().encodeToString(fileBytes);

				// Create response map
				Map<String, Object> responseBody = new HashMap<>();
				responseBody.put("fileName", fileName);
				responseBody.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				responseBody.put("fileContent", base64Encoded);

				return ResponseBuilder.success(responseBody, ApiResponse.DATA_ADDED_SUCCESSFULLY, HttpStatus.OK);
			} else {
				// File writing logic (legacy) - path validation required
				if (path == null || path.trim().isEmpty()) {
					return ResponseBuilder.error(ApiResponse.INVALID_PATH, HttpStatus.BAD_REQUEST);
				}

				Path directoryPath = Paths.get(path);
				if (!Files.exists(directoryPath)) {
					return ResponseBuilder.error(ApiResponse.PATH_NOT_FOUND, HttpStatus.BAD_REQUEST);
				}
				if (!Files.isDirectory(directoryPath)) {
					return ResponseBuilder.error(ApiResponse.NOT_A_DIRECTORY, HttpStatus.BAD_REQUEST);
				}
				if (!Files.isWritable(directoryPath)) {
					return ResponseBuilder.error(ApiResponse.WRITE_PERMISSION_DENIED, HttpStatus.FORBIDDEN);
				}

				Path filePath = directoryPath.resolve(fileName);
				boolean fileExisted = Files.exists(filePath);

				Files.write(filePath, bos.toByteArray(), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);

				return fileExisted
						? ResponseBuilder.success("Template overwritten at: " + filePath,
								ApiResponse.TEMPLATE_OVERWRITTEN, HttpStatus.OK)
						: ResponseBuilder.success("Template created at: " + filePath, ApiResponse.TEMPLATE_CREATED,
								HttpStatus.CREATED);
			}

		} catch (IOException e) {
			return ResponseBuilder.error("Failed to process Excel file: " + e.getMessage(),
					ApiResponse.FILE_WRITE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Additional method for file writing when needed
	public ServiceResponse downloadCoachTemplateToFile(String path) {
		return generateExcel(path, "coach_template.xlsx", CoachExcelTemplateGenerator.generateCoachTemplate(), false);
	}

	public ServiceResponse downloadPlayerTemplateToFile(String path) {
		return generateExcel(path, "player_template.xlsx", PlayerExcelTemplateGenerator.generatePlayerTemplate(),
				false);
	}

	public ServiceResponse downloadProgramTemplateToFile(String academyId, String path) {
		return generateExcel(path, "program_template.xlsx", ProgramExcelTemplateGenerator.generateProgramTemplate(),
				false);
	}

	public ServiceResponse downloadProgramEnrollmentTemplateToFile(String academyId, String path) {
		return generateExcel(path, "program_enrollment_template.xlsx",
				ProgramPlayerEnrollmentTemplateGenerator.generateProgramPlayerEnrollmentTemplate(), false);
	}

	public ServiceResponse downloadProgramPlayerTemplateToFile(String userId, String academyId, String path) {
		try {
			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = "";
			}

			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);

			List<TraineeDetailsDto> trainees = traineeService.getTraineesByAcademyIdAndNameAndPhoneNumber(academyId,
					null, null, null);

			GenericFilter filter = GenericFilter.builder().academyId(academyId).build();
			ServiceResponse serviceResponse = traineeService.getAllTrainess(filter, academyDomain);
			List<TraineeView> traineesList = (List<TraineeView>) serviceResponse.getBody();

			if (coursesDto.isEmpty() || traineesList.isEmpty()) {
				return ResponseBuilder.error("No courses or players found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			Workbook workbook = ProgramPlayerExcelTemplateGenerator.generateProgramPlayerTemplate(coursesDto,
					traineesList);

			return generateExcel(path, "program_player_template.xlsx", workbook, false);
		} catch (ResourceException e) {
			e.printStackTrace();
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ServiceResponse downloadProgramCoachTemplateToFile(String userId, String academyId, String path) {
		try {
			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(userId, academyId, null, null, null, null,
					null, null, null, null);
			List<CoachDetailsDto> coaches = coachService.getCoachesByAcademyIdAndNameAndPhoneNumber(academyId, null,
					null, null, false);

			if (coursesDto.isEmpty() || coaches.isEmpty()) {
				return ResponseBuilder.error("No courses or coaches found to generate template.",
						ApiResponse.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
			}

			Workbook workbook = ProgramCoachExcelTemplateGenerator.generateProgramCoachTemplate(coursesDto, coaches);
			return generateExcel(path, "program_coach_template.xlsx", workbook, false);
		} catch (ResourceException e) {
			return ResponseBuilder.error("Error generating template: " + e.getMessage(), ApiResponse.UNEXPECTED_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}