package com.playmotech.api.core.services.impl;

import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.createErrorResponse;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.handleEmptyParseResult;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.validateInputParameters;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.BulkType;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CoursePaymentOptionsMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CoursePaymentOptionDto;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.BulkUploadHistoryRepository;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.excel.ParsedRow;
import com.playmotech.api.core.response.excel.RowError;
import com.playmotech.api.core.services.BulkUploaderService;
import com.playmotech.api.core.services.impl.BulkUploadAsyncServiceImpl.ValidatedProgramPlayerEnrollmentRow;
import com.playmotech.api.core.services.impl.BulkUploadAsyncServiceImpl.ValidatedProgramRow;
import com.playmotech.api.core.services.impl.BulkUploadAsyncServiceImpl.ValidatedRow;
import com.playmotech.api.core.utils.excel.BulkUploaderHelper;
import com.playmotech.api.core.utils.excel.ExcelUserUploadUtil;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploaderServiceImpl implements BulkUploaderService {

	private final AcademyRepo academyRepo;
	private final BulkUploadHistoryRepository bulkUploadHistoryRepository;
	private final CourseRepo courseRepo;

	private final BulkUploaderHelper bulkUploaderHelper;

	private final BulkUploadAsyncServiceImpl asyncServiceImpl;

	@Value("${bulk.upload.s3.bucket}")
	private String bulkUploadBucket;

	@Value("${bulk.upload.base.url}")
	private String bulkUploadBaseUrl;

	@Override
	public ServiceResponse bulkUploadCoachesFromExcel(MultipartFile file, String academyId, boolean saveData,
			boolean edit) {
		log.info("Starting bulk upload for coaches - File: {}, Academy ID: {}, Save Data: {}, Edit Mode: {}",
				file != null ? file.getOriginalFilename() : "null", academyId, saveData, edit);
		return bulkUploadUsers(file, null, academyId, UserType.COACH, saveData, edit);
	}

	@Override
	public ServiceResponse bulkUploadPlayersFromExcel(MultipartFile file, String academyId, boolean saveData,
			boolean edit) {
		log.info("Starting bulk upload for players - File: {}, Academy ID: {}, Save Data: {}, Edit Mode: {}",
				file != null ? file.getOriginalFilename() : "null", academyId, saveData, edit);
		return bulkUploadUsers(file, null, academyId, UserType.PLAYER, saveData, edit);
	}

	@Override
	public ServiceResponse bulkUploadCoachesFromExcel(List<ParsedRow> rows, String academyId, boolean save,
			String userId, boolean edit) {
		log.info("Starting async bulk upload for coaches - {} rows, Academy ID: {}, Save: {}, User ID: {}, Edit: {}",
				rows != null ? rows.size() : 0, academyId, save, userId, edit);
		return initiateAsyncBulkUpload(null, rows, academyId, UserType.COACH, BulkUploadHistory.BulkType.COACHES, save,
				userId, edit);
	}

	@Override
	public ServiceResponse bulkUploadPlayersFromExcel(List<ParsedRow> rows, String academyId, boolean save,
			String userId, boolean edit) {
		log.info("Starting async bulk upload for players - {} rows, Academy ID: {}, Save: {}, User ID: {}, Edit: {}",
				rows != null ? rows.size() : 0, academyId, save, userId, edit);
		return initiateAsyncBulkUpload(null, rows, academyId, UserType.PLAYER, BulkUploadHistory.BulkType.PLAYERS, save,
				userId, edit);
	}

	@Override
	public ServiceResponse bulkUploadProgramsFromExcel(MultipartFile file, String academyId, String userId,
			boolean save) {
		log.info("Starting bulk upload for programs - File: {}, Academy ID: {}, User ID: {}, Save Data: {}",
				file != null ? file.getOriginalFilename() : "null", academyId, userId, save);

		try {
			// Input validation
			if (file != null) {
				log.info("INPUT_VALIDATION: Validating file-based {} upload", BulkType.PROGRAMS);
				List<String> inputErrors = validateInputParameters(file, academyId);
				if (!inputErrors.isEmpty()) {
					log.error("FILE_VALIDATION_ERROR: Program upload validation failed - {}",
							String.join("; ", inputErrors));
					return createErrorResponse(String.join("; ", inputErrors), HttpStatus.BAD_REQUEST);
				}
				log.info("INPUT_VALIDATION: File validation successful for {} upload", BulkType.PROGRAMS);
			}

			List<ParsedRow> parsedRows = new ArrayList<>();
			// Parse Excel if file is provided
			if (file != null) {
				try {
					log.info("HEADER_VALIDATION: Starting Excel header validation for {} upload", BulkType.PROGRAMS);
					ServiceResponse headerValidationResult = validateExcelHeaders(file, BulkType.PROGRAMS, false);
					if (headerValidationResult != null) {
						log.error("HEADER_VALIDATION_ERROR: Excel headers validation failed for {} upload",
								BulkType.PROGRAMS);
						return headerValidationResult;
					}
					log.info("HEADER_VALIDATION: Excel headers validated successfully for {} upload",
							BulkType.PROGRAMS);

					log.info("EXCEL_PARSING: Starting to parse {} Excel file", BulkType.PROGRAMS);
					parsedRows = ExcelUserUploadUtil.parseProgramExcelWithErrors(file);

					if (parsedRows == null || parsedRows.isEmpty()) {
						log.error("EXCEL_PARSING_ERROR: No data parsed from {} Excel file", BulkType.PROGRAMS);
						return handleEmptyParseResult(parsedRows);
					}
					log.info("EXCEL_PARSING: Successfully parsed {} rows from {} Excel file", parsedRows.size(),
							BulkType.PROGRAMS);

				} catch (IOException e) {
					log.error("FILE_READ_ERROR: Failed to read {} Excel file", BulkType.PROGRAMS, e);
					return createErrorResponse("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				} catch (Exception e) {
					log.error("EXCEL_PARSING_ERROR: Failed to parse {} Excel file", BulkType.PROGRAMS, e);
					return createErrorResponse("Failed to parse Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				}
			} else {
				log.warn("NO_FILE_PROVIDED: No file provided for {} upload", BulkType.PROGRAMS);
				return createErrorResponse("No file provided for upload", HttpStatus.BAD_REQUEST);
			}

			// Initialize collections
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedProgramRow> validRows = new ArrayList<>();
			Set<String> courseTitlesInUpload = new HashSet<>();

			log.info("DATA_PROCESSING_INIT: Initialized collections for {} upload processing", BulkType.PROGRAMS);

			// Process parsing errors
			log.info("ERROR_PROCESSING: Processing parsing errors for {} upload", BulkType.PROGRAMS);
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);
			log.info("ERROR_PROCESSING: Found {} parsing errors for {} upload", allErrors.size(), BulkType.PROGRAMS);

			// Validate rows
			log.info("ROW_VALIDATION: Starting row validation for {} upload - {} rows to validate", BulkType.PROGRAMS,
					parsedRows.size());
			asyncServiceImpl.validateAndPrepareProgramRows(parsedRows, validRows, allErrors, courseTitlesInUpload,
					academyId);

			log.info("ROW_VALIDATION: Validation completed for {} upload - Valid: {}, Errors: {}", BulkType.PROGRAMS,
					validRows.size(), allErrors.size());

			// Process valid rows
			log.info("ROW_PROCESSING: Starting to process {} valid {} rows - Save Mode: {}", validRows.size(),
					BulkType.PROGRAMS, save);
			int success = asyncServiceImpl.processValidProgramRows(validRows, allErrors, academyId, save, userId);
			log.info("ROW_PROCESSING: Successfully processed {} {} rows", success, BulkType.PROGRAMS);

			ServiceResponse response = asyncServiceImpl.generateEnhancedResponse(allErrors, success, BulkType.PROGRAMS,
					parsedRows, save, null, academyId);

			log.info("BULK_UPLOAD_COMPLETE: {} upload completed - Success: {}, Errors: {}, Total: {}",
					BulkType.PROGRAMS, success, allErrors.size(), parsedRows.size());

			return response;

		} catch (Exception e) {
			log.error("BULK_UPLOAD_ERROR: Unhandled exception during {} bulk upload", BulkType.PROGRAMS, e);
			return createErrorResponse("Unexpected error occurred: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse bulkUploadProgramsFromExcel(List<ParsedRow> rows, String academyId, String userId,
			boolean save) {
		return initiateAsyncProgramUpload(null, rows, academyId, userId, save);
	}

	private ServiceResponse initiateAsyncProgramUpload(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			String userId, boolean saveData) {

		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new EntityNotFoundException("Academy not found"));

		UserProfile currentUser = new UserProfile();
		currentUser.setId(userId);

		BulkUploadHistory historyEntry = createOptimizedHistoryEntry(BulkType.PROGRAMS, academy, currentUser, null,
				file != null ? file.getOriginalFilename() : "data", parsedRows.size(), false);

		log.info("HISTORY_CREATE: Creating bulk upload history for {} upload", BulkType.PROGRAMS);
		BulkUploadHistory savedHistory = bulkUploadHistoryRepository.save(historyEntry);
		Long historyId = savedHistory.getId();

		log.info("HISTORY_CREATE: History entry created with ID: {} for {} upload", historyId, BulkType.PROGRAMS);

		asyncServiceImpl.processProgramUpload(null, parsedRows, academyId, saveData, historyId, academy.getManagerUserId());

		// OPTIMIZED: Create response
		Map<String, Object> response = createOptimizedResponse(historyId, null, "Bulk upload initiated successfully");

		log.info("ASYNC_BULK_INIT_COMPLETE: Async {} upload initiated successfully - History ID: {}", BulkType.PROGRAMS,
				historyId);
		return ResponseBuilder.success(response, ApiResponse.UPLOAD_INITIATED, HttpStatus.ACCEPTED);

	}

	@Override
	public ServiceResponse bulkUploadProgramsMapPlayersFromExcel(MultipartFile file, String academyId, String programId,
			String userId, boolean save) {
		log.info(
				"Starting synchronous program player enrollment - File: {}, Academy ID: {}, Program ID: {}, User ID: {}, Save: {}",
				file != null ? file.getOriginalFilename() : "null", academyId, programId, userId, save);

		// Validate input parameters
		if (file == null || file.isEmpty()) {
			log.error("VALIDATION_ERROR: File is null or empty for program player enrollment");
			return ResponseBuilder.error("File is required and cannot be empty", HttpStatus.BAD_REQUEST);
		}

		if (academyId == null || academyId.trim().isEmpty()) {
			log.error("VALIDATION_ERROR: Academy ID is null or empty for program player enrollment");
			return ResponseBuilder.error("Academy ID is required", HttpStatus.BAD_REQUEST);
		}

		if (programId == null || programId.trim().isEmpty()) {
			log.error("VALIDATION_ERROR: Program ID is null or empty for program player enrollment");
			return ResponseBuilder.error("Program ID is required", HttpStatus.BAD_REQUEST);
		}

		log.info("INPUT_VALIDATION: All required parameters validated successfully");

		// Verify program exists and belongs to academy
		log.info("PROGRAM_VALIDATION: Checking if program {} exists in academy {}", programId, academyId);
		Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(academyId, programId);
		if (courseOpt.isEmpty() || Boolean.TRUE.equals(courseOpt.get().getInactive())) {
			log.error("PROGRAM_VALIDATION_ERROR: Program {} not found or inactive in academy {}", programId, academyId);
			return ResponseBuilder.error("Program not found or inactive", HttpStatus.BAD_REQUEST);
		}

		Course course = courseOpt.get();
		log.info("PROGRAM_VALIDATION: Program {} validated successfully - Name: {}", programId, course.getTitle());

		List<ParsedRow> parsedRows;
		List<RowError> allErrors = new ArrayList<>();
		List<ValidatedProgramPlayerEnrollmentRow> validRows = new ArrayList<>();

		List<CoursePaymentOptionDto> paymentOptionDtos = mapToPaymentOptionDtos(course.getPaymentOptions());
		log.info("PAYMENT_OPTIONS: Mapped {} payment options for program {}", paymentOptionDtos.size(), programId);

		try {
			// Validate Excel file headers before processing
			log.info("HEADER_VALIDATION: Starting Excel header validation for program player enrollment");
			ServiceResponse headerValidationResult = validateExcelHeaders(file, BulkType.PLAYER_ENROLLEMENTS, false);
			if (headerValidationResult != null) {
				log.error("HEADER_VALIDATION_ERROR: Excel headers validation failed");
				return headerValidationResult;
			}
			log.info("HEADER_VALIDATION: Excel headers validated successfully");

			// Parse Excel file
			log.info("EXCEL_PARSING: Starting to parse program player enrollment Excel file");
			parsedRows = ExcelUserUploadUtil.parseProgramPlayerEnrollmentExcelWithErrors(file);

			if (parsedRows == null || parsedRows.isEmpty()) {
				log.error("EXCEL_PARSING_ERROR: Excel file contains no data to process");
				return ResponseBuilder.error("Excel file contains no data to process", HttpStatus.BAD_REQUEST);
			}

			log.info("EXCEL_PARSING: Successfully parsed {} rows from Excel file", parsedRows.size());

			// Process parsing errors using helper
			log.info("ERROR_PROCESSING: Starting to process parsing errors from {} rows", parsedRows.size());
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);
			log.info("ERROR_PROCESSING: Found {} parsing errors", allErrors.size());

			// Validate and prepare enrollment rows
			log.info("ROW_VALIDATION: Starting validation and preparation of enrollment rows");
			asyncServiceImpl.validateAndPrepareProgramPlayerEnrollmentRows(parsedRows, validRows, allErrors, academyId,
					programId, course);

			log.info("ROW_VALIDATION: Validation completed - Valid rows: {}, Error rows: {}", validRows.size(),
					allErrors.size());

			// Process valid enrollment rows
			log.info("ROW_PROCESSING: Starting to process {} valid enrollment rows", validRows.size());
			int success = asyncServiceImpl.processValidProgramPlayerEnrollmentRows(validRows, allErrors, course, save);
			log.info("ROW_PROCESSING: Successfully processed {} enrollment rows", success);

			ServiceResponse response = asyncServiceImpl.generateEnhancedResponse(allErrors, success,
					BulkType.PLAYER_ENROLLEMENTS, parsedRows, save, paymentOptionDtos, academyId);

			log.info("BULK_UPLOAD_COMPLETE: Program player enrollment completed - Success: {}, Errors: {}, Total: {}",
					success, allErrors.size(), parsedRows.size());

			return response;

		} catch (IOException e) {
			log.error("FILE_READ_ERROR: Failed to read Excel file for program player enrollment", e);
			return ResponseBuilder.error("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (StackOverflowError e) {
			log.error("STACK_OVERFLOW_ERROR: Circular reference detected during program player enrollment processing",
					e);
			return ResponseBuilder.error("Data processing error - circular reference detected",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("UNEXPECTED_ERROR: Unhandled exception during program player enrollment bulk upload", e);
			return ResponseBuilder.error("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse bulkUploadProgramsMapPlayersFromExcel(List<ParsedRow> rows, String academyId,
			String programId, String userId, boolean save) {
		log.info(
				"Starting async program player enrollment from parsed rows - {} rows, Academy ID: {}, Program ID: {}, User ID: {}, Save: {}",
				rows != null ? rows.size() : 0, academyId, programId, userId, save);
		return initiateAsyncProgramPlayerMapping(null, rows, academyId, programId, userId, save);
	}

	public static List<CoursePaymentOptionDto> mapToPaymentOptionDtos(
			List<CoursePaymentOptionsMapping> paymentOptions) {
		log.debug("Mapping {} payment options to DTOs", paymentOptions != null ? paymentOptions.size() : 0);
		return paymentOptions.stream()
				.map(option -> CoursePaymentOptionDto.builder().id(option.getId())
						.paymentSchedule(option.getPaymentSchedule()).paymentAmount(option.getPaymentAmount())
						.currency(option.getCurrency()).build())
				.collect(Collectors.toList());
	}

	/**
	 * Initiates async program player mapping and returns immediate response with
	 * tracking ID - OPTIMIZED VERSION
	 */
	private ServiceResponse initiateAsyncProgramPlayerMapping(MultipartFile file, List<ParsedRow> parsedRows,
			String academyId, String programId, String userId, boolean saveData) {
		log.info(
				"ASYNC_INIT: Initiating async program player mapping - File: {}, Parsed Rows: {}, Academy: {}, Program: {}, User: {}, Save: {}",
				file != null ? file.getOriginalFilename() : "null", parsedRows != null ? parsedRows.size() : 0,
				academyId, programId, userId, saveData);

		try {
			// Basic validation
			if (file != null) {
				log.info("FILE_VALIDATION: Validating file-based parameters");
				List<String> inputErrors = validateProgramPlayerMappingParameters(file, academyId, programId);
				if (!inputErrors.isEmpty()) {
					log.error("FILE_VALIDATION_ERROR: Validation failed - {}", String.join("; ", inputErrors));
					return BulkUploaderHelper.createErrorResponse(String.join("; ", inputErrors),
							HttpStatus.BAD_REQUEST);
				}
				log.info("FILE_VALIDATION: File parameters validated successfully");
			} else if (parsedRows == null || parsedRows.isEmpty()) {
				log.error("DATA_VALIDATION_ERROR: No data provided for processing");
				return BulkUploaderHelper.createErrorResponse("No data provided for processing",
						HttpStatus.BAD_REQUEST);
			}

			// Verify program exists and belongs to academy
			log.info("PROGRAM_LOOKUP: Searching for program {} in academy {}", programId, academyId);
			Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(academyId, programId);
			if (courseOpt.isEmpty() || Boolean.TRUE.equals(courseOpt.get().getInactive())) {
				log.error("PROGRAM_LOOKUP_ERROR: Program {} not found or inactive in academy {}", programId, academyId);
				return BulkUploaderHelper.createErrorResponse("Program not found or inactive", HttpStatus.BAD_REQUEST);
			}
			log.info("PROGRAM_LOOKUP: Program {} found and active", programId);

			// OPTIMIZED: Batch fetch academy and create entities efficiently
			log.info("ENTITY_SETUP: Setting up entities for bulk upload history");
			Academy academy = academyRepo.findById(academyId)
					.orElseThrow(() -> new EntityNotFoundException("Academy not found"));

			UserProfile currentUser = new UserProfile();
			currentUser.setId(userId);

			Course course = new Course();
			course.setId(programId);

			// OPTIMIZED: Create history entry with minimal database calls
			BulkUploadHistory historyEntry = createOptimizedHistoryEntry(BulkUploadHistory.BulkType.PLAYER_ENROLLEMENTS,
					academy, currentUser, course, file != null ? file.getOriginalFilename() : "Parsed Data",
					parsedRows != null ? parsedRows.size() : null, false);

			log.info("HISTORY_CREATE: Creating bulk upload history entry");
			BulkUploadHistory savedHistory = bulkUploadHistoryRepository.save(historyEntry);
			Long historyId = savedHistory.getId();
			log.info("HISTORY_CREATE: History entry created with ID: {}", historyId);

			// Start async processing
			log.info("ASYNC_START: Starting async processing for program player mapping with history ID: {}",
					historyId);
			asyncServiceImpl.processProgramPlayerMappingAsync(file, parsedRows, academyId, programId, saveData,
					historyId);

			// OPTIMIZED: Create response with structured data
			Map<String, Object> response = createOptimizedResponse(historyId, programId,
					"Program player mapping initiated successfully");

			log.info("ASYNC_INIT_COMPLETE: Async program player mapping initiated successfully - History ID: {}",
					historyId);
			return ResponseBuilder.success(response, ApiResponse.UPLOAD_INITIATED, HttpStatus.ACCEPTED);

		} catch (Exception e) {
			log.error("ASYNC_INIT_ERROR: Error initiating async program player mapping", e);
			return BulkUploaderHelper.createErrorResponse(
					"Failed to initiate program player mapping: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * OPTIMIZED: Create history entry with minimal object creation
	 */
	private BulkUploadHistory createOptimizedHistoryEntry(BulkUploadHistory.BulkType bulkType, Academy academy,
			UserProfile user, Course course, String fileName, Integer totalRecords, boolean edit) {

		BulkUploadHistory historyEntry = new BulkUploadHistory();
		historyEntry.setType(bulkType);
		historyEntry
				.setOperationType(edit ? BulkUploadHistory.OperationType.EDIT : BulkUploadHistory.OperationType.ADD);
		historyEntry.setAcademy(academy);
		historyEntry.setStatus(BulkUploadHistory.UploadStatus.IN_PROGRESS);
		historyEntry.setFileName(fileName);
		historyEntry.setTotalRecords(totalRecords);
		historyEntry.setStartedAt(LocalDateTime.now());
		historyEntry.setUploadedBy(user);
		historyEntry.setDeleted(false);

		if (course != null) {
			historyEntry.setProgram(course);
		}

		return historyEntry;
	}

	/**
	 * OPTIMIZED: Create standardized response object
	 */
	private Map<String, Object> createOptimizedResponse(Long historyId, String programId, String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", message);
		response.put("historyId", historyId);
		response.put("status", "IN_PROGRESS");
		response.put("timestamp", Instant.now().toString());

		if (programId != null) {
			response.put("programId", programId);
		}

		return response;
	}

	/**
	 * Validation method for program player mapping parameters
	 */
	private List<String> validateProgramPlayerMappingParameters(MultipartFile file, String academyId,
			String programId) {
		log.debug("PARAM_VALIDATION: Validating program player mapping parameters");
		List<String> errors = BulkUploaderHelper.validateInputParameters(file, academyId);

		if (programId == null || programId.trim().isEmpty()) {
			errors.add("Program ID is required");
			log.warn("PARAM_VALIDATION_WARNING: Program ID is missing");
		}

		log.debug("PARAM_VALIDATION: Found {} validation errors", errors.size());
		return errors;
	}

	/**
	 * OPTIMIZED: Initiates async bulk upload and returns immediate response with
	 * tracking ID
	 */
	private ServiceResponse initiateAsyncBulkUpload(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			UserType userType, BulkUploadHistory.BulkType bulkType, boolean saveData, String userId, boolean edit) {

		log.info(
				"ASYNC_BULK_INIT: Initiating async bulk upload - Type: {}, File: {}, Rows: {}, Academy: {}, Save: {}, Edit: {}",
				userType, file != null ? file.getOriginalFilename() : "null",
				parsedRows != null ? parsedRows.size() : 0, academyId, saveData, edit);

		try {
			// Basic validation
			if (file != null) {
				log.info("FILE_VALIDATION: Validating file parameters for {} upload", userType);
				List<String> inputErrors = validateInputParameters(file, academyId);
				if (!inputErrors.isEmpty()) {
					log.error("FILE_VALIDATION_ERROR: {} upload validation failed - {}", userType,
							String.join("; ", inputErrors));
					return createErrorResponse(String.join("; ", inputErrors), HttpStatus.BAD_REQUEST);
				}
				log.info("FILE_VALIDATION: File parameters validated for {} upload", userType);
			} else if (parsedRows == null || parsedRows.isEmpty()) {
				log.error("DATA_VALIDATION_ERROR: No data provided for {} upload", userType);
				return createErrorResponse("No data provided for processing", HttpStatus.BAD_REQUEST);
			}

			// OPTIMIZED: Batch entity setup
			log.info("ENTITY_SETUP: Setting up entities for {} bulk upload", userType);
			Academy academy = academyRepo.findById(academyId)
					.orElseThrow(() -> new EntityNotFoundException("Academy not found"));

			UserProfile currentUser = new UserProfile();
			currentUser.setId(userId);

			// OPTIMIZED: Create history entry
			BulkUploadHistory historyEntry = createOptimizedHistoryEntry(bulkType, academy, currentUser, null,
					file != null ? file.getOriginalFilename() : "Parsed Data",
					parsedRows != null ? parsedRows.size() : null, edit);

			log.info("HISTORY_CREATE: Creating bulk upload history for {} upload", userType);
			BulkUploadHistory savedHistory = bulkUploadHistoryRepository.save(historyEntry);
			Long historyId = savedHistory.getId();
			log.info("HISTORY_CREATE: History entry created with ID: {} for {} upload", historyId, userType);

			// Start async processing
			log.info("ASYNC_START: Starting async processing for {} upload with history ID: {}", userType, historyId);
			asyncServiceImpl.processBulkUploadAsync(file, parsedRows, academyId, userType, saveData, historyId, edit);

			// OPTIMIZED: Create response
			Map<String, Object> response = createOptimizedResponse(historyId, null,
					"Bulk upload initiated successfully");

			log.info("ASYNC_BULK_INIT_COMPLETE: Async {} upload initiated successfully - History ID: {}", userType,
					historyId);
			return ResponseBuilder.success(response, ApiResponse.UPLOAD_INITIATED, HttpStatus.ACCEPTED);

		} catch (Exception e) {
			log.error("ASYNC_BULK_INIT_ERROR: Error initiating async {} upload", userType, e);
			return createErrorResponse("Failed to initiate bulk upload: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * OPTIMIZED: Generic bulk upload method with enhanced logging and row tracking
	 */
	private ServiceResponse bulkUploadUsers(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			UserType userType, boolean saveData, boolean edit) {
		String inputType = (file != null) ? "file" : "parsed rows";
		log.info("BULK_UPLOAD_START: Starting {} upload from {} - Academy: {}, Save: {}, Edit: {}", userType, inputType,
				academyId, saveData, edit);

		try {
			// Input validation
			if (file != null) {
				log.info("INPUT_VALIDATION: Validating file-based {} upload", userType);
				List<String> inputErrors = validateInputParameters(file, academyId);
				if (!inputErrors.isEmpty()) {
					log.error("INPUT_VALIDATION_ERROR: {} upload validation failed - {}", userType,
							String.join("; ", inputErrors));
					return createErrorResponse(String.join("; ", inputErrors), HttpStatus.BAD_REQUEST);
				}
				log.info("INPUT_VALIDATION: File validation successful for {} upload", userType);
			} else {
				log.info("INPUT_VALIDATION: Validating parsed rows for {} upload", userType);
				if (parsedRows == null || parsedRows.isEmpty()) {
					log.error("INPUT_VALIDATION_ERROR: No data provided for {} upload", userType);
					return createErrorResponse("No data provided for processing", HttpStatus.BAD_REQUEST);
				}
				if (academyId == null || academyId.trim().isEmpty()) {
					log.error("INPUT_VALIDATION_ERROR: Academy ID required for {} upload", userType);
					return createErrorResponse("Academy ID is required", HttpStatus.BAD_REQUEST);
				}
				log.info("INPUT_VALIDATION: Parsed rows validation successful for {} upload - {} rows", userType,
						parsedRows.size());
			}

			// Parse Excel if file is provided, otherwise use provided parsed rows
			if (file != null) {
				try {
					log.info("HEADER_VALIDATION: Starting Excel header validation for {} upload", userType);
					ServiceResponse headerValidationResult = validateExcelHeaders(file,
							userType.equals(UserType.COACH) ? BulkType.COACHES : BulkType.PLAYERS, edit);
					if (headerValidationResult != null) {
						log.error("HEADER_VALIDATION_ERROR: Excel headers validation failed for {} upload", userType);
						return headerValidationResult;
					}
					log.info("HEADER_VALIDATION: Excel headers validated successfully for {} upload", userType);

					log.info("EXCEL_PARSING: Starting to parse {} Excel file", userType);
					parsedRows = asyncServiceImpl.parseExcelByUserType(file, userType);
					if (parsedRows == null || parsedRows.isEmpty()) {
						log.error("EXCEL_PARSING_ERROR: No data parsed from {} Excel file", userType);
						return handleEmptyParseResult(parsedRows);
					}
					log.info("EXCEL_PARSING: Successfully parsed {} rows from {} Excel file", parsedRows.size(),
							userType);

				} catch (IOException e) {
					log.error("FILE_READ_ERROR: Failed to read {} Excel file", userType, e);
					return createErrorResponse("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				} catch (Exception e) {
					log.error("EXCEL_PARSING_ERROR: Failed to parse {} Excel file", userType, e);
					return createErrorResponse("Failed to parse Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				}
			} else {
				log.info("PARSED_ROWS_RECEIVED: Using provided parsed rows for {} upload - {} rows", userType,
						parsedRows.size());
			}

			// Initialize collections
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedRow> validRows = new ArrayList<>();
			Map<String, List<Integer>> phoneNumberToRows = new HashMap<>();
			Map<String, List<Integer>> emailToRows = new HashMap<>();

			log.info("DATA_PROCESSING_INIT: Initialized collections for {} upload processing", userType);

			// Process parsing errors
			log.info("ERROR_PROCESSING: Processing parsing errors for {} upload", userType);
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);
			log.info("ERROR_PROCESSING: Found {} parsing errors for {} upload", allErrors.size(), userType);

			// Validate rows based on user type
			log.info("ROW_VALIDATION: Starting row validation for {} upload - {} rows to validate", userType,
					parsedRows.size());
			asyncServiceImpl.validateAndPrepareRows(parsedRows, validRows, allErrors, phoneNumberToRows, emailToRows,
					academyId, userType, edit);
			log.info("ROW_VALIDATION: Validation completed for {} upload - Valid: {}, Errors: {}", userType,
					validRows.size(), allErrors.size());

			// Log details for each valid row
			for (int i = 0; i < validRows.size(); i++) {
				ValidatedRow validRow = validRows.get(i);
				log.info("VALID_ROW_{}: Processing {} at row {} - ID: {}", i + 1, userType, validRow.getRowNumber(),
						validRow.getRowNumber());
			}

			// Process valid rows based on user type and save flag
			log.info("ROW_PROCESSING: Starting to process {} valid {} rows - Save Mode: {}", validRows.size(), userType,
					saveData);
			int success = asyncServiceImpl.processValidRows(validRows, allErrors, academyId, userType, saveData, edit);
			log.info("ROW_PROCESSING: Successfully processed {} {} rows", success, userType);

			ServiceResponse response = asyncServiceImpl.generateEnhancedResponse(allErrors, success,
					userType.equals(UserType.COACH) ? BulkType.COACHES : BulkType.PLAYERS, parsedRows, saveData, null, academyId);

			log.info("BULK_UPLOAD_COMPLETE: {} upload completed - Success: {}, Errors: {}, Total: {}", userType,
					success, allErrors.size(), parsedRows.size());

			return response;

		} catch (Exception e) {
			log.error("BULK_UPLOAD_ERROR: Unhandled exception during {} bulk upload from {}", userType, inputType, e);
			return createErrorResponse("Unexpected error occurred: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * OPTIMIZED: Validates Excel file headers against expected template headers
	 */
	private ServiceResponse validateExcelHeaders(MultipartFile file, BulkType bulkType, boolean edit) {
		log.info("HEADER_VALIDATION_START: Starting header validation for {} template, Edit mode: {}", bulkType, edit);

		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);

			// Check if sheet has any rows
			if (sheet.getPhysicalNumberOfRows() == 0) {
				log.error("HEADER_VALIDATION_ERROR: Excel file is empty - no rows found");
				return createErrorResponse("Excel file is empty or has no data rows", HttpStatus.BAD_REQUEST);
			}
			log.info("HEADER_VALIDATION: Excel file contains {} rows", sheet.getPhysicalNumberOfRows());

			// Get the expected headers based on bulk type
			String[] expectedHeaders = getHeadersForBulkType(bulkType, edit);
			if (expectedHeaders == null || expectedHeaders.length == 0) {
				log.error("HEADER_VALIDATION_ERROR: No header template configured for bulk type {}", bulkType);
				return createErrorResponse("No header template found for selected upload type", HttpStatus.BAD_REQUEST);
			}
			log.info("HEADER_VALIDATION: Expected {} headers for {} template", expectedHeaders.length, bulkType);

			// Validate header row
			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				log.error("HEADER_VALIDATION_ERROR: Excel file missing header row");
				return createErrorResponse("Excel file missing header row", HttpStatus.BAD_REQUEST);
			}

			// Check each expected header
			log.info("HEADER_VALIDATION: Validating {} headers against template", expectedHeaders.length);
			for (int i = 0; i < expectedHeaders.length; i++) {
				Cell cell = headerRow.getCell(i);
				String actualHeader = cell != null ? cell.getStringCellValue().trim() : "";
				if (!expectedHeaders[i].equals(actualHeader)) {
					log.error("HEADER_VALIDATION_ERROR: Header mismatch at column {} - Expected: '{}', Found: '{}'",
							i + 1, expectedHeaders[i], actualHeader);
					return createErrorResponse("Invalid Excel template. Please use the correct format.",
							HttpStatus.BAD_REQUEST);
				}
				log.debug("HEADER_VALIDATION: Column {} validated - '{}'", i + 1, expectedHeaders[i]);
			}

			// Check for extra columns
			int actualColumnCount = headerRow.getPhysicalNumberOfCells();
			if (actualColumnCount > expectedHeaders.length) {
				log.error("HEADER_VALIDATION_ERROR: Template has extra columns - Expected: {}, Found: {}",
						expectedHeaders.length, actualColumnCount);
				return createErrorResponse("Invalid Excel template. Please use the correct format.",
						HttpStatus.BAD_REQUEST);
			}

			log.info("HEADER_VALIDATION_SUCCESS: All {} headers validated successfully for {} template",
					expectedHeaders.length, bulkType);
			return null;

		} catch (IOException e) {
			log.error("HEADER_VALIDATION_ERROR: Error reading Excel file during header validation", e);
			return createErrorResponse("Failed to validate Excel headers", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("HEADER_VALIDATION_ERROR: Unexpected error during header validation", e);
			return createErrorResponse("Unexpected error during Excel validation", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * OPTIMIZED: Get headers for bulk type with enhanced logging
	 */
	private String[] getHeadersForBulkType(BulkType bulkType, boolean edit) {
		log.debug("HEADER_TEMPLATE: Getting headers for bulk type {} with edit mode {}", bulkType, edit);

		String[] headers = null;
		switch (bulkType) {
		case COACHES:
			headers = edit ? ExcelUserUploadUtil.COACH_TEMPLATE_HEADERS_WITH_ID
					: ExcelUserUploadUtil.COACH_TEMPLATE_HEADERS;
			log.debug("HEADER_TEMPLATE: Selected {} headers for COACHES template",
					headers != null ? headers.length : 0);
			break;
		case PLAYERS:
			headers = edit ? ExcelUserUploadUtil.PLAYER_HEADERS_WITH_ID : ExcelUserUploadUtil.PLAYER_HEADERS;
			log.debug("HEADER_TEMPLATE: Selected {} headers for PLAYERS template",
					headers != null ? headers.length : 0);
			break;
		case PLAYER_ENROLLEMENTS:
			headers = ExcelUserUploadUtil.PROGRAM_PLAYER_ENROLLMENT_HEADERS;
			log.debug("HEADER_TEMPLATE: Selected {} headers for PLAYER_ENROLLEMENTS template",
					headers != null ? headers.length : 0);
			break;
		case PROGRAMS:
			headers = ExcelUserUploadUtil.PROGRAM_HEADERS;
			log.debug("HEADER_TEMPLATE: Selected {} headers for PROGRAM_HEADERS template",
					headers != null ? headers.length : 0);
			break;
		default:
			log.warn("HEADER_TEMPLATE_WARNING: No template found for bulk type {}", bulkType);
			break;
		}

		return headers;
	}

}