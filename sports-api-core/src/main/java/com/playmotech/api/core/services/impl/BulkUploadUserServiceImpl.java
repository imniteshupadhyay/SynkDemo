package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CourseCoachMapping;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentDto;
import com.playmotech.api.core.dto.ProgramPlayerEnrollmentDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.AttendanceViewRepository;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CourseCoachMappingRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.excel.ErrorReportEntry;
import com.playmotech.api.core.response.excel.ErrorReportGenerator;
import com.playmotech.api.core.response.excel.ParsedRow;
import com.playmotech.api.core.response.excel.RowError;
import com.playmotech.api.core.services.BulkUploadUserService;
import com.playmotech.api.core.services.BulkUserService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.utils.excel.ExcelUserUploadUtil;
import com.playmotech.api.core.validation.ExcelValidator;
import com.playmotech.api.core.views.AttendanceView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadUserServiceImpl implements BulkUploadUserService {

	private final BulkUserService bulkUserService;
	private final ITraineeService traineeService;

	private final IPaymentService paymentService;

	private final RolesRepo rolesRepo;

	private final AcademyRepo academyRepo;

	private final CoachAcademyMappingRepo coachAcademyMappingRepo;

	private final AttendanceViewRepository attendanceViewRepository;

	private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;

	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;

	private final CourseCoachMappingRepo courseCoachMappingRepo;

	private final UserProfileRepo userProfileRepo;

	private final CourseRepo courseRepo;

	private final CourseService courseService;

	@Override
	public ServiceResponse bulkUploadProgramsFromExcel(MultipartFile file, String academyId, String userId) {
		List<ParsedRow> parsedRows;
		List<RowError> allErrors = new ArrayList<>();
		List<ValidatedProgramRow> validRows = new ArrayList<>();
		List<String> processedCourseIds = new ArrayList<>();

		try {
			parsedRows = ExcelUserUploadUtil.parseProgramExcelWithErrors(file);

			// Capture parsing errors
			parsedRows.stream().filter(ParsedRow::hasParsingErrors)
					.forEach(row -> allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors())));

			Set<String> existingTitles = courseRepo.findByAcademy_Id(academyId).stream().map(Course::getTitle)
					.collect(Collectors.toSet());

			Set<String> uploadTitles = new HashSet<>();

			// Process rows without parsing errors
			parsedRows.stream().filter(row -> !row.hasParsingErrors()).forEach(row -> {
				CreateCourseDto dto = row.getCourseDto();
				dto.setAcademyId(academyId);

				List<String> validationErrors = ExcelValidator.validateCourse(dto, row.getRowNumber());

				// Check for existing title in the academy
				if (existingTitles.contains(dto.getTitle())) {
					validationErrors.add("A course with this title already exists in the academy.");
				}

				// Check for duplicate title in the current upload
				if (uploadTitles.contains(dto.getTitle())) {
					validationErrors.add("Duplicate title found within the upload file.");
				}

				if (!validationErrors.isEmpty()) {
					allErrors.add(new RowError(row.getRowNumber(), validationErrors));
				} else {
					uploadTitles.add(dto.getTitle());
					validRows.add(new ValidatedProgramRow(row.getRowNumber(), dto));
				}
			});

			for (ValidatedProgramRow row : validRows) {
				CreateCourseDto createDto = row.getCreateCourseDto();
				try {
					CourseDto created = courseService.createCourse(academyId, userId, createDto, true);
					processedCourseIds.add(created.getId());
				} catch (ResourceException e) {
					// Handle exceptions such as database constraints due to race conditions
					allErrors.add(new RowError(row.getRowNumber(), List.of(e.getMessage())));
				}
			}

			// Handle errors
			if (!allErrors.isEmpty()) {
				List<ErrorReportEntry> entries = compileErrorReportEntries(parsedRows, allErrors);
				return ResponseBuilder.success(entries, ApiResponse.PARTIAL_UPLOAD, HttpStatus.MULTI_STATUS);
			}

			return ResponseBuilder.success(processedCourseIds, ApiResponse.COURSES_CREATED, HttpStatus.CREATED);

		} catch (IOException e) {
			return ResponseBuilder.error("Program upload failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// Fix for the bulkUploadPlayers method in BulkUploadUserServiceImpl
	private ServiceResponse bulkUploadPlayers(MultipartFile file, String academyId) {
		log.info("Bulk upload started for players. Academy ID: {}", academyId);

		// Validate input parameters
		if (file == null || file.isEmpty()) {
			log.error("File is null or empty");
			return ResponseBuilder.error("File is required and cannot be empty", HttpStatus.BAD_REQUEST);
		}

		if (academyId == null || academyId.trim().isEmpty()) {
			log.error("Academy ID is null or empty");
			return ResponseBuilder.error("Academy ID is required", HttpStatus.BAD_REQUEST);
		}

		List<ParsedRow> parsedRows = null;
		List<RowError> allErrors = new ArrayList<>();
		List<ValidatedRow> validRows = new ArrayList<>();
		Set<String> seenPhoneNumbersInExcel = new HashSet<>();

		try {
			// Parse Excel for players with proper exception handling
			try {
				parsedRows = ExcelUserUploadUtil.parsePlayerExcelWithErrors(file);
				log.info("Excel parsing completed for players");
			} catch (Exception parseException) {
				log.error("Exception occurred during Excel parsing for players", parseException);
				return ResponseBuilder.error("Failed to parse Excel file: " + parseException.getMessage(),
						HttpStatus.BAD_REQUEST);
			}

			// Critical null check with immediate return
			if (parsedRows == null) {
				log.error("Excel parsing returned null for player file - this indicates a critical parsing issue");
				return ResponseBuilder.error(
						"Failed to parse Excel file - the file may be corrupted or in an unsupported format",
						HttpStatus.BAD_REQUEST);
			}

			log.info("Player Excel file parsed successfully. Total rows: {}", parsedRows.size());

			// Check if the list is empty
			if (parsedRows.isEmpty()) {
				log.warn("Excel file contains no data rows");
				return ResponseBuilder.error("Excel file contains no data to process", HttpStatus.BAD_REQUEST);
			}

			// Process parsing errors with additional safety checks
			try {
				for (ParsedRow row : parsedRows) {
					if (row == null) {
						log.warn("Encountered null row in parsed data");
						continue;
					}

					if (row.hasParsingErrors()) {
						log.warn("Parsing error at row {}: {}", row.getRowNumber(), row.getParsingErrors());
						allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors()));
					}
				}
			} catch (Exception e) {
				log.error("Error while processing parsing errors", e);
				return ResponseBuilder.error("Error processing Excel data: " + e.getMessage(), HttpStatus.BAD_REQUEST);
			}

			// Validate rows and check duplicates in Excel with try-catch
			try {
				validateAndPreparePlayerRows(parsedRows, validRows, allErrors, seenPhoneNumbersInExcel);
			} catch (Exception validationException) {
				log.error("Error during player row validation", validationException);
				return ResponseBuilder.error("Error validating player data: " + validationException.getMessage(),
						HttpStatus.BAD_REQUEST);
			}

			// Process valid player rows with error handling
			int success = 0;
			try {
				success = processValidPlayerRows(validRows, allErrors, academyId);
			} catch (Exception processingException) {
				log.error("Error during processing valid player rows", processingException);
				return ResponseBuilder.error("Error processing player data: " + processingException.getMessage(),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}

			return generateResponse(allErrors, success, "player", parsedRows);

		} catch (IOException e) {
			log.error("File reading error", e);
			return ResponseBuilder.error("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Unhandled exception during player bulk upload", e);
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ServiceResponse generateResponse(List<RowError> allErrors, int success, String userType,
			List<ParsedRow> parsedRows) throws IOException {
		if (!allErrors.isEmpty()) {
			List<ErrorReportEntry> entries = compileErrorReportEntries(parsedRows, allErrors);
			ByteArrayOutputStream report = ErrorReportGenerator.generate(entries,
					ErrorReportGenerator.TemplateType.PROGRAM_PLAYER_ENROLLMENT_HEADERS);
			// String errorUrl = storeErrorReport(report, userType);

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("errors", entries);
			responseBody.put("fileContent", parsedRows);

			return ResponseBuilder.success(responseBody, ApiResponse.PARTIAL_UPLOAD, HttpStatus.ACCEPTED);
		}

		return ResponseBuilder.success(null, ApiResponse.USERS_UPLOADED, HttpStatus.CREATED);
	}

	// Enhanced validation method with comprehensive null checks
	private void validateAndPreparePlayerRows(List<ParsedRow> parsedRows, List<ValidatedRow> validRows,
			List<RowError> allErrors, Set<String> seenPhoneNumbersInExcel) {

		// First line of defense - critical null check
		if (parsedRows == null) {
			log.error("parsedRows is null in validateAndPreparePlayerRows - this should not happen");
			throw new IllegalArgumentException("Parsed rows cannot be null");
		}

		if (parsedRows.isEmpty()) {
			log.warn("No rows to validate for players - parsedRows is empty");
			return;
		}

		log.info("Starting validation for {} player rows", parsedRows.size());

		try {
			for (ParsedRow row : parsedRows) {
				// Skip null rows
				if (row == null) {
					log.warn("Encountered null row in parsed data");
					continue;
				}

				// Skip rows with parsing errors (they're already handled)
				if (row.hasParsingErrors()) {
					continue;
				}

				UserProfileDto dto = row.getUserDto();
				if (dto == null) {
					log.warn("UserDto is null for row {}", row.getRowNumber());
					allErrors.add(new RowError(row.getRowNumber(), List.of("User data is missing")));
					continue;
				}

				List<String> rowErrors = new ArrayList<>();

				try {
					// Format phone number and set user details
					String phoneNumber = dto.getPhoneNumber();
					if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
						rowErrors.add("Phone number is required");
					} else {
						String formattedPhone = formatPhoneNumber(phoneNumber);
						dto.setPhoneNumber(formattedPhone);
						dto.setUsername(formattedPhone);

						// Check for duplicate phone numbers in Excel
						if (!seenPhoneNumbersInExcel.add(formattedPhone)) {
							rowErrors.add("Duplicate phone number in Excel: " + dto.getPhoneNumber());
						}
					}

					dto.setUserType(UserType.PLAYER);

					// Set default role for player
					if (dto.getRole() == null) {
						dto.setRole(Role.PLAYER);
					}

					// Validate role and other fields
					validateRole(dto, rowErrors);
					List<String> validationErrors = ExcelValidator.validateUserProfile(dto, row.getRowNumber());
					if (validationErrors != null) {
						rowErrors.addAll(validationErrors);
					}

				} catch (Exception rowException) {
					log.error("Error processing row {}: {}", row.getRowNumber(), rowException.getMessage());
					rowErrors.add("Error processing row data: " + rowException.getMessage());
				}

				if (!rowErrors.isEmpty()) {
					allErrors.add(new RowError(row.getRowNumber(), rowErrors));
				} else {
					validRows.add(new ValidatedRow(row.getRowNumber(), dto));
				}
			}

			log.info("Validation completed. Valid rows: {}, Error rows: {}", validRows.size(), allErrors.size());

		} catch (Exception e) {
			log.error("Unexpected error during player row validation", e);
			throw new RuntimeException("Error during player row validation: " + e.getMessage(), e);
		}
	}

	// Also fix the bulkUploadCoaches method with the same pattern
	private ServiceResponse bulkUploadCoaches(MultipartFile file, String academyId) {
		log.info("Bulk upload started for coaches. Academy ID: {}", academyId);

		// Validate input parameters
		if (file == null || file.isEmpty()) {
			log.error("File is null or empty");
			return ResponseBuilder.error("File is required and cannot be empty", HttpStatus.BAD_REQUEST);
		}

		if (academyId == null || academyId.trim().isEmpty()) {
			log.error("Academy ID is null or empty");
			return ResponseBuilder.error("Academy ID is required", HttpStatus.BAD_REQUEST);
		}

		List<ParsedRow> parsedRows;
		List<RowError> allErrors = new ArrayList<>();
		List<ValidatedRow> validRows = new ArrayList<>();
		Set<String> seenPhoneNumbersInExcel = new HashSet<>();

		try {
			// Parse Excel for coaches
			parsedRows = ExcelUserUploadUtil.parseCoachExcelWithErrors(file);

			// Check if parsing returned null or empty
			if (parsedRows == null) {
				log.error("Excel parsing returned null for coach file");
				return ResponseBuilder.error("Failed to parse Excel file - invalid or empty file",
						HttpStatus.BAD_REQUEST);
			}

			log.info("Coach Excel file parsed. Total rows: {}", parsedRows.size());

			if (parsedRows.isEmpty()) {
				log.warn("Excel file contains no data rows");
				return ResponseBuilder.error("Excel file contains no data to process", HttpStatus.BAD_REQUEST);
			}

			// Process parsing errors
			parsedRows.stream().filter(ParsedRow::hasParsingErrors).forEach(row -> {
				log.warn("Parsing error at row {}: {}", row.getRowNumber(), row.getParsingErrors());
				allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors()));
			});

			// Validate rows and check duplicates in Excel - ONLY if parsedRows is not null
			validateAndPrepareCoachRows(parsedRows, validRows, allErrors, seenPhoneNumbersInExcel, academyId);

			// Process valid coach rows
			int success = processValidCoachRows(validRows, allErrors, academyId);

			return generateResponse(allErrors, success, "coach", parsedRows);

		} catch (IOException e) {
			log.error("File reading error", e);
			return ResponseBuilder.error("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Unhandled exception during coach bulk upload", e);
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateAndPrepareCoachRows(List<ParsedRow> parsedRows, List<ValidatedRow> validRows,
			List<RowError> allErrors, Set<String> seenPhoneNumbersInExcel, String academyId) {
		// Additional null check for safety
		if (parsedRows == null || parsedRows.isEmpty()) {
			log.warn("No rows to validate for coaches");
			return;
		}

		List<String> academyIds = List.of(academyId);

		for (ParsedRow row : parsedRows) {
			// Add null check for individual row as well
			if (row == null) {
				log.warn("Encountered null row in parsed data");
				continue;
			}

			if (row.hasParsingErrors()) {
				continue;
			}

			UserProfileDto dto = row.getUserDto();
			if (dto == null) {
				log.warn("UserDto is null for row {}", row.getRowNumber());
				allErrors.add(new RowError(row.getRowNumber(), List.of("User data is missing")));
				continue;
			}

			List<String> rowErrors = new ArrayList<>();

			// Format phone number and set user details
			String formattedPhone = formatPhoneNumber(dto.getPhoneNumber());
			dto.setPhoneNumber(formattedPhone);
			dto.setUsername(formattedPhone);
			dto.setUserType(UserType.COACH);
			dto.setAcademyId(academyIds);

			// Set default role for coach
			if (dto.getRole() == null) {
				dto.setRole(Role.COACH);
			}

			// Check for duplicate phone numbers in Excel
			if (!seenPhoneNumbersInExcel.add(formattedPhone)) {
				rowErrors.add("Duplicate phone number in Excel: " + dto.getPhoneNumber());
			}

			// Validate role and other fields
			validateRole(dto, rowErrors);
			List<String> validationErrors = ExcelValidator.validateUserProfile(dto, row.getRowNumber());
			rowErrors.addAll(validationErrors);

			if (!rowErrors.isEmpty()) {
				allErrors.add(new RowError(row.getRowNumber(), rowErrors));
			} else {
				validRows.add(new ValidatedRow(row.getRowNumber(), dto));
			}
		}
	}

	private int processValidCoachRows(List<ValidatedRow> validRows, List<RowError> allErrors, String academyId) {
		int success = 0;

		for (ValidatedRow row : validRows) {
			UserProfileDto dto = row.getUserDto();
			try {
				// Check if user already exists
				List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(dto.getPhoneNumber(),
						false);
				UserProfile existingUser = existingUsers.isEmpty() ? null : existingUsers.get(0);

				if (existingUser != null) {
					// User exists - check type compatibility and academy mapping
					if (existingUser.getUserType() != UserType.COACH) {
						String msg = String.format("User exists as %s, cannot upload as COACH",
								existingUser.getUserType());
						allErrors.add(new RowError(row.getRowNumber(), List.of(msg)));
						continue;
					}

					// Check if coach is already mapped to this academy
					boolean isAlreadyMapped = coachAcademyMappingRepo
							.existsByCoachUserProfileIdAndAcademyId(existingUser.getId(), academyId);

					if (isAlreadyMapped) {
						String msg = "Coach already mapped to this academy";
						allErrors.add(new RowError(row.getRowNumber(), List.of(msg)));
						continue;
					} else {
						// Create new coach-academy mapping
						createCoachAcademyMapping(existingUser, dto, academyId);
						success++;
						log.info("Mapped existing coach {} to academy {}", existingUser.getId(), academyId);
					}
				} else {
					// Create new coach user
					ServiceResponse response = bulkUserService.create(dto);
					if (response.getHttpStatus().is2xxSuccessful()) {
						UserProfile newUser = (UserProfile) response.getBody();
						// Create coach-academy mapping for new user
						createCoachAcademyMapping(newUser, dto, academyId);
						success++;
						log.info("Created new coach {} and mapped to academy {}", newUser.getId(), academyId);
					} else {
						allErrors.add(new RowError(row.getRowNumber(), List.of(response.getMessage())));
					}
				}
			} catch (Exception e) {
				log.error("Error processing coach row {}: {}", row.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(), List.of("Unexpected error: " + e.getMessage())));
			}
		}

		return success;
	}

	private int processValidPlayerRows(List<ValidatedRow> validRows, List<RowError> allErrors, String academyId) {
		int success = 0;
		List<String> playerIdsToMap = new ArrayList<>();

		for (ValidatedRow row : validRows) {
			UserProfileDto dto = row.getUserDto();
			try {
				// Check if user already exists
				List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(dto.getPhoneNumber(),
						false);
				UserProfile existingUser = existingUsers.isEmpty() ? null : existingUsers.get(0);

				if (existingUser != null) {
					// User exists - check type compatibility and academy mapping
					if (existingUser.getUserType() != UserType.PLAYER) {
						String msg = String.format("User exists as %s, cannot upload as PLAYER",
								existingUser.getUserType());
						allErrors.add(new RowError(row.getRowNumber(), List.of(msg)));
						continue;
					}

					// Check if player is already mapped to this academy
					boolean isAlreadyMapped = traineeAcademyMappingRepo
							.existsByTraineeUserProfileIdAndAcademyId(existingUser.getId(), academyId);

					if (isAlreadyMapped) {
						String msg = "Player already mapped to this academy";
						allErrors.add(new RowError(row.getRowNumber(), List.of(msg)));
						continue;
					} else {
						// Add to list for batch academy mapping
						playerIdsToMap.add(existingUser.getId());
						success++;
						log.info("Found existing player {} to map to academy {}", existingUser.getId(), academyId);
					}
				} else {
					// Create new player user
					ServiceResponse response = bulkUserService.create(dto);
					if (response.getHttpStatus().is2xxSuccessful()) {
						UserProfile newUser = (UserProfile) response.getBody();
						playerIdsToMap.add(newUser.getId());
						success++;
						log.info("Created new player {} to map to academy {}", newUser.getId(), academyId);
					} else {
						allErrors.add(new RowError(row.getRowNumber(), List.of(response.getMessage())));
					}
				}
			} catch (Exception e) {
				log.error("Error processing player row {}: {}", row.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(), List.of("Unexpected error: " + e.getMessage())));
			}
		}

		// Batch add all players to academy
		if (!playerIdsToMap.isEmpty()) {
			try {
				traineeService.addTraineesToAcademy(playerIdsToMap, academyId);
				log.info("Successfully mapped {} players to academy {}", playerIdsToMap.size(), academyId);
			} catch (Exception e) {
				log.error("Failed to map players to academy: {}", e.getMessage(), e);
				// Since players were created successfully but mapping failed,
				// we should handle this as a partial success
				for (int i = 0; i < playerIdsToMap.size(); i++) {
					allErrors.add(new RowError(i + 1,
							List.of("Player created but failed to map to academy: " + e.getMessage())));
				}
			}
		}

		return success;
	}

	private void createCoachAcademyMapping(UserProfile coach, UserProfileDto dto, String academyId) {
		CoachAcademyMapping mapping = new CoachAcademyMapping();
		mapping.setId(UUID.randomUUID().toString());
		mapping.setCoachUserProfile(coach);

		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found with ID: " + academyId));
		mapping.setAcademy(academy);
		mapping.setCreatedOn(Timestamp.from(Instant.now()));

		// Set additional coach-specific fields from DTO
		mapping.setRoleId(dto.getRoleId());
		mapping.setDesignation(dto.getDesignation());
		mapping.setExperienceInMonths(dto.getExperienceInMonths());
		mapping.setStatus(Status.ACTIVE);

		coachAcademyMappingRepo.save(mapping);
	}

	/**
	 * Format phone number by ensuring it starts with "91"
	 */
	private String formatPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}
		// Remove any non-digit characters
		String digitsOnly = phoneNumber.replaceAll("\\D", "");
		// // Ensure it starts with "91"
		// if (!digitsOnly.startsWith("91")) {
		// digitsOnly = "91" + digitsOnly;
		// }
		return digitsOnly;
	}

	/**
	 * Validate role and set roleId if valid
	 */
	private void validateRole(UserProfileDto dto, List<String> errors) {
		if (dto.getRole() == null) {
			errors.add("Role cannot be null");
			return;
		}

		List<Roles> roles = rolesRepo.findByRoleName(dto.getRole().name());
		if (roles.isEmpty()) {
			errors.add("Invalid role: " + dto.getRole().name());
		} else {
			dto.setRoleId(roles.get(0).getId());
		}
	}

	// private String storeErrorReport(ByteArrayOutputStream reportStream, String
	// reportType) {
	// // Generate a dynamic filename based on the report type and current timestamp
	// String filename = reportType + "_errors_" + System.currentTimeMillis() +
	// ".xlsx";
	//
	// // Dynamically choose the directory path based on the report type (optional)
	// String baseDir = filePath != null ? filePath : "default/directory"; // Use
	// default if filePath is not provided
	// Path dirPath = Paths.get(baseDir);
	//
	// // Ensure the directory exists
	// try {
	// Files.createDirectories(dirPath);
	// } catch (IOException e) {
	// log.error("Unable to create directory {}", baseDir, e);
	// }
	//
	// // Create the full path for the file
	// Path file = dirPath.resolve(filename);
	//
	// // Write the error report to the specified file
	// try (OutputStream out = Files.newOutputStream(file,
	// StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
	// reportStream.writeTo(out);
	// } catch (IOException e) {
	// log.error("Failed to write error report to {}", file.toAbsolutePath(), e);
	// }
	//
	// // Log the success and return the full path of the stored file
	// log.info("Error report stored at {}", file.toAbsolutePath());
	// return file.toAbsolutePath().toString();
	// }

	private List<ErrorReportEntry> compileErrorReportEntries(List<ParsedRow> parsedRows, List<RowError> allErrors) {
		Map<Integer, List<String>> errorsByRow = allErrors.stream().collect(Collectors.groupingBy(
				RowError::getRowNumber, Collectors.flatMapping(e -> e.getMessages().stream(), Collectors.toList())));

		return parsedRows.stream().filter(row -> errorsByRow.containsKey(row.getRowNumber()))
				.map(row -> new ErrorReportEntry(row.getRowNumber(), row.getOriginalData(),
						errorsByRow.get(row.getRowNumber())))
				.collect(Collectors.toList());
	}

	@Override
	public ServiceResponse bulkUploadProgramsCoachesFromExcel(MultipartFile file, String academyId, String userId) {
		try {
			List<ParsedRow> parsedRows = ExcelUserUploadUtil.parseProgramCoachExcelWithErrors(file);
			List<RowError> allErrors = new ArrayList<>();
			List<CourseCoachMapping> mappingsToSave = new ArrayList<>();

			for (ParsedRow row : parsedRows) {
				if (row.hasParsingErrors()) {
					allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors()));
					continue;
				}

				String programId = row.getProgram().getProgramId();
				String coachId = row.getCoach().getCoachId();

				if (!StringUtils.hasText(programId) || !StringUtils.hasText(coachId)) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Program ID or Coach ID is missing")));
					continue;
				}

				Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(academyId, programId);
				if (courseOpt.isEmpty() || Boolean.TRUE.equals(courseOpt.get().getInactive())) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Program not found or inactive")));
					continue;
				}

				Optional<UserProfile> coachOpt = userProfileRepo.findById(coachId);
				if (coachOpt.isEmpty()) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Coach not found")));
					continue;
				}

				Course course = courseOpt.get();
				boolean mappingExists = courseCoachMappingRepo.existsByCourseIdAndCoachUserProfileId(course.getId(),
						coachId);
				if (!mappingExists) {
					Timestamp now = Timestamp.from(Instant.now());
					CourseCoachMapping mapping = new CourseCoachMapping();
					mapping.setCourse(course);
					mapping.setCoachUserProfile(coachOpt.get());
					mapping.setCreatedOn(now);
					mappingsToSave.add(mapping);
				}
			}

			// Save valid mappings
			if (!mappingsToSave.isEmpty()) {
				courseCoachMappingRepo.saveAll(mappingsToSave);
			}

			// Generate error report if any
			if (!allErrors.isEmpty()) {
				List<ErrorReportEntry> entries = compileErrorReportEntries(parsedRows, allErrors);
				ByteArrayOutputStream report = ErrorReportGenerator.generate(entries,
						ErrorReportGenerator.TemplateType.PROGRAM_COACH_MAPPING);
				// String errorUrl = storeErrorReport(report, "program-coach");
				return ResponseBuilder.success(ApiResponse.PARTIAL_UPLOAD, HttpStatus.MULTI_STATUS);
			}

			return ResponseBuilder.success(ApiResponse.COACHES_MAPPED);

		} catch (Exception e) {
			log.error("Error in bulkUploadProgramsCoachesFromExcel", e);
			return ResponseBuilder.badRequest(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	public ServiceResponse bulkUploadProgramsPlayersFromExcel(MultipartFile file, String academyId, String userId) {
		try {
			List<ParsedRow> parsedRows = ExcelUserUploadUtil.parseProgramPlayersExcelWithErrors(file);
			List<RowError> allErrors = new ArrayList<>();

			for (ParsedRow row : parsedRows) {
				if (row.hasParsingErrors()) {
					allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors()));
					continue;
				}

				String courseId = row.getProgram().getProgramId();
				String playerId = row.getPlayer().getPlayerId();

				if (!StringUtils.hasText(courseId) || !StringUtils.hasText(playerId)) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Program ID or Player ID is missing")));
					continue;
				}

				Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(academyId, courseId);
				if (courseOpt.isEmpty() || Boolean.TRUE.equals(courseOpt.get().getInactive())) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Program not found or inactive")));
					continue;
				}

				Optional<UserProfile> playerOpt = userProfileRepo.findById(playerId);
				if (playerOpt.isEmpty()) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Player not found")));
					continue;
				}

				Course course = courseOpt.get();
				UserProfile player = playerOpt.get();

				// Check if already enrolled
				List<TraineeCourseEnrollmentDto> existingEnrollments = courseService.getEnrolledTrainees(academyId,
						courseId);
				boolean alreadyEnrolled = existingEnrollments.stream()
						.anyMatch(e -> e.getUserProfile().getId().equals(playerId));
				if (alreadyEnrolled) {
					allErrors.add(new RowError(row.getRowNumber(), List.of("Player already enrolled in this program")));
					continue;
				}

				// Validate if paymentSchedule exists in CoursePaymentOptionsMapping
				boolean validPaymentOption = course.getPaymentOptions().stream().anyMatch(
						option -> option.getPaymentSchedule().equals(row.getEnrollmentDto().getPaymentSchedule()));
				if (!validPaymentOption) {
					allErrors.add(
							new RowError(row.getRowNumber(), List.of("Invalid payment schedule for this program")));
					continue;
				}

				// Create new enrollment
				TraineeCourseEnrollment enrollment = new TraineeCourseEnrollment();
				enrollment.setId(UUID.randomUUID().toString());
				enrollment.setTraineeUserProfile(player);
				enrollment.setCourse(course);
				enrollment.setAcademy(Academy.builder().id(academyId).build());
				enrollment.setStatus(Status.ACTIVE);
				enrollment.setPaymentSchedule(row.getEnrollmentDto().getPaymentSchedule());
				enrollment.setAmount(row.getEnrollmentDto().getAmount());
				enrollment.setCreatedOn(Timestamp.from(Instant.now()));

				if (row.getEnrollmentDto().getJoiningDate() != null) {
					enrollment.setJoiningDate(row.getEnrollmentDto().getJoiningDate());
				} else {
					enrollment.setJoiningDate(LocalDate.now());
				}
				if (row.getEnrollmentDto().getDueDate() != null) {
					enrollment.setDueDate(row.getEnrollmentDto().getDueDate());
				}

				enrollment.setUseForFuture(Boolean.FALSE);
				enrollment.setDiscountAmount(0L);

				// Save enrollment
				traineeCourseEnrollmentRepo.save(enrollment);
			}

			// If any errors, generate an error report
			if (!allErrors.isEmpty()) {
				List<ErrorReportEntry> entries = compileErrorReportEntries(parsedRows, allErrors);
				ByteArrayOutputStream report = ErrorReportGenerator.generate(entries,
						ErrorReportGenerator.TemplateType.PROGRAM_PLAYER_MAPPING);
				// String errorUrl = storeErrorReport(report, "program-player");
				return ResponseBuilder.success(ApiResponse.PARTIAL_UPLOAD, HttpStatus.MULTI_STATUS);
			}

			return ResponseBuilder.success(ApiResponse.PLAYERS_ENROLLED_SUCCESSFULLY);

		} catch (Exception e) {
			log.error("Error in bulkUploadProgramsPlayersFromExcel", e);
			return ResponseBuilder.badRequest(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	public ServiceResponse bulkUploadProgramsMapPlayersFromExcel(MultipartFile file, String academyId, String programId,
			String userId) {
		log.info("Starting bulk upload for program player enrollment. Academy ID: {}, Program ID: {}", academyId,
				programId);

		// Validate input parameters
		if (file == null || file.isEmpty()) {
			log.error("File is null or empty");
			return ResponseBuilder.error("File is required and cannot be empty", HttpStatus.BAD_REQUEST);
		}

		if (academyId == null || academyId.trim().isEmpty()) {
			log.error("Academy ID is null or empty");
			return ResponseBuilder.error("Academy ID is required", HttpStatus.BAD_REQUEST);
		}

		if (programId == null || programId.trim().isEmpty()) {
			log.error("Program ID is null or empty");
			return ResponseBuilder.error("Program ID is required", HttpStatus.BAD_REQUEST);
		}

		// Verify program exists and belongs to academy
		Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(academyId, programId);
		if (courseOpt.isEmpty() || Boolean.TRUE.equals(courseOpt.get().getInactive())) {
			return ResponseBuilder.error("Program not found or inactive", HttpStatus.BAD_REQUEST);
		}

		Course course = courseOpt.get();
		List<ParsedRow> parsedRows;
		List<RowError> allErrors = new ArrayList<>();
		List<ValidatedProgramPlayerEnrollmentRow> validRows = new ArrayList<>();

		try {
			// Parse Excel file
			parsedRows = ExcelUserUploadUtil.parseProgramPlayerEnrollmentExcelWithErrors(file);

			if (parsedRows == null || parsedRows.isEmpty()) {
				return ResponseBuilder.error("Excel file contains no data to process", HttpStatus.BAD_REQUEST);
			}

			log.info("Program player enrollment Excel file parsed. Total rows: {}", parsedRows.size());

			// Process parsing errors
			parsedRows.stream().filter(ParsedRow::hasParsingErrors)
					.forEach(row -> allErrors.add(new RowError(row.getRowNumber(), row.getParsingErrors())));

			// Validate and prepare enrollment rows
			validateAndPrepareProgramPlayerEnrollmentRows(parsedRows, validRows, allErrors, academyId, programId,
					course);

			// Process valid enrollment rows
			int success = processValidProgramPlayerEnrollmentRows(validRows, allErrors, course);

			return generateResponse(allErrors, success, "program-player-enrollment", parsedRows);

		} catch (IOException e) {
			log.error("File reading error", e);
			return ResponseBuilder.error("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Unhandled exception during program player enrollment bulk upload", e);
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateAndPrepareProgramPlayerEnrollmentRows(List<ParsedRow> parsedRows,
			List<ValidatedProgramPlayerEnrollmentRow> validRows, List<RowError> allErrors, String academyId,
			String programId, Course course) {

		if (parsedRows == null || parsedRows.isEmpty()) {
			log.warn("No rows to validate for program player enrollment");
			return;
		}

		log.info("Starting validation for {} program player enrollment rows", parsedRows.size());

		for (ParsedRow row : parsedRows) {
			if (row == null || row.hasParsingErrors()) {
				continue;
			}

			ProgramPlayerEnrollmentDto enrollmentDto = row.getPlayerEnrollmentDto();
			if (enrollmentDto == null) {
				allErrors.add(new RowError(row.getRowNumber(), List.of("Enrollment data is missing")));
				continue;
			}

			List<String> rowErrors = new ArrayList<>();

			try {
				// Validate if paymentSchedule exists in CoursePaymentOptionsMapping
				if (enrollmentDto.getPaymentSchedule() != null) {
					boolean validPaymentOption = course.getPaymentOptions().stream()
							.anyMatch(option -> option.getPaymentSchedule().equals(enrollmentDto.getPaymentSchedule()));
					if (!validPaymentOption) {
						rowErrors.add("Invalid payment schedule for this program");
					}
				}

				// Format phone number for database lookup
				String formattedPhone = formatPhoneNumber(enrollmentDto.getPhoneNumber());

				// Find user by phone number
				List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(formattedPhone, false);

				if (existingUsers.isEmpty()) {
					rowErrors.add("User with phone number " + enrollmentDto.getPhoneNumber() + " not found");
				} else {
					UserProfile user = existingUsers.get(0);

					// Check if user type is PLAYER
					if (user.getUserType() != UserType.PLAYER) {
						rowErrors.add("User is not a player, cannot enroll in program");
					} else {
						// Check if full name matches (case-insensitive, trimmed)
						String dbName = user.getDisplayName() != null ? user.getDisplayName().trim().toLowerCase() : "";
						String excelName = enrollmentDto.getFullName() != null
								? enrollmentDto.getFullName().trim().toLowerCase()
								: "";

						if (!dbName.equals(excelName)) {
							rowErrors.add("Full name mismatch. Database: '" + user.getDisplayName() + "', Excel: '"
									+ enrollmentDto.getFullName() + "'");
						} else {
							// Check if user is enrolled in this academy
							boolean isInAcademy = traineeAcademyMappingRepo
									.existsByTraineeUserProfileIdAndAcademyId(user.getId(), academyId);

							if (!isInAcademy) {
								rowErrors.add("User is not enrolled in this academy");
							} else {
								// Check if already enrolled in this specific program
								boolean isAlreadyEnrolled = traineeCourseEnrollmentRepo
										.existsByCourseIdAndTraineeUserProfileIdAndStatus(programId, user.getId(),
												Status.ACTIVE);

								if (isAlreadyEnrolled) {
									rowErrors.add("User is already enrolled in this program");
								} else {
									// Check if user has inactive enrollment in this program
									boolean hasInactiveEnrollment = traineeCourseEnrollmentRepo
											.existsByCourseIdAndTraineeUserProfileIdAndStatus(programId, user.getId(),
													Status.INACTIVE);

									if (hasInactiveEnrollment) {
										rowErrors.add(
												"User has inactive enrollment in this program. Please reactivate existing enrollment.");
									}
								}
							}
						}
					}

					// If no errors so far, store the user for processing
					if (rowErrors.isEmpty()) {
						enrollmentDto.setUserProfile(user);
					}
				}

				// Validate dates
				if (enrollmentDto.getJoiningDate() != null && enrollmentDto.getNextDueDate() != null) {
					if (enrollmentDto.getJoiningDate().isAfter(enrollmentDto.getNextDueDate())) {
						rowErrors.add("Joining date cannot be after due date");
					}
				}

				// Validate fee amount
				if (enrollmentDto.getFeeAmount() != null && enrollmentDto.getFeeAmount() <= 0) {
					rowErrors.add("Fee amount must be greater than 0");
				}

			} catch (Exception e) {
				log.error("Error validating enrollment row {}: {}", row.getRowNumber(), e.getMessage());
				rowErrors.add("Error processing row: " + e.getMessage());
			}

			if (!rowErrors.isEmpty()) {
				allErrors.add(new RowError(row.getRowNumber(), rowErrors));
			} else {
				validRows.add(new ValidatedProgramPlayerEnrollmentRow(row.getRowNumber(), enrollmentDto));
			}
		}

		log.info("Validation completed. Valid rows: {}, Error rows: {}", validRows.size(), allErrors.size());
	}

	private int processValidProgramPlayerEnrollmentRows(List<ValidatedProgramPlayerEnrollmentRow> validRows,
			List<RowError> allErrors, Course course) {

		int success = 0;

		for (ValidatedProgramPlayerEnrollmentRow row : validRows) {
			ProgramPlayerEnrollmentDto enrollmentDto = row.getEnrollmentDto();

			try {
				// Create new enrollment
				TraineeCourseEnrollment enrollment = new TraineeCourseEnrollment();
				enrollment.setId(UUID.randomUUID().toString());
				enrollment.setTraineeUserProfile(enrollmentDto.getUserProfile());
				enrollment.setCourse(course);
				enrollment.setAcademy(course.getAcademy());
				enrollment.setStatus(Status.ACTIVE);
				enrollment.setPaymentSchedule(enrollmentDto.getPaymentSchedule());
				enrollment.setAmount(enrollmentDto.getFeeAmount());
				enrollment.setCreatedOn(Timestamp.from(Instant.now()));

				// Set dates
				if (enrollmentDto.getJoiningDate() != null) {
					enrollment.setJoiningDate(enrollmentDto.getJoiningDate());
				} else {
					enrollment.setJoiningDate(LocalDate.now());
				}

				if (enrollmentDto.getNextDueDate() != null) {
					enrollment.setDueDate(enrollmentDto.getNextDueDate());
				}

				enrollment.setUseForFuture(Boolean.FALSE);
				enrollment.setDiscountAmount(0L);

				// Save enrollment
				traineeCourseEnrollmentRepo.save(enrollment);
				success++;

				log.info("Successfully enrolled player {} in program {}", enrollmentDto.getUserProfile().getId(),
						course.getId());

			} catch (Exception e) {
				log.error("Error enrolling player in row {}: {}", row.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(), List.of("Failed to enroll player: " + e.getMessage())));
			}
		}

		return success;
	}

	private static class ValidatedRow {
		private final int rowNumber;
		private final UserProfileDto userDto;

		public ValidatedRow(int rowNumber, UserProfileDto dto) {
			this.rowNumber = rowNumber;
			this.userDto = dto;
		}

		public int getRowNumber() {
			return rowNumber;
		}

		public UserProfileDto getUserDto() {
			return userDto;
		}
	}

	private static class ValidatedProgramRow {
		private final int rowNumber;
		private final CreateCourseDto courseDto;

		public ValidatedProgramRow(int rowNumber, CreateCourseDto dto) {
			this.rowNumber = rowNumber;
			this.courseDto = dto;
		}

		public int getRowNumber() {
			return rowNumber;
		}

		public CreateCourseDto getCreateCourseDto() {
			return courseDto;
		}
	}

	private static class ValidatedProgramPlayerEnrollmentRow {
		private final int rowNumber;
		private final ProgramPlayerEnrollmentDto enrollmentDto;

		public ValidatedProgramPlayerEnrollmentRow(int rowNumber, ProgramPlayerEnrollmentDto enrollmentDto) {
			this.rowNumber = rowNumber;
			this.enrollmentDto = enrollmentDto;
		}

		public int getRowNumber() {
			return rowNumber;
		}

		public ProgramPlayerEnrollmentDto getEnrollmentDto() {
			return enrollmentDto;
		}
	}

	@Override
	public ServiceResponse bulkUploadProgramsPayments(String academyId, String programId, String userId) {
		try {
			// Find all enrolled trainees for the given academy and program
			List<TraineeCourseEnrollment> enrollments = findEnrolledTrainees(academyId, programId);

			if (enrollments.isEmpty()) {
				return ResponseBuilder.error("No enrolled trainees found for the given academy and program",
						HttpStatus.NOT_FOUND);
			}

			List<PaymentDto> createdPayments = new ArrayList<>();
			List<String> failedEnrollments = new ArrayList<>();
			List<String> skippedEnrollments = new ArrayList<>();
			String targetDate = "2025-04-01"; // Specific date you want to create payments for

			for (TraineeCourseEnrollment enrollment : enrollments) {
				try {
					// Check if enrollment exists in attendance view
					List<AttendanceView> attendanceViews = attendanceViewRepository.findByEnrollId(enrollment.getId());
					if (attendanceViews.isEmpty()) {
						skippedEnrollments.add(enrollment.getId() + ": No attendance record found");
						log.info("Skipping enrollment {} - no attendance record found", enrollment.getId());
						continue;
					}

					// Create actual payment for the specific date
					PaymentDto payment = createPaymentForSpecificDate(enrollment, userId, targetDate);
					if (payment != null) {
						createdPayments.add(payment);
					}
				} catch (Exception e) {
					failedEnrollments.add(enrollment.getId() + ": " + e.getMessage());
					// Log the error but continue processing other enrollments
					log.error("Failed to create payment for enrollment {}: {}", enrollment.getId(), e.getMessage());
				}
			}

			// Prepare response with summary
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("totalEnrollments", enrollments.size());
			responseData.put("successfulPayments", createdPayments.size());
			responseData.put("failedPayments", failedEnrollments.size());
			responseData.put("skippedEnrollments", skippedEnrollments.size());
			responseData.put("createdPayments", createdPayments);
			responseData.put("targetDate", targetDate);
			responseData.put("message", "Payments created successfully for " + targetDate);

			if (!failedEnrollments.isEmpty()) {
				responseData.put("failedEnrollments", failedEnrollments);
			}

			if (!skippedEnrollments.isEmpty()) {
				responseData.put("skippedEnrollmentDetails", skippedEnrollments);
			}

			return ResponseBuilder.success(responseData, ApiResponse.DATA_ADDED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Bulk payment creation failed for academy {} and program {}: {}", academyId, programId,
					e.getMessage());
			return ResponseBuilder.error("Payment creation failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Create payment for a specific enrollment and specific date
	 */
	private PaymentDto createPaymentForSpecificDate(TraineeCourseEnrollment enrollment, String userId,
			String targetDate) throws ResourceException {

		// Get payment details for the enrollment
		PaymentDetailsDto paymentDetailsDto = paymentService.getPaymentDetails(enrollment.getId(),
				PaymentCategory.COURSE_FEE);

		// Check if the target date exists in pending payments
		Map<String, Long> pendingPayments = paymentDetailsDto.getPendingPayments();
		if (pendingPayments == null || !pendingPayments.containsKey(targetDate)) {
			log.warn("No pending payment found for date {} in enrollment {}", targetDate, enrollment.getId());
			return null;
		}

		// Get the amount for the specific date
		Long amountForDate = pendingPayments.get(targetDate);

		// Create InitPaymentDto for the specific installment
		InitPaymentDto initPaymentDto = new InitPaymentDto();
		initPaymentDto.setAmount(amountForDate); // Use amount for specific date (1750)
		initPaymentDto.setCurrency(paymentDetailsDto.getCurrency());
		initPaymentDto.setPaymentSchedule(paymentDetailsDto.getPaymentSchedule());
		initPaymentDto.setPaymentInstallmentDate(targetDate); // Set to specific date
		initPaymentDto.setPaymentMode("ONLINE");
		initPaymentDto.setPaymentCategory(PaymentCategory.COURSE_FEE);

		// Create the actual payment
		return paymentService.initPayment(userId, enrollment.getId(), initPaymentDto);
	}

	/**
	 * Alternative method: Create payment preview but for specific date only
	 */
	public ServiceResponse bulkPreviewPaymentsForSpecificDate(String academyId, String programId, String userId,
			String targetDate) {
		try {
			// Find all enrolled trainees for the given academy and program
			List<TraineeCourseEnrollment> enrollments = findEnrolledTrainees(academyId, programId);

			if (enrollments.isEmpty()) {
				return ResponseBuilder.error("No enrolled trainees found for the given academy and program",
						HttpStatus.NOT_FOUND);
			}

			List<PaymentPreviewDto> paymentPreviews = new ArrayList<>();
			List<String> failedEnrollments = new ArrayList<>();
			List<String> skippedEnrollments = new ArrayList<>();

			for (TraineeCourseEnrollment enrollment : enrollments) {
				try {
					// Check if enrollment exists in attendance view
					List<AttendanceView> attendanceViews = attendanceViewRepository.findByEnrollId(enrollment.getId());
					if (attendanceViews.isEmpty()) {
						skippedEnrollments.add(enrollment.getId() + ": No attendance record found");
						log.info("Skipping enrollment {} - no attendance record found", enrollment.getId());
						continue;
					}

					// Create payment preview for specific date only
					PaymentPreviewDto paymentPreview = createPaymentPreviewForSpecificDate(enrollment, userId,
							targetDate);
					if (paymentPreview != null) {
						paymentPreviews.add(paymentPreview);
					}
				} catch (Exception e) {
					failedEnrollments.add(enrollment.getId() + ": " + e.getMessage());
					log.error("Failed to create payment preview for enrollment {}: {}", enrollment.getId(),
							e.getMessage());
				}
			}

			// Prepare response with summary
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("totalEnrollments", enrollments.size());
			responseData.put("successfulPaymentPreviews", paymentPreviews.size());
			responseData.put("failedPayments", failedEnrollments.size());
			responseData.put("skippedEnrollments", skippedEnrollments.size());
			responseData.put("paymentPreviews", paymentPreviews);
			responseData.put("targetDate", targetDate);
			responseData.put("message", "Payment preview generated for " + targetDate
					+ " only. Use bulkUploadProgramsPayments to create actual payments.");

			if (!failedEnrollments.isEmpty()) {
				responseData.put("failedEnrollments", failedEnrollments);
			}

			if (!skippedEnrollments.isEmpty()) {
				responseData.put("skippedEnrollmentDetails", skippedEnrollments);
			}

			return ResponseBuilder.success(responseData, ApiResponse.DATA_ADDED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Bulk payment preview failed for academy {} and program {}: {}", academyId, programId,
					e.getMessage());
			return ResponseBuilder.error("Payment preview failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Create payment preview for a specific date only
	 */
	private PaymentPreviewDto createPaymentPreviewForSpecificDate(TraineeCourseEnrollment enrollment, String userId,
			String targetDate) throws ResourceException {

		// Get payment details for the enrollment
		PaymentDetailsDto paymentDetailsDto = paymentService.getPaymentDetails(enrollment.getId(),
				PaymentCategory.COURSE_FEE);

		// Check if the target date exists in pending payments
		Map<String, Long> pendingPayments = paymentDetailsDto.getPendingPayments();
		if (pendingPayments == null || !pendingPayments.containsKey(targetDate)) {
			log.warn("No pending payment found for date {} in enrollment {}", targetDate, enrollment.getId());
			return null;
		}

		// Get the amount for the specific date
		Long amountForDate = pendingPayments.get(targetDate);

		// Create payment preview with details for specific date
		PaymentPreviewDto paymentPreview = new PaymentPreviewDto();
		paymentPreview.setId(UUID.randomUUID().toString());
		paymentPreview.setEnrollmentId(enrollment.getId());
		paymentPreview.setTraineeName(enrollment.getTraineeUserProfile().getDisplayName());
		paymentPreview.setCourseName(enrollment.getCourse().getTitle());
		paymentPreview.setAmount(amountForDate); // Amount for specific date (1750)
		paymentPreview.setCurrency(paymentDetailsDto.getCurrency());
		paymentPreview.setPaymentSchedule(paymentDetailsDto.getPaymentSchedule());
		paymentPreview.setPaymentInstallmentDate(targetDate); // Specific date
		paymentPreview.setPaymentMode("CASH");
		paymentPreview.setPaymentCategory(PaymentCategory.COURSE_FEE);
		paymentPreview.setPaymentStatus("WOULD_BE_PENDING");

		// Only include the specific date in pending payments
		Map<String, Long> singleDatePayment = new HashMap<>();
		singleDatePayment.put(targetDate, amountForDate);
		paymentPreview.setPendingPayments(singleDatePayment);

		paymentPreview.setCreatedAt(Timestamp.from(Instant.now()));
		paymentPreview.setPaymentInitiatedBy(userId);

		return paymentPreview;
	}

	/**
	 * Find all enrolled trainees for a specific academy and program
	 */
	private List<TraineeCourseEnrollment> findEnrolledTrainees(String academyId, String programId) {
		return traineeCourseEnrollmentRepo.findByCourse_IdAndAcademy_Id(programId, academyId);
	}

	/**
	 * Create payment preview for a specific enrollment (without saving to database)
	 */
	private PaymentPreviewDto createPaymentPreviewForEnrollment(TraineeCourseEnrollment enrollment, String userId)
			throws ResourceException {

		// Get payment details for the enrollment
		PaymentDetailsDto paymentDetailsDto = paymentService.getPaymentDetails(enrollment.getId(),
				PaymentCategory.COURSE_FEE);

		// Create payment preview with all the details that would be saved
		PaymentPreviewDto paymentPreview = new PaymentPreviewDto();
		paymentPreview.setId(UUID.randomUUID().toString()); // Preview ID
		paymentPreview.setEnrollmentId(enrollment.getId());
		paymentPreview.setTraineeName(enrollment.getTraineeUserProfile().getDisplayName());
		paymentPreview.setCourseName(enrollment.getCourse().getTitle());
		paymentPreview.setAmount(paymentDetailsDto.getPendingAmount());
		paymentPreview.setCurrency(paymentDetailsDto.getCurrency());
		paymentPreview.setPaymentSchedule(paymentDetailsDto.getPaymentSchedule());
		paymentPreview.setPaymentInstallmentDate(paymentDetailsDto.getNextDueAt());
		paymentPreview.setPaymentMode("CASH");
		paymentPreview.setPaymentCategory(PaymentCategory.COURSE_FEE);
		paymentPreview.setPaymentStatus("WOULD_BE_PENDING");
		paymentPreview.setPendingPayments(paymentDetailsDto.getPendingPayments());
		paymentPreview.setCreatedAt(Timestamp.from(Instant.now()));
		paymentPreview.setPaymentInitiatedBy(userId);
		return paymentPreview;
	}

	/**
	 * COMMENTED OUT - Uncomment this method to actually create payments
	 */
	/*
	 * private PaymentDto createActualPaymentForEnrollment(TraineeCourseEnrollment
	 * enrollment, String userId) throws ResourceException {
	 * 
	 * // Get payment details for the enrollment PaymentDetailsDto paymentDetailsDto
	 * = paymentService.getPaymentDetails(enrollment.getId(),
	 * PaymentCategory.COURSE_FEE);
	 * 
	 * // Create InitPaymentDto with default values InitPaymentDto initPaymentDto =
	 * new InitPaymentDto();
	 * initPaymentDto.setAmount(paymentDetailsDto.getPendingAmount());
	 * initPaymentDto.setCurrency(paymentDetailsDto.getCurrency());
	 * initPaymentDto.setPaymentSchedule(paymentDetailsDto.getPaymentSchedule());
	 * initPaymentDto.setPaymentInstallmentDate(paymentDetailsDto.getNextDueAt());
	 * initPaymentDto.setPaymentMode("ONLINE");
	 * initPaymentDto.setPaymentCategory(PaymentCategory.COURSE_FEE);
	 * 
	 * // UNCOMMENT THIS LINE TO ACTUALLY SAVE TO DATABASE // return
	 * paymentService.initPayment(userId, enrollment.getId(), initPaymentDto);
	 * 
	 * // For now, return null or throw exception to prevent accidental saves throw
	 * new
	 * RuntimeException("Payment creation is disabled. Uncomment the save logic to enable."
	 * ); }
	 */

	// DTO class for payment preview
	public static class PaymentPreviewDto {
		private String id;
		private String enrollmentId;
		private String traineeName;
		private String traineeEmail;
		private String courseName;
		private Long amount;
		private String paymentInstallmentDate;
		private String paymentMode;
		private PaymentCategory paymentCategory;
		private String paymentStatus;
		private Timestamp createdAt;
		private String paymentInitiatedBy;
		private Currency currency;
		private PaymentSchedule paymentSchedule;
		private Map<String, Long> pendingPayments;

		// Getters and setters
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Map<String, Long> getPendingPayments() {
			return pendingPayments;
		}

		public void setPendingPayments(Map<String, Long> pendingPayments) {
			this.pendingPayments = pendingPayments;
		}

		public String getEnrollmentId() {
			return enrollmentId;
		}

		public void setEnrollmentId(String enrollmentId) {
			this.enrollmentId = enrollmentId;
		}

		public String getTraineeName() {
			return traineeName;
		}

		public void setTraineeName(String traineeName) {
			this.traineeName = traineeName;
		}

		public String getTraineeEmail() {
			return traineeEmail;
		}

		public void setTraineeEmail(String traineeEmail) {
			this.traineeEmail = traineeEmail;
		}

		public String getCourseName() {
			return courseName;
		}

		public void setCourseName(String courseName) {
			this.courseName = courseName;
		}

		public Long getAmount() {
			return amount;
		}

		public void setAmount(Long amount) {
			this.amount = amount;
		}

		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}

		public PaymentSchedule getPaymentSchedule() {
			return paymentSchedule;
		}

		public void setPaymentSchedule(PaymentSchedule paymentSchedule) {
			this.paymentSchedule = paymentSchedule;
		}

		public String getPaymentInstallmentDate() {
			return paymentInstallmentDate;
		}

		public void setPaymentInstallmentDate(String paymentInstallmentDate) {
			this.paymentInstallmentDate = paymentInstallmentDate;
		}

		public String getPaymentMode() {
			return paymentMode;
		}

		public void setPaymentMode(String paymentMode) {
			this.paymentMode = paymentMode;
		}

		public PaymentCategory getPaymentCategory() {
			return paymentCategory;
		}

		public void setPaymentCategory(PaymentCategory paymentCategory) {
			this.paymentCategory = paymentCategory;
		}

		public String getPaymentStatus() {
			return paymentStatus;
		}

		public void setPaymentStatus(String paymentStatus) {
			this.paymentStatus = paymentStatus;
		}

		public Timestamp getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Timestamp createdAt) {
			this.createdAt = createdAt;
		}

		public String getPaymentInitiatedBy() {
			return paymentInitiatedBy;
		}

		public void setPaymentInitiatedBy(String paymentInitiatedBy) {
			this.paymentInitiatedBy = paymentInitiatedBy;
		}
	}

}
