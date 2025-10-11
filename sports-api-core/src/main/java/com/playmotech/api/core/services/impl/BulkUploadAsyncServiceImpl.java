package com.playmotech.api.core.services.impl;

import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.createErrorResponse;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.createFieldError;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.createGeneralError;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.handleEmptyParseResult;
import static com.playmotech.api.core.utils.excel.BulkUploaderHelper.validateInputParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.internal.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.BulkType;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CoursePaymentOptionsMapping;
import com.playmotech.api.core.dao_postgres.PaymentLedger;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.Schedule;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CoursePaymentOptionDto;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.InstallmentInfo;
import com.playmotech.api.core.dto.ProgramPlayerEnrollmentDto;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.helper.PaymentLedgerHelper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.BulkUploadHistoryRepository;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.PaymentLedgerRepository;
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
import com.playmotech.api.core.services.BulkUserService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.MiscellaneousService;
import com.playmotech.api.core.utils.InstallmentUtil;
import com.playmotech.api.core.utils.excel.BulkUploaderHelper;
import com.playmotech.api.core.utils.excel.ExcelUserUploadUtil;
import com.playmotech.api.core.validation.ExcelValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class BulkUploadAsyncServiceImpl {

	private final ICourseService courseService;
	private final CourseRepo courseRepo;

	public final BulkUserService bulkUserService;
	public final ITraineeService traineeService;
	public final RolesRepo rolesRepo;
	public final AcademyRepo academyRepo;
	public final CoachAcademyMappingRepo coachAcademyMappingRepo;
	public final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
	private final BulkUploadHistoryRepository bulkUploadHistoryRepository;
	private final MiscellaneousService miscellaneousService;

	public final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	public final UserProfileRepo userProfileRepo;

	public final IStorageService storageService;

	public final BulkUploaderHelper bulkUploaderHelper;

	private final PaymentLedgerRepository ledgerRepository;

	private final InstallmentUtil installmentUtil;
	private final PaymentLedgerHelper ledgerHelper;

	@Value("${bulk.upload.s3.bucket}")
	public String bulkUploadBucket;

	@Value("${bulk.upload.base.url}")
	public String bulkUploadBaseUrl;

	/**
	 * Async processing method for program player mapping
	 */
	@Async("taskExecutor")
	@Transactional
	public void processProgramPlayerMappingAsync(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			String programId, boolean saveData, Long historyId) {

		BulkUploadHistory historyEntry = null;
		Course course = null;

		try {
			// Get the history entry to update
			historyEntry = bulkUploadHistoryRepository.findById(historyId)
					.orElseThrow(() -> new EntityNotFoundException("History entry not found"));

			// Get the course
			course = courseRepo.findByAcademy_IdAndId(academyId, programId)
					.orElseThrow(() -> new EntityNotFoundException("Program not found"));

			// Safely initialize the payment options (triggers lazy loading)
			List<CoursePaymentOptionsMapping> paymentOptions = course.getPaymentOptions();
			if (paymentOptions != null) {
				paymentOptions.size(); // Force lazy initialization
			}

			log.info("Starting async processing for program player mapping. History ID: {}, Program ID: {}", historyId,
					programId);

			// Parse Excel if file is provided
			if (file != null) {
				try {
					parsedRows = ExcelUserUploadUtil.parseProgramPlayerEnrollmentExcelWithErrors(file);
					if (parsedRows == null || parsedRows.isEmpty()) {
						updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
								"No valid data found in Excel file", 0, 0, 0);
						return;
					}

					// Update total records count
					historyEntry.setTotalRecords(parsedRows.size());
					bulkUploadHistoryRepository.save(historyEntry);

					log.info("Program player mapping Excel file parsed for history ID: {}. Total rows: {}", historyId,
							parsedRows.size());
				} catch (Exception e) {
					log.error("Excel parsing error for history ID: {}", historyId, e);
					updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
							"Failed to parse Excel file: " + e.getMessage(), 0, 0,
							parsedRows != null ? parsedRows.size() : 0);
					return;
				}
			}

			// Initialize collections for processing
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedProgramPlayerEnrollmentRow> validRows = new ArrayList<>();

			// Process parsing errors using the helper
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);

			// Validate and prepare enrollment rows
			validateAndPrepareProgramPlayerEnrollmentRows(parsedRows, validRows, allErrors, academyId, programId,
					course);

			// Process valid rows (save data if saveData is true)
			int successCount = processValidProgramPlayerEnrollmentRows(validRows, allErrors, course, saveData);
			int failureCount = allErrors.size();

			// Handle error report generation and S3 upload if there are errors and saveData
			// is true
			String errorReportUrl = null;

			if (!allErrors.isEmpty() && saveData) {
				errorReportUrl = generateAndUploadErrorReport(allErrors, parsedRows, historyId,
						BulkType.PLAYER_ENROLLEMENTS);
			}

			// Upload original Excel file to S3 if saveData is true
			String excelFileUrl = null;
			if (file != null && saveData) {
				excelFileUrl = uploadExcelFileToS3(file, historyId, BulkType.PLAYER_ENROLLEMENTS);
			}

			// Determine final status
			BulkUploadHistory.UploadStatus finalStatus;
			if (failureCount == 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED;
			} else if (successCount > 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED_WITH_ERRORS;
			} else {
				finalStatus = BulkUploadHistory.UploadStatus.FAILED;
			}

			// Update history with final results
			updateHistoryWithResults(historyEntry, finalStatus, successCount, failureCount, errorReportUrl,
					excelFileUrl, "Program player mapping completed");

			log.info("Async program player mapping completed for history ID: {}. Success: {}, Errors: {}", historyId,
					successCount, failureCount);

		} catch (Exception e) {
			log.error("Unhandled exception during async program player mapping for history ID: {}", historyId, e);
			if (historyEntry != null) {
				updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
						"Unexpected error: " + e.getMessage(), 0, 0, parsedRows != null ? parsedRows.size() : 0);
			}
		}
	}

	/**
	 * Async processing method for program bulk upload
	 *
	 * @param userId
	 */
	@Async("taskExecutor")
	@Transactional
	public void processProgramUpload(MultipartFile file, List<ParsedRow> parsedRows, String academyId, boolean saveData,
			Long historyId, String userId) {

		BulkUploadHistory historyEntry = null;
		try {
			// Get the history entry to update
			historyEntry = bulkUploadHistoryRepository.findById(historyId)
					.orElseThrow(() -> new EntityNotFoundException("History entry not found"));

			log.info("Starting async processing for program bulk upload. History ID: {}", historyId);

			// Parse Excel if file is provided
			if (file != null) {
				try {
					parsedRows = ExcelUserUploadUtil.parseProgramExcelWithErrors(file);
					if (parsedRows == null || parsedRows.isEmpty()) {
						updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
								"No valid data found in Excel file", 0, 0, 0);
						return;
					}

					// Update total records count
					historyEntry.setTotalRecords(parsedRows.size());
					bulkUploadHistoryRepository.save(historyEntry);

					log.info("Program Excel file parsed for history ID: {}. Total rows: {}", historyId,
							parsedRows.size());
				} catch (Exception e) {
					log.error("Excel parsing error for history ID: {}", historyId, e);
					updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
							"Failed to parse Excel file: " + e.getMessage(), 0, 0,
							parsedRows != null ? parsedRows.size() : 0);
					return;
				}
			}

			// Initialize collections for processing
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedProgramRow> validRows = new ArrayList<>();
			Set<String> courseTitlesInUpload = new HashSet<>();

			// Process parsing errors
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);

			// Validate and prepare program rows
			validateAndPrepareProgramRows(parsedRows, validRows, allErrors, courseTitlesInUpload, academyId);

			// Process valid rows (save data if saveData is true)
			int successCount = processValidProgramRows(validRows, allErrors, academyId, saveData, userId);
			int failureCount = allErrors.size();

			// Handle error report generation and S3 upload if there are errors and saveData
			// is true
			String errorReportUrl = null;
			if (!allErrors.isEmpty() && saveData) {
				errorReportUrl = generateAndUploadErrorReport(allErrors, parsedRows, historyId, BulkType.PROGRAMS);
			}

			// Upload original Excel file to S3 if saveData is true
			String excelFileUrl = null;
			if (file != null && saveData) {
				excelFileUrl = uploadExcelFileToS3(file, historyId, BulkType.PROGRAMS);
			}

			// Determine final status
			BulkUploadHistory.UploadStatus finalStatus;
			if (failureCount == 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED;
			} else if (successCount > 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED_WITH_ERRORS;
			} else {
				finalStatus = BulkUploadHistory.UploadStatus.FAILED;
			}

			// Update history with final results
			updateHistoryWithResults(historyEntry, finalStatus, successCount, failureCount, errorReportUrl,
					excelFileUrl, "Program upload processing completed");

			log.info("Async program upload completed for history ID: {}. Success: {}, Errors: {}", historyId,
					successCount, failureCount);

		} catch (Exception e) {
			log.error("Unhandled exception during async program upload for history ID: {}", historyId, e);
			if (historyEntry != null) {
				updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
						"Unexpected error: " + e.getMessage(), 0, 0, parsedRows != null ? parsedRows.size() : 0);
			}
		}
	}

	/**
	 * Validate and prepare program rows for bulk upload
	 */
	public void validateAndPrepareProgramRows(List<ParsedRow> parsedRows, List<ValidatedProgramRow> validRows,
			List<RowError> allErrors, Set<String> courseTitlesInUpload, String academyId) {

		if (parsedRows == null || parsedRows.isEmpty()) {
			log.warn("No rows to validate for program upload");
			return;
		}

		log.info("Starting validation for {} program rows for academy: {}", parsedRows.size(), academyId);

		for (ParsedRow parsedRow : parsedRows) {
			int rowNumber = parsedRow.getRowNumber();
			List<String> currentErrors = new ArrayList<>();

			log.debug("Processing row number: {}", rowNumber);

			if (parsedRow == null || parsedRow.hasParsingErrors()) {
				log.warn("Row {} has parsing errors or is null", rowNumber);
				// Add parsing errors to allErrors if they exist
				if (parsedRow != null && parsedRow.getParsingErrors() != null
						&& !parsedRow.getParsingErrors().isEmpty()) {
					log.error("Parsing errors for row {}: {}", rowNumber, parsedRow.getParsingErrors());
					allErrors.add(new RowError(rowNumber, parsedRow.getParsingErrors()));
				}
				continue;
			}

			CreateCourseDto courseDto = parsedRow.getCourseDto();
			if (courseDto == null) {
				log.error("Row {}: Course data is missing", rowNumber);
				allErrors.add(new RowError(rowNumber, List.of("Course data is missing")));
				continue;
			}

			if (courseDto.getSchedule() != null) {
				courseDto.getSchedule().setAmount(0L);
			} else {
				log.error("Row {}: Course data is missing", rowNumber);
				allErrors.add(new RowError(rowNumber, List.of("Course data is missing")));
				continue;
			}

			try {
				log.debug("Row {}: Starting field validations", rowNumber);

				// 1. Title validation (Required)
				String title = courseDto.getTitle();
				log.debug("Row {}: Validating title: '{}'", rowNumber, title);
				if (!StringUtils.hasText(title)) {
					log.warn("Row {}: Title validation failed - title is empty or null", rowNumber);
					currentErrors.add(createFieldError("Title", "REQUIRED", "Title is required"));
				} else {
					title = title.trim();
					log.debug("Row {}: Title after trim: '{}', length: {}", rowNumber, title, title.length());

					if (title.length() > 255) {
						log.warn("Row {}: Title validation failed - length {} exceeds 255 characters", rowNumber,
								title.length());
						currentErrors
								.add(createFieldError("Title", "LENGTH", "Title must be less than 255 characters"));
					}

					// Check for duplicate titles
					boolean duplicateInUpload = courseTitlesInUpload.contains(title.toLowerCase());
					boolean duplicateInDb = courseRepo.existsByTitleAndAcademyId(title, academyId);

					log.debug("Row {}: Duplicate check - in upload: {}, in database: {}", rowNumber, duplicateInUpload,
							duplicateInDb);

					if (duplicateInUpload || duplicateInDb) {
						log.warn("Row {}: Title validation failed - duplicate title found. In upload: {}, In DB: {}",
								rowNumber, duplicateInUpload, duplicateInDb);
						currentErrors.add(createFieldError("Title", "DUPLICATE",
								"A program with this title already exists in the academy or in this file"));
					} else {
						courseTitlesInUpload.add(title.toLowerCase());
						log.debug("Row {}: Title added to upload set: '{}'", rowNumber, title.toLowerCase());
					}
				}

				// 2. Description validation (Optional)
				String description = courseDto.getDescription();
				log.debug("Row {}: Validating description: '{}'", rowNumber, description);
				if (StringUtils.hasText(description) && description.length() > 200) {
					log.warn("Row {}: Description validation failed - length {} exceeds 200 characters", rowNumber,
							description.length());
					currentErrors.add(
							createFieldError("Description", "LENGTH", "Description must be less than 200 characters"));
				}

				// 3. Sport validation (Required) - Already parsed, just validate not null
				log.debug("Row {}: Validating sport: {}", rowNumber, courseDto.getSport());
				if (courseDto.getSport() == null) {
					log.warn("Row {}: Sport validation failed - sport is null", rowNumber);
					currentErrors.add(createFieldError("Sport", "REQUIRED", "Sport is required"));
				}

				// 4. Skill Level validation (Required) - Already parsed as 'level'
				log.debug("Row {}: Validating skill level: {}", rowNumber, courseDto.getLevel());
				if (courseDto.getLevel() == null) {
					log.warn("Row {}: Level validation failed - level is null", rowNumber);
					currentErrors.add(createFieldError("level", "REQUIRED", "Skill Level is required"));
				}

				// 5. Age Group validation (Optional) - Already parsed as string
				String ageGroup = courseDto.getAgeGroup();
				log.debug("Row {}: Validating age group: '{}'", rowNumber, ageGroup);
				if (StringUtils.hasText(ageGroup) && ageGroup.length() > 100) {
					log.warn("Row {}: Age group validation failed - length {} exceeds 100 characters", rowNumber,
							ageGroup.length());
					currentErrors
							.add(createFieldError("ageGroup", "LENGTH", "Age Group must be less than 100 characters"));
				}

				// 6. Total Max Trainees validation (Required)
				Long maxTrainees = courseDto.getTotalMaxTrainees();
				log.debug("Row {}: Validating max trainees: {}", rowNumber, maxTrainees);
				if (maxTrainees == null || maxTrainees <= 0) {
					log.warn("Row {}: Max trainees validation failed - value is null or <= 0: {}", rowNumber,
							maxTrainees);
					currentErrors.add(createFieldError("totalMaxTrainees", "INVALID",
							"Total Max Trainees must be a positive number"));
				} else if (maxTrainees > 1000) {
					log.warn("Row {}: Max trainees validation failed - value {} exceeds 1000", rowNumber, maxTrainees);
					currentErrors.add(
							createFieldError("totalMaxTrainees", "RANGE", "Total Max Trainees cannot exceed 1000"));
				}

				// 7. Visibility validation (Required)
				log.debug("Row {}: Validating visibility: {}", rowNumber, courseDto.getVisibility());
				if (courseDto.getVisibility() == null) {
					log.warn("Row {}: Visibility validation failed - visibility is null", rowNumber);
					currentErrors.add(createFieldError("visibility", "REQUIRED", "Visibility is required"));
				}

				// 8. Registration Fee validation (Required)
				Long registrationFee = courseDto.getRegistrationFee();
				log.debug("Row {}: Validating registration fee: {}", rowNumber, registrationFee);
				if (registrationFee == null || registrationFee < 0) {
					log.warn("Row {}: Registration fee validation failed - value is null or < 0: {}", rowNumber,
							registrationFee);
					courseDto.setRegistrationFee(null);
				}

				// 9. Schedule validation
				ScheduleDto schedule = courseDto.getSchedule();
				log.debug("Row {}: Validating schedule: {}", rowNumber, schedule);
				if (schedule == null) {
					log.warn("Row {}: Schedule validation failed - schedule is null", rowNumber);
					currentErrors.add(createFieldError("schedule", "REQUIRED", "Schedule information is required"));
				} else {
					log.debug("Row {}: Calling validateSchedule method", rowNumber);
					validateSchedule(schedule, currentErrors);
					if (!currentErrors.isEmpty()) {
						log.warn("Row {}: Schedule validation produced errors", rowNumber);
					}
				}

				// 10. Payment Options validation
				Map<PaymentSchedule, Long> paymentOptions = courseDto.getPaymentOptions();
				log.debug("Row {}: Validating payment options: {}", rowNumber, paymentOptions);
				if (paymentOptions == null || paymentOptions.isEmpty()) {
					log.warn("Row {}: Payment options validation failed - null or empty", rowNumber);
					currentErrors.add(
							createFieldError("paymentOptions", "REQUIRED", "At least one payment option is required"));
				} else {
					log.debug("Row {}: Calling validatePaymentOptions method", rowNumber);
					validatePaymentOptions(paymentOptions, currentErrors);
					if (!currentErrors.isEmpty()) {
						log.warn("Row {}: Payment options validation produced errors", rowNumber);
					}
				}

			} catch (Exception e) {
				log.error("Row {}: Exception occurred during validation: {}", rowNumber, e.getMessage(), e);
				currentErrors.add("Error processing row: " + e.getMessage());
			}

			if (!currentErrors.isEmpty()) {
				log.warn("Row {}: Validation completed with {} errors: {}", rowNumber, currentErrors.size(),
						currentErrors);
				allErrors.add(new RowError(rowNumber, currentErrors));
			} else {
				log.debug("Row {}: Validation successful - adding to valid rows", rowNumber);
				validRows.add(new ValidatedProgramRow(rowNumber, courseDto, currentErrors));
			}
		}

		log.info("Validation completed for academy {}. Valid rows: {}, Error rows: {}", academyId, validRows.size(),
				allErrors.size());
	}

	/**
	 * Validate schedule information
	 */
	private void validateSchedule(ScheduleDto schedule, List<String> errors) {
		// Schedule Type validation
		if (schedule.getType() == null) {
			errors.add(createFieldError("scheduleType", "REQUIRED", "Schedule Type is required"));
		}

		// Date validation
		String parsedStartDate = parseToStandardFormat(schedule.getStartDate());
		if (!StringUtils.hasText(schedule.getStartDate())) {
			errors.add(createFieldError("startDate", "REQUIRED", "Start Date is required"));
		}

		String parsedEndDate = parseToStandardFormat(schedule.getEndDate());
		if (!StringUtils.hasText(schedule.getEndDate())) {
			errors.add(createFieldError("endDate", "REQUIRED", "End Date is required"));
		}

		// Date range validation
		if (!StringUtils.hasText(schedule.getStartDate()) && !StringUtils.hasText(schedule.getEndDate())
				&& isValidDate(schedule.getStartDate()) && isValidDate(schedule.getEndDate())) {
			try {
				LocalDate startDate = parseDate(schedule.getStartDate());
				LocalDate endDate = parseDate(schedule.getEndDate());
				if (endDate.isBefore(startDate)) {
					errors.add(createFieldError("dateRange", "INVALID", "End Date must be after Start Date"));
				}
			} catch (Exception e) {
				errors.add(createFieldError("dateRange", "INVALID", "Invalid date format"));
			}
		}

		// Time validation
		if (!StringUtils.hasText(schedule.getStartTime())) {
			errors.add(createFieldError("startTime", "REQUIRED", "Session Start Time is required"));
		} else {
			// if (!isValidTime(formatTimeToAmPm(schedule.getStartTime()))) {
			// errors.add(createFieldError("startTime", "INVALID", "Start Time must be in
			// HH:MM format"));
			// } else {
			// Format time to hh:mm A format
			schedule.setStartTime(formatTimeToAmPm(schedule.getStartTime()));
			// }
		}

		if (!StringUtils.hasText(formatTimeToAmPm(schedule.getEndTime()))) {
			errors.add(createFieldError("endTime", "REQUIRED", "Session End Time is required"));
		} else {
			// if (!isValidTime(formatTimeToAmPm(schedule.getEndTime()))) {
			// errors.add(createFieldError("endTime", "INVALID", "End Time must be in HH:MM
			// format"));
			// } else {
			// Format time to hh:mm A format
			schedule.setEndTime(formatTimeToAmPm(schedule.getEndTime()));
			// }
		}

		// Time range validation
		if (!StringUtils.hasText(schedule.getStartTime()) && !StringUtils.hasText(schedule.getEndTime())
				&& isValidTime(schedule.getStartTime()) && isValidTime(schedule.getEndTime())) {
			try {
				LocalTime startTime = LocalTime.parse(schedule.getStartTime());
				LocalTime endTime = LocalTime.parse(schedule.getEndTime());
				if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
					errors.add(createFieldError("timeRange", "INVALID", "End Time must be after Start Time"));
				}
			} catch (Exception e) {
				errors.add(createFieldError("timeRange", "INVALID", "Invalid time format"));
			}
		}

		// Weekdays validation for specific schedule types
		if (schedule.getType() != null && (schedule.getType() == ScheduleType.WEEKDAYS)) {
			if (schedule.getWeekdays() == null || schedule.getWeekdays().isEmpty()) {
				errors.add(
						createFieldError("weekdays", "REQUIRED", "Days of Week are required for this schedule type"));
			}
		}

	}

	/**
	 * Validate payment options
	 */
	private void validatePaymentOptions(Map<PaymentSchedule, Long> paymentOptions, List<String> errors) {
		for (Map.Entry<PaymentSchedule, Long> entry : paymentOptions.entrySet()) {
			PaymentSchedule schedule = entry.getKey();
			Long amount = entry.getValue();

			if (schedule == null) {
				errors.add(createFieldError("paymentSchedule", "INVALID", "Invalid payment schedule type"));
			}

			if (amount == null || amount <= 0) {
				errors.add(createFieldError("paymentAmount", "INVALID", "Payment amounts must be positive numbers"));
			}
		}
	}

	/**
	 * Process valid program rows (create courses)
	 */
	public int processValidProgramRows(List<ValidatedProgramRow> validRows, List<RowError> allErrors, String academyId,
			boolean saveData, String userId) {
		int successCount = 0;

		if (!saveData) {
			// If not saving, all valid rows are considered a "success" for the count
			return validRows.size();
		}

		for (ValidatedProgramRow validRow : validRows) {
			try {
				log.info("Processing valid program row #{}", validRow.getRowNumber());
				courseService.createCourse(academyId, userId, validRow.getCreateCourseDto(), true);
				successCount++;
			} catch (Exception e) {
				log.error("Error processing row number {}: {}", validRow.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(validRow.getRowNumber(),
						List.of(BulkUploaderHelper.createGeneralError("ROW_PROCESSING_ERROR",
								"An unexpected error occurred while saving the program: " + e.getMessage()))));
			}
		}

		return successCount;
	}

	// Helper methods for validation
	private boolean isValidDate(String dateStr) {
		if (dateStr == null || dateStr.trim().isEmpty()) {
			return false;
		}

		String[] patterns = { "dd-MM-yyyy", "dd/MM/yyyy", "d-M-yyyy", "d/M/yyyy", "yyyy-MM-dd", "yyyy/MM/dd" };

		for (String pattern : patterns) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
						.withResolverStyle(ResolverStyle.STRICT);
				LocalDate.parse(dateStr, formatter);
				return true;
			} catch (Exception e) {
				// Try next format
			}
		}

		return false;
	}

	private String parseToStandardFormat(String dateStr) {
		if (dateStr == null || dateStr.trim().isEmpty()) {
			return null;
		}

		String[] patterns = { "dd-MM-yyyy", "dd/MM/yyyy", "d-M-yyyy", "d/M/yyyy", "yyyy-MM-dd", "yyyy/MM/dd" };

		for (String pattern : patterns) {
			try {
				DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(pattern)
						.withResolverStyle(ResolverStyle.STRICT);
				LocalDate date = LocalDate.parse(dateStr, inputFormatter);

				// Output format: dd-MM-yyyy
				DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				return date.format(outputFormatter);
			} catch (Exception e) {
				// Try next format
			}
		}

		return null; // Or throw an exception if needed
	}

	private LocalDate parseDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		return LocalDate.parse(dateStr, formatter);
	}

	private boolean isValidTime(String timeStr) {
		if (timeStr == null || timeStr.trim().isEmpty()) {
			return false;
		}

		try {
			LocalTime.parse(timeStr);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Format time from HH:MM (24-hour) to hh:mm a (12-hour with AM/PM) format
	 */
	private String formatTimeToAmPm(String timeStr) {
		if (timeStr == null || timeStr.trim().isEmpty()) {
			return null;
		}

		try {
			// Parse the input time (expected in HH:MM format)
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("HH:mm");
			LocalTime time = LocalTime.parse(timeStr, inputFormatter);

			// Format to 12-hour with AM/PM
			DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("hh:mm a");
			return time.format(outputFormatter);
		} catch (Exception e) {
			log.error("Error formatting time to AM/PM format: {}", timeStr, e);
			return timeStr; // Return original if parsing fails
		}
	}

	/**
	 * Validate payment schedule with structured error
	 */
	public void validatePaymentSchedule(ProgramPlayerEnrollmentDto enrollmentDto, Course course,
			List<String> rowErrors) {
		if (enrollmentDto.getPaymentSchedule() != null) {
			boolean validPaymentOption = course.getPaymentOptions().stream()
					.anyMatch(option -> option.getPaymentSchedule().equals(enrollmentDto.getPaymentSchedule()));
			if (!validPaymentOption) {
				rowErrors.add(BulkUploaderHelper.createFieldError("Payment Schedule", "INVALID_VALUE",
						"Payment schedule '" + enrollmentDto.getPaymentSchedule() + "' is not valid for this program"));
			}
		}
	}

	/**
	 * Validate and find user with structured errors - handles multiple users with
	 * same phone number
	 */
	public UserProfile validateAndFindUser(ProgramPlayerEnrollmentDto enrollmentDto, String academyId, String programId,
			List<String> rowErrors) {

		if (enrollmentDto.getPhoneNumber() == null || enrollmentDto.getPhoneNumber().trim().isEmpty()) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Phone Number", "REQUIRED", "Phone number is required"));
			return null;
		}

		// Format phone number for database lookup
		String formattedPhone = formatPhoneNumber(enrollmentDto.getPhoneNumber());

		// Find user by phone number
		List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(formattedPhone, false);

		if (existingUsers.isEmpty()) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Phone Number", "NOT_FOUND",
					"User with phone number " + enrollmentDto.getPhoneNumber() + " not found"));
			return null;
		}

		// Handle multiple users with same phone number
		if (existingUsers.size() > 1) {
			log.warn("Multiple users found with phone number: {}", enrollmentDto.getPhoneNumber());

			// If full name is provided, try to find exact match
			if (enrollmentDto.getFullName() != null && !enrollmentDto.getFullName().trim().isEmpty()) {
				List<UserProfile> matchingUsers = findUsersByNameMatch(existingUsers, enrollmentDto.getFullName());

				if (matchingUsers.isEmpty()) {
					rowErrors.add(BulkUploaderHelper.createFieldError("Full Name", "NO_MATCH",
							"No user found with matching name '" + enrollmentDto.getFullName() + "' among "
									+ existingUsers.size() + " users with this phone number"));
					return null;
				} else if (matchingUsers.size() > 1) {
					// Multiple users with same name and phone - cannot determine unique user
					rowErrors.add(BulkUploaderHelper.createFieldError("User Identity", "AMBIGUOUS",
							"Multiple users found with same name and phone number. Cannot determine unique user."));
					return null;
				} else {
					// Single user found with matching name
					return validateSingleUser(matchingUsers.get(0), enrollmentDto, academyId, programId, rowErrors);
				}
			} else {
				// No name provided, cannot disambiguate
				rowErrors.add(BulkUploaderHelper.createFieldError("Full Name", "REQUIRED_FOR_DISAMBIGUATION",
						"Full name is required when multiple users exist with phone number "
								+ enrollmentDto.getPhoneNumber()));
				return null;
			}
		}

		// Single user found
		return validateSingleUser(existingUsers.get(0), enrollmentDto, academyId, programId, rowErrors);
	}

	/**
	 * Find users by name match from a list of users
	 */
	public List<UserProfile> findUsersByNameMatch(List<UserProfile> users, String targetName) {
		String normalizedTargetName = targetName.trim().toLowerCase();

		return users.stream().filter(user -> {
			String dbName = user.getDisplayName() != null ? user.getDisplayName().trim().toLowerCase() : "";
			return dbName.equals(normalizedTargetName);
		}).collect(Collectors.toList());
	}

	/**
	 * Validate a single user profile against enrollment criteria
	 */
	public UserProfile validateSingleUser(UserProfile user, ProgramPlayerEnrollmentDto enrollmentDto, String academyId,
			String programId, List<String> rowErrors) {

		// Check if user type is PLAYER
		if (user.getUserType() != UserType.PLAYER) {
			rowErrors.add(BulkUploaderHelper.createFieldError("User Type", "INVALID_TYPE",
					"User is not a player, cannot enroll in program"));
			return null;
		}

		// Check if full name matches (case-insensitive, trimmed) - only if name is
		// provided
		if (enrollmentDto.getFullName() != null && !enrollmentDto.getFullName().trim().isEmpty()) {
			String dbName = user.getDisplayName() != null ? user.getDisplayName().trim().toLowerCase() : "";
			String excelName = enrollmentDto.getFullName().trim().toLowerCase();

			if (!dbName.equals(excelName)) {
				rowErrors.add(
						BulkUploaderHelper.createFieldError("Full Name", "MISMATCH", "Full name mismatch. Database: '"
								+ user.getDisplayName() + "', Excel: '" + enrollmentDto.getFullName() + "'"));
				return null;
			}
		}

		// Check if user is enrolled in this academy
		boolean isInAcademy = traineeAcademyMappingRepo.existsByTraineeUserProfileIdAndAcademyId(user.getId(),
				academyId);
		if (!isInAcademy) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Academy Enrollment", "NOT_ENROLLED",
					"User is not enrolled in this academy"));
			return null;
		}

		// Check if already enrolled in this specific program
		boolean isAlreadyEnrolled = traineeCourseEnrollmentRepo
				.existsByCourseIdAndTraineeUserProfileIdAndStatus(programId, user.getId(), Status.ACTIVE);

		if (isAlreadyEnrolled) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Program Enrollment", "ALREADY_ENROLLED",
					"User is already enrolled in this program"));
			return null;
		}

		// Check if user has inactive enrollment in this program
		boolean hasInactiveEnrollment = traineeCourseEnrollmentRepo
				.existsByCourseIdAndTraineeUserProfileIdAndStatus(programId, user.getId(), Status.INACTIVE);

		if (hasInactiveEnrollment) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Program Enrollment", "INACTIVE_ENROLLMENT",
					"User has inactive enrollment in this program. Please reactivate existing enrollment."));
			return null;
		}

		return user;
	}

	/**
	 * Enhanced validation method that also handles payment schedule and other
	 * validations
	 */
	public void validateAndPrepareProgramPlayerEnrollmentRows(List<ParsedRow> parsedRows,
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
				// Validate payment schedule
				validatePaymentSchedule(enrollmentDto, course, rowErrors);

				// Validate and find user (handles all scenarios including multiple users)
				UserProfile user = validateAndFindUser(enrollmentDto, academyId, programId, rowErrors);

				if (user != null) {
					enrollmentDto.setUserProfile(user);
				}

				// Validate dates
				validateDates(enrollmentDto, rowErrors, course);

				// Validate fee amount
				validateFeeAmount(enrollmentDto, rowErrors);

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

	/**
	 * Validate dates with structured errors
	 */
	public void validateDates(ProgramPlayerEnrollmentDto enrollmentDto, List<String> rowErrors, Course course) {
		LocalDate joiningDate = enrollmentDto.getJoiningDate();
		LocalDate nextDueDate = enrollmentDto.getNextDueDate();
		Schedule schedule = course.getSchedule();

		if (joiningDate != null && nextDueDate != null && joiningDate.isAfter(nextDueDate)) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Dates", "INVALID_RANGE",
					"Joining date cannot be after due date"));
		}

		if (schedule != null) {
			try {
				LocalDate scheduleStartDate = LocalDate.parse(schedule.getStartDate());
				LocalDate scheduleEndDate = LocalDate.parse(schedule.getEndDate());

				if (joiningDate != null
						&& (joiningDate.isBefore(scheduleStartDate) || joiningDate.isAfter(scheduleEndDate))) {
					rowErrors.add(BulkUploaderHelper.createFieldError("Joining Date", "OUT_OF_SCHEDULE",
							"Joining date must be within course schedule range (" + scheduleStartDate + " to "
									+ scheduleEndDate + ")"));
				}

				if (nextDueDate != null
						&& (nextDueDate.isBefore(scheduleStartDate) || nextDueDate.isAfter(scheduleEndDate))) {
					rowErrors.add(BulkUploaderHelper.createFieldError("Next Due Date", "OUT_OF_SCHEDULE",
							"Next due date must be within course schedule range (" + scheduleStartDate + " to "
									+ scheduleEndDate + ")"));
				}
			} catch (DateTimeParseException e) {
				rowErrors.add(BulkUploaderHelper.createFieldError("Schedule Dates", "INVALID_FORMAT",
						"Schedule start or end date is in invalid format"));
			}
		}
	}

	/**
	 * Validate fee amount with structured errors
	 */
	public void validateFeeAmount(ProgramPlayerEnrollmentDto enrollmentDto, List<String> rowErrors) {
		if (enrollmentDto.getFeeAmount() != null && enrollmentDto.getFeeAmount() <= 0) {
			rowErrors.add(BulkUploaderHelper.createFieldError("Fee Amount", "INVALID_VALUE",
					"Fee amount must be greater than 0"));
		}
	}

	/**
	 * Process enrollments with duplicate check and error if inactive. Enhanced with
	 * comprehensive validation, installment calculations, and ledger entries.
	 */
	public int processValidProgramPlayerEnrollmentRows(List<ValidatedProgramPlayerEnrollmentRow> validRows,
			List<RowError> allErrors, Course course, boolean saveData) {

		int success = 0;

		for (ValidatedProgramPlayerEnrollmentRow row : validRows) {
			ProgramPlayerEnrollmentDto dto = row.getEnrollmentDto();

			try {
				if (saveData) {
					String courseId = course.getId();
					String traineeId = dto.getUserProfile().getId();
					String academyId = course.getAcademy().getId();

					// ✅ Fetch any existing enrollments (might be multiple, use first)
					List<TraineeCourseEnrollment> existingList = traineeCourseEnrollmentRepo
							.findByCourse_IdInAndTraineeUserProfile_Id(List.of(courseId), traineeId);

					long activeCount = existingList.stream().filter(e -> Status.ACTIVE.equals(e.getStatus())).count();

					long inactiveCount = existingList.stream().filter(e -> Status.INACTIVE.equals(e.getStatus()))
							.count();

					// ✅ Business Rule Enforcement: Only 1 active enrollment allowed per course
					if (activeCount > 1) {
						log.error(
								"❌ Data inconsistency: Player {} has {} active enrollments for course {}. Manual fix needed.",
								traineeId, activeCount, courseId);
						allErrors.add(new RowError(row.getRowNumber(),
								List.of(BulkUploaderHelper.createGeneralError("MULTIPLE_ACTIVE_ENROLLMENTS",
										"Player has multiple active enrollments. Please resolve manually."))));
						continue;
					} else if (activeCount == 1) {
						log.warn("⚠️ Player {} already has an active enrollment in course {}. Skipping.", traineeId,
								courseId);
						allErrors.add(new RowError(row.getRowNumber(), List.of(BulkUploaderHelper
								.createGeneralError("DUPLICATE_ENROLLMENT", "Player is already actively enrolled."))));
						continue;
					}

					// 🔄 Optional: You can also decide what to do with inactive enrollments
					if (inactiveCount > 0) {
						log.info("ℹ️ Player {} has {} inactive enrollment(s) in course {}. Skipping.", traineeId,
								inactiveCount, courseId);
						allErrors.add(new RowError(row.getRowNumber(),
								List.of(BulkUploaderHelper.createGeneralError("INACTIVE_ENROLLMENT",
										"Player is enrolled but inactive. Please reactivate manually if needed."))));
						continue;
					}

					// ✅ Enhanced validations from the original method
					// Validate payment schedule
					CourseDto courseDto = courseService.adaptCourseDtos(List.of(course)).stream().findFirst()
							.orElse(null);

					if (dto.getPaymentSchedule() == null
							|| !courseDto.getPaymentOptions().containsKey(dto.getPaymentSchedule())) {
						log.error("Invalid payment schedule for trainee with id {} in row {}", traineeId,
								row.getRowNumber());
						allErrors.add(new RowError(row.getRowNumber(), List.of(BulkUploaderHelper
								.createGeneralError("INVALID_PAYMENT_SCHEDULE", "Invalid payment schedule"))));
						continue;
					}

					// Validate dates
					LocalDate joiningDate = dto.getJoiningDate() != null ? dto.getJoiningDate() : LocalDate.now();
					LocalDate dueDate = dto.getNextDueDate();

					if (dueDate != null && joiningDate.isAfter(dueDate)) {
						log.error("Invalid trainee due date or joining date. Trainee ID: {} in row {}", traineeId,
								row.getRowNumber());
						allErrors.add(new RowError(row.getRowNumber(), List.of(BulkUploaderHelper
								.createGeneralError("INVALID_DATES", "Joining date cannot be after the due date"))));
						continue;
					}

					// ✅ Calculate installments and due dates like in the original method
					TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();
					enrollmentDto.setAmount(dto.getFeeAmount());
					enrollmentDto.setJoiningDate(joiningDate);
					enrollmentDto.setDueDate(dueDate != null ? dueDate : joiningDate.plusMonths(1)); // Default due date
																										// if not
																										// provided
					enrollmentDto.setPaymentSchedule(dto.getPaymentSchedule());

					Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
							.calculateInstallmentsAndDueDates(enrollmentDto, courseDto,
									enrollmentDto.getPaymentSchedule());

					List<InstallmentInfo> allInstallments = result.getLeft();
					List<LocalDate> dueDates = result.getRight();

					LocalDate lastDue = installmentUtil.getLastPassedDueDate(dueDates, LocalDate.now());

					List<InstallmentInfo> installmentsBeforeLastDue = new ArrayList<>();
					InstallmentInfo lastDueInstallment = null;

					if (lastDue != null) {
						for (InstallmentInfo inst : allInstallments) {
							if (inst.getDueDate().isBefore(lastDue)) {
								installmentsBeforeLastDue.add(inst);
							} else if (inst.getDueDate().equals(lastDue)) {
								lastDueInstallment = inst;
							}
						}
					}

					// ✅ Create and configure enrollment entity with all calculated values
					TraineeCourseEnrollment newEnrollment = new TraineeCourseEnrollment();
					newEnrollment.setId(UUID.randomUUID().toString());
					newEnrollment.setTraineeUserProfile(dto.getUserProfile());
					newEnrollment.setCourse(course);
					newEnrollment.setAcademy(course.getAcademy());
					newEnrollment.setStatus(Status.ACTIVE);
					newEnrollment.setCreatedOn(Timestamp.from(Instant.now()));
					newEnrollment.setPaymentSchedule(dto.getPaymentSchedule());
					newEnrollment.setAmount(dto.getFeeAmount());
					newEnrollment.setJoiningDate(joiningDate);
					newEnrollment.setDueDate(enrollmentDto.getDueDate());
					newEnrollment.setUseForFuture(Boolean.FALSE);
					newEnrollment.setDiscountAmount(0L);

					// ✅ Set calculated values for dues like in the original method
					if (lastDueInstallment != null) {
						newEnrollment.setDuesOn(lastDueInstallment.getDueDate());
						newEnrollment.setFinalDueAmount(lastDueInstallment.getAmount());
					} else {
						newEnrollment.setDuesOn(enrollmentDto.getDueDate());
						newEnrollment.setFinalDueAmount(dto.getFeeAmount());
					}

					// ✅ Save enrollment entity once with all final values
					TraineeCourseEnrollment savedEnrollment = traineeCourseEnrollmentRepo.save(newEnrollment);

					// ✅ Collect all ledger entries to save in batch like in the original method
					List<PaymentLedger> ledgersToSave = new ArrayList<>();

					// Create past due ledger entries for installments before last due date
					for (InstallmentInfo pastInst : installmentsBeforeLastDue) {
						PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(academyId, courseId,
								savedEnrollment, pastInst);
						ledgersToSave.add(pastDueLedger);
					}

					// Create registration fee ledger entry
					PaymentLedger registrationFeeLedger = ledgerHelper.createRegistrationFeeLedger(academyId, courseId,
							savedEnrollment, courseDto);
					ledgersToSave.add(registrationFeeLedger);

					// Save all ledgers at once
					ledgerRepository.saveAll(ledgersToSave);

					log.info(
							"Successfully enrolled trainee {} in course {} with payment schedule {} from bulk upload row {}",
							traineeId, courseId, dto.getPaymentSchedule(), row.getRowNumber());
				}

				success++; // Count as success even in preview mode

			} catch (Exception e) {
				log.error("Error enrolling player in row {}: {}", row.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(), List.of(BulkUploaderHelper
						.createGeneralError("ENROLLMENT_ERROR", "Failed to enroll player: " + e.getMessage()))));
			}
		}

		return success;
	}

	/**
	 * Async processing method that handles the actual bulk upload
	 */
	@Async("taskExecutor")
	@Transactional
	public void processBulkUploadAsync(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			UserType userType, boolean saveData, Long historyId, boolean edit) {

		BulkUploadHistory historyEntry = null;
		try {
			// Get the history entry to update
			historyEntry = bulkUploadHistoryRepository.findById(historyId)
					.orElseThrow(() -> new EntityNotFoundException("History entry not found"));

			log.info("Starting async processing for bulk upload. History ID: {}, User Type: {}", historyId,
					userType.name());

			// Parse Excel if file is provided
			if (file != null) {
				try {
					parsedRows = parseExcelByUserType(file, userType);
					if (parsedRows == null || parsedRows.isEmpty()) {
						updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
								"No valid data found in Excel file", 0, 0, 0);
						return;
					}

					// Update total records count
					historyEntry.setTotalRecords(parsedRows.size());
					bulkUploadHistoryRepository.save(historyEntry);

					log.info("Excel file parsed for history ID: {}. Total rows: {}", historyId, parsedRows.size());
				} catch (Exception e) {
					log.error("Excel parsing error for history ID: {}", historyId, e);
					updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
							"Failed to parse Excel file: " + e.getMessage(), 0, 0,
							parsedRows != null ? parsedRows.size() : 0);
					return;
				}
			}

			// Initialize collections for processing
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedRow> validRows = new ArrayList<>();
			Map<String, List<Integer>> phoneNumberToRows = new HashMap<>();
			Map<String, List<Integer>> emailToRows = new HashMap<>();

			// Process parsing errors
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);

			// Validate rows
			validateAndPrepareRows(parsedRows, validRows, allErrors, phoneNumberToRows, emailToRows, academyId,
					userType, edit);

			// Process valid rows (save data if saveData is true)
			int successCount = processValidRows(validRows, allErrors, academyId, userType, saveData, edit);
			int failureCount = allErrors.size();

			// Handle error report generation and S3 upload if there are errors and saveData
			// is true
			String errorReportUrl = null;
			if (!allErrors.isEmpty() && saveData) {
				errorReportUrl = generateAndUploadErrorReport(allErrors, parsedRows, historyId,
						userType.equals(UserType.COACH) ? BulkType.COACHES : BulkType.PLAYERS);
			}

			// Upload original Excel file to S3 if saveData is true
			String excelFileUrl = null;
			if (file != null && saveData) {
				excelFileUrl = uploadExcelFileToS3(file, historyId,
						userType.equals(UserType.COACH) ? BulkType.COACHES : BulkType.PLAYERS);
			}

			// Determine final status
			BulkUploadHistory.UploadStatus finalStatus;
			if (failureCount == 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED;
			} else if (successCount > 0) {
				finalStatus = BulkUploadHistory.UploadStatus.COMPLETED_WITH_ERRORS;
			} else {
				finalStatus = BulkUploadHistory.UploadStatus.FAILED;
			}

			// Update history with final results
			updateHistoryWithResults(historyEntry, finalStatus, successCount, failureCount, errorReportUrl,
					excelFileUrl, "Processing completed");

			log.info("Async bulk upload completed for history ID: {}. Success: {}, Errors: {}", historyId, successCount,
					failureCount);

		} catch (Exception e) {
			log.error("Unhandled exception during async bulk upload for history ID: {}", historyId, e);
			if (historyEntry != null) {
				updateHistoryStatus(historyEntry, BulkUploadHistory.UploadStatus.FAILED,
						"Unexpected error: " + e.getMessage(), 0, 0, parsedRows != null ? parsedRows.size() : 0);
			}
		}
	}

	/**
	 * Updates history entry with final results
	 */
	public void updateHistoryWithResults(BulkUploadHistory historyEntry, BulkUploadHistory.UploadStatus status,
			int successCount, int failureCount, String errorReportUrl, String excelFileUrl, String remarks) {
		try {
			historyEntry.setStatus(status);
			historyEntry.setSuccessCount(successCount);
			historyEntry.setFailureCount(failureCount);
			historyEntry.setCompletedAt(LocalDateTime.now());
			historyEntry.setRemarks(remarks);

			if (errorReportUrl != null) {
				historyEntry.setErrorReportUrl(errorReportUrl);
			}

			if (excelFileUrl != null) {
				historyEntry.setExcelFileUrl(excelFileUrl);
			}

			bulkUploadHistoryRepository.save(historyEntry);
			log.info("History entry updated successfully. ID: {}, Status: {}", historyEntry.getId(), status);

		} catch (Exception e) {
			log.error("Failed to update history entry with ID: {}", historyEntry.getId(), e);
		}
	}

	/**
	 * Updates history status for error cases
	 */
	public void updateHistoryStatus(BulkUploadHistory historyEntry, BulkUploadHistory.UploadStatus status,
			String remarks, int successCount, int failureCount, int totalRecords) {
		try {
			historyEntry.setStatus(status);
			historyEntry.setSuccessCount(successCount);
			historyEntry.setFailureCount(failureCount);
			historyEntry.setTotalRecords(totalRecords);
			historyEntry.setCompletedAt(LocalDateTime.now());
			historyEntry.setRemarks(remarks);

			bulkUploadHistoryRepository.save(historyEntry);
			log.info("History status updated. ID: {}, Status: {}, Remarks: {}", historyEntry.getId(), status, remarks);

		} catch (Exception e) {
			log.error("Failed to update history status for ID: {}", historyEntry.getId(), e);
		}
	}

	/**
	 * Generates error report and uploads to S3
	 */
	public String generateAndUploadErrorReport(List<RowError> allErrors, List<ParsedRow> parsedRows, Long historyId,
			BulkType type) {
		try {
			log.info("Generating error report for history ID: {}", historyId);

			// Create error report entries
			List<ErrorReportEntry> reportEntries = compileErrorReportEntries(parsedRows, allErrors);

			// Map BulkType to TemplateType
			ErrorReportGenerator.TemplateType templateType;
			switch (type) {
			case COACHES -> templateType = ErrorReportGenerator.TemplateType.COACH;
			case PLAYERS -> templateType = ErrorReportGenerator.TemplateType.PLAYER;
			case PROGRAMS -> templateType = ErrorReportGenerator.TemplateType.PROGRAM;
			case PLAYER_ENROLLEMENTS ->
				templateType = ErrorReportGenerator.TemplateType.PROGRAM_PLAYER_ENROLLMENT_HEADERS;
			default -> {
				log.warn("Unsupported BulkType '{}' for error report generation", type);
				return null;
			}
			}

			// Generate Excel report
			ByteArrayOutputStream reportStream = ErrorReportGenerator.generate(reportEntries, templateType);

			// Create a unique file name
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String uniqueId = UUID.randomUUID().toString().substring(0, 8);
			String bulkTypeFormatted = type.name().toLowerCase();

			String fileName = String.format("error_report_%s_%s_hist_%d_%s.xlsx", bulkTypeFormatted, timestamp,
					historyId, uniqueId);

			// Upload to S3
			String folder = "users-media/bulk-upload-reports/";
			String fullStoragePath = folder + fileName;
			byte[] reportBytes = reportStream.toByteArray();

			storageService.upload(bulkUploadBucket, fullStoragePath, reportBytes,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			String reportUrl = bulkUploadBaseUrl + "bulk-upload-reports/" + fileName;
			log.info("Error report uploaded successfully for history ID: {}. URL: {}", historyId, reportUrl);

			return reportUrl;

		} catch (Exception e) {
			log.error("Failed to generate/upload error report for history ID: {}", historyId, e);
			return null;
		}
	}

	/**
	 * Uploads original Excel file to S3
	 */
	public String uploadExcelFileToS3(MultipartFile file, Long historyId, BulkType type) {
		try {
			log.info("Uploading Excel file to S3 for history ID: {}", historyId);

			// Clean and format original filename
			String originalFileName = file.getOriginalFilename();
			String cleanedOriginalName = cleanFileName(originalFileName);

			// Create unique, formatted file name
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String uniqueId = UUID.randomUUID().toString().substring(0, 8);
			String userTypeFormatted = type.name().toLowerCase().replace(" ", "_");

			String fileName = String.format("original_%s_%s_hist_%d_%s_%s", userTypeFormatted, timestamp, historyId,
					uniqueId, cleanedOriginalName);

			String folder = "users-media/bulk-upload-files/";
			String fullStoragePath = folder + fileName;

			// Upload original file
			storageService.upload(bulkUploadBucket, fullStoragePath, file.getBytes(), file.getContentType());

			String fileUrl = bulkUploadBaseUrl + fileName;
			log.info("Excel file uploaded successfully for history ID: {}. URL: {}", historyId, fileUrl);

			return fileUrl;

		} catch (Exception e) {
			log.error("Failed to upload Excel file for history ID: {}", historyId, e);
			return null;
		}
	}

	/**
	 * Utility method to clean file names - removes special characters, spaces, etc.
	 */
	public String cleanFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return "unnamed_file";
		}

		// Get file extension
		String extension = "";
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
			extension = fileName.substring(lastDotIndex);
			fileName = fileName.substring(0, lastDotIndex);
		}

		// Clean the filename
		String cleanedName = fileName.toLowerCase() // Convert to lowercase
				.replaceAll("[^a-zA-Z0-9._-]", "_") // Replace special chars with underscore
				.replaceAll("_{2,}", "_") // Replace multiple underscores with single
				.replaceAll("^_+|_+$", ""); // Remove leading/trailing underscores

		// Ensure name is not empty
		if (cleanedName.isEmpty()) {
			cleanedName = "file";
		}

		// Limit length to reasonable size (50 chars max for name part)
		if (cleanedName.length() > 50) {
			cleanedName = cleanedName.substring(0, 50);
		}

		return cleanedName + extension;
	}

	/**
	 * Generic bulk upload method that handles both coaches and players with save
	 * control Supports both MultipartFile and List<ParsedRow> inputs
	 */
	public ServiceResponse bulkUploadUsers(MultipartFile file, List<ParsedRow> parsedRows, String academyId,
			UserType userType, boolean saveData, boolean edit) {
		String inputType = (file != null) ? "file" : "parsed rows";
		log.info("Bulk upload started for {} from {}. Academy ID: {}, Save: {}", userType.name().toLowerCase(),
				inputType, academyId, saveData);

		try {
			// Input validation
			if (file != null) {
				// File-based validation
				List<String> inputErrors = validateInputParameters(file, academyId);
				if (!inputErrors.isEmpty()) {
					return createErrorResponse(String.join("; ", inputErrors), HttpStatus.BAD_REQUEST);
				}
			} else {
				// ParsedRow-based validation
				if (parsedRows == null || parsedRows.isEmpty()) {
					return createErrorResponse("No data provided for processing", HttpStatus.BAD_REQUEST);
				}
				if (academyId == null || academyId.trim().isEmpty()) {
					return createErrorResponse("Academy ID is required", HttpStatus.BAD_REQUEST);
				}
			}

			// Parse Excel if file is provided, otherwise use provided parsed rows
			if (file != null) {
				try {
					parsedRows = parseExcelByUserType(file, userType);
					if (parsedRows == null || parsedRows.isEmpty()) {
						return handleEmptyParseResult(parsedRows);
					}
					log.info("{} Excel file parsed. Total rows: {}", userType.name(), parsedRows.size());
				} catch (IOException e) {
					log.error("File reading error", e);
					return createErrorResponse("Failed to read Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				} catch (Exception e) {
					log.error("Excel parsing error", e);
					return createErrorResponse("Failed to parse Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
				}
			} else {
				log.info("{} parsed rows received. Total rows: {}", userType.name(), parsedRows.size());
			}

			// Initialize collections
			List<RowError> allErrors = new ArrayList<>();
			List<ValidatedRow> validRows = new ArrayList<>();
			Map<String, List<Integer>> phoneNumberToRows = new HashMap<>();
			Map<String, List<Integer>> emailToRows = new HashMap<>();

			// Process parsing errors
			bulkUploaderHelper.processParsingErrors(parsedRows, allErrors);

			// Validate rows based on user type
			validateAndPrepareRows(parsedRows, validRows, allErrors, phoneNumberToRows, emailToRows, academyId,
					userType, edit);

			// Process valid rows based on user type and save flag
			int success = processValidRows(validRows, allErrors, academyId, userType, saveData, edit);

			return generateEnhancedResponse(allErrors, success,
					userType.equals(UserType.COACH) ? BulkType.COACHES : BulkType.PLAYERS, parsedRows, saveData, null,
					academyId);

		} catch (Exception e) {
			log.error("Unhandled exception during {} bulk upload from {}", userType.name().toLowerCase(), inputType, e);
			return createErrorResponse("Unexpected error occurred: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Dynamic Excel parsing based on user type
	 */
	public List<ParsedRow> parseExcelByUserType(MultipartFile file, UserType userType) throws IOException {
		return switch (userType) {
		case COACH -> ExcelUserUploadUtil.parseCoachExcelWithErrors(file);
		case PLAYER -> ExcelUserUploadUtil.parsePlayerExcelWithErrors(file);
		default -> throw new IllegalArgumentException("Unsupported user type for bulk upload: " + userType);
		};
	}

	/**
	 * Generic validation method that handles both user types
	 */
	public void validateAndPrepareRows(List<ParsedRow> parsedRows, List<ValidatedRow> validRows,
			List<RowError> allErrors, Map<String, List<Integer>> phoneNumberToRows,
			Map<String, List<Integer>> emailToRows, String academyId, UserType userType, boolean edit) {

		if (parsedRows == null || parsedRows.isEmpty()) {
			log.warn("No rows to validate for {}", userType.name().toLowerCase());
			return;
		}

		List<String> academyIds = List.of(academyId);
		buildDuplicateDetectionMaps(parsedRows, phoneNumberToRows, emailToRows);

		for (ParsedRow row : parsedRows) {
			if (row == null || row.hasParsingErrors()) {
				continue;
			}

			UserProfileDto dto = row.getUserDto();
			if (dto == null) {
				log.warn("UserDto is null for row {}", row.getRowNumber());
				allErrors.add(new RowError(row.getRowNumber(),
						List.of(createGeneralError("MISSING_DATA", "User data is missing"))));
				continue;
			}

			List<String> rowErrors = new ArrayList<>();

			// Set user type specific properties
			configureUserTypeProperties(dto, userType, academyIds);

			try {
				// Format phone number
				formatPhoneNumber(dto);

				// Common validations
				validateExcelDuplicates(dto, row.getRowNumber(), phoneNumberToRows, emailToRows, rowErrors, parsedRows,
						edit);
				validateAgainstDatabase(dto, row.getRowNumber(), academyId, rowErrors, userType, edit);
				validateRole(dto, rowErrors);

				// Standard field validation
				List<String> validationErrors = ExcelValidator.validateUserProfile(dto, row.getRowNumber());
				if (validationErrors != null) {
					rowErrors.addAll(validationErrors);
				}

				if (!rowErrors.isEmpty()) {
					allErrors.add(new RowError(row.getRowNumber(), rowErrors));
				} else {
					validRows.add(new ValidatedRow(row.getRowNumber(), dto));
				}
			} catch (Exception e) {
				log.error("Error validating row {}: {}", row.getRowNumber(), e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(),
						List.of(createGeneralError("VALIDATION_ERROR", "Validation failed: " + e.getMessage()))));
			}
		}

		log.info("{} validation completed. Valid rows: {}, Error rows: {}", userType.name(), validRows.size(),
				allErrors.size());
	}

	/**
	 * Configure DTO properties based on user type
	 */
	public void configureUserTypeProperties(UserProfileDto dto, UserType userType, List<String> academyIds) {
		dto.setUserType(userType);
		dto.setAcademyId(academyIds);

		if (dto.getRole() == null) {
			dto.setRole(userType == UserType.COACH ? Role.COACH : Role.PLAYER);
		}
	}

	/**
	 * Format phone number consistently
	 */
	public void formatPhoneNumber(UserProfileDto dto) {
		String phoneNumber = dto.getPhoneNumber();
		if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
			String formattedPhone = formatPhoneNumber(phoneNumber);
			dto.setPhoneNumber(formattedPhone);
			dto.setUsername(formattedPhone);
		}
	}

	/**
	 * Generic processing of valid rows based on user type with save control
	 *
	 * @param edit
	 */
	public int processValidRows(List<ValidatedRow> validRows, List<RowError> allErrors, String academyId,
			UserType userType, boolean saveData, boolean edit) {
		int success = 0;
		List<String> userIdsToMap = new ArrayList<>();
		Map<String, UserProfileDto> userIdToDtoMap = new HashMap<>();

		log.info("Processing {} valid rows. Save mode: {}, Edit mode: {}", validRows.size(), saveData, edit);

		for (ValidatedRow row : validRows) {
			UserProfileDto dto = row.getUserDto();
			try {
				// FIX: Check for non-null and non-empty ID
				if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
					// If an ID is provided, always treat it as an update
					int result = handleEditUser(dto, academyId, row.getRowNumber(), allErrors, userIdsToMap,
							userIdToDtoMap, userType, saveData);
					success += result;
				} else {
					// FIX: Explicitly prevent new user creation in edit mode
					if (edit) {
						allErrors.add(new RowError(row.getRowNumber(),
								List.of(createGeneralError("MISSING_ID", "User ID is required for edit operations"))));
						continue;
					}

					// Existing logic for create mode
					List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(dto.getPhoneNumber(),
							false);

					UserProfile matchingUser = null;
					for (UserProfile existingUser : existingUsers) {
						String existingName = Optional.ofNullable(existingUser.getDisplayName()).orElse("").trim();
						String newName = Optional.ofNullable(dto.getDisplayName()).orElse("").trim();

						if (formatNameForComparison(existingName).equals(formatNameForComparison(newName))) {
							matchingUser = existingUser;
							break;
						}
					}

					if (matchingUser != null) {
						int result = handleExistingUser(matchingUser, dto, academyId, row.getRowNumber(), allErrors,
								userIdsToMap, userIdToDtoMap, userType, saveData, edit);
						success += result;
					} else {
						int result = handleNewUser(dto, academyId, row.getRowNumber(), allErrors, userIdsToMap,
								userIdToDtoMap, userType, saveData, edit);
						success += result;
					}
				}
			} catch (Exception e) {
				log.error("Error processing {} row {}: {}", userType.name().toLowerCase(), row.getRowNumber(),
						e.getMessage(), e);
				allErrors.add(new RowError(row.getRowNumber(),
						List.of(createGeneralError("PROCESSING_ERROR", "Unexpected error: " + e.getMessage()))));
			}
		}

		// Map all collected users to academy at the end (only if save is enabled and
		// there are users to map)
		if (saveData && !userIdsToMap.isEmpty()) {
			try {
				mapUsersToAcademy(userIdsToMap, userIdToDtoMap, academyId, userType, edit);
				log.info("Successfully mapped {} {}s to academy {}", userIdsToMap.size(), userType.name().toLowerCase(),
						academyId);
			} catch (Exception e) {
				log.error("Failed to map {}s to academy: {}", userType.name().toLowerCase(), e.getMessage(), e);
				handleBatchMappingFailure(allErrors, userIdsToMap, e.getMessage());
			}
		} else if (!saveData && !userIdsToMap.isEmpty()) {
			log.info("Save disabled - {} users would be mapped to academy {}", userIdsToMap.size(), academyId);
		}

		return success;
	}

	/**
	 * Handle editing an existing user
	 */
	public int handleEditUser(UserProfileDto dto, String academyId, int rowNumber, List<RowError> allErrors,
			List<String> userIdsToMap, Map<String, UserProfileDto> userIdToDtoMap, UserType userType,
			boolean saveData) {
		try {
			String userId = dto.getId();

			// Verify the user exists
			Optional<UserProfile> existingUserOpt = userProfileRepo.findById(userId);
			if (existingUserOpt.isEmpty()) {
				allErrors.add(new RowError(rowNumber, List.of(
						createGeneralError("USER_NOT_FOUND", "User with ID " + userId + " not found for editing"))));
				return 0;
			}

			UserProfile existingUser = existingUserOpt.get();

			if (saveData) {
				// Update the existing user
				ServiceResponse response = bulkUserService.update(dto);
				if (response.getHttpStatus().is2xxSuccessful()) {
					// Check if user needs to be mapped to academy (in case academy mapping is new)
					boolean isAlreadyMapped = isUserMappedToAcademy(userId, academyId, userType);
					if (!isAlreadyMapped) {
						userIdsToMap.add(userId);
						userIdToDtoMap.put(userId, dto);
					}
					log.info("Updated existing {} {} for academy {} (Save: {})", userType.name().toLowerCase(), userId,
							academyId, saveData);
					return 1;
				} else {
					allErrors.add(new RowError(rowNumber,
							List.of(createGeneralError("USER_UPDATE_ERROR", response.getMessage()))));
					return 0;
				}
			} else {
				// Simulate update (validation passed, would update if save was enabled)
				log.info("Would update {} {} for row {} (Save disabled)", userType.name().toLowerCase(), userId,
						rowNumber);
				return 1; // Count as success for validation purposes
			}
		} catch (Exception e) {
			log.error("Error handling edit {} at row {}: {}", userType.name().toLowerCase(), rowNumber, e.getMessage(),
					e);
			allErrors.add(new RowError(rowNumber, List.of(createGeneralError("EDIT_USER_ERROR",
					"Error updating " + userType.name().toLowerCase() + ": " + e.getMessage()))));
			return 0;
		}
	}

	/**
	 * Handle existing user based on user type with save control Note: This method
	 * is only called when phone AND name match (same user)
	 */
	public int handleExistingUser(UserProfile existingUser, UserProfileDto dto, String academyId, int rowNumber,
			List<RowError> allErrors, List<String> userIdsToMap, Map<String, UserProfileDto> userIdToDtoMap,
			UserType userType, boolean saveData, boolean edit) {
		try {
			// In edit mode, if this is not the user being edited, treat as duplicate
			if (edit && dto.getId() != null && !dto.getId().equals(existingUser.getId())) {
				String existingName = Optional.ofNullable(existingUser.getDisplayName()).orElse("").trim();
				allErrors.add(new RowError(rowNumber,
						List.of(createGeneralError("DUPLICATE_USER_IN_EDIT", "Cannot edit: Another user " + existingName
								+ " already exists with the same phone and name"))));
				return 0;
			}

			// Check if already mapped to this academy
			boolean isAlreadyMapped = isUserMappedToAcademy(existingUser.getId(), academyId, userType);
			if (isAlreadyMapped && !edit) {
				String existingName = Optional.ofNullable(existingUser.getDisplayName()).orElse("").trim();
				allErrors.add(new RowError(rowNumber, List.of(createGeneralError("ALREADY_MAPPED",
						userType.name() + " " + existingName + " is already mapped to this academy"))));
				return 0; // Don't add to success count and don't add to userIdsToMap
			}

			// Only add to mapping list if not already mapped or if in edit mode
			if (!isAlreadyMapped || edit) {
				userIdsToMap.add(existingUser.getId());
				userIdToDtoMap.put(existingUser.getId(), dto);
			}

			log.info("Found existing {} {} to map to academy {} (Save: {}, Edit: {})", userType.name().toLowerCase(),
					existingUser.getId(), academyId, saveData, edit);
			return 1;

		} catch (Exception e) {
			log.error("Error handling existing {} at row {}: {}", userType.name().toLowerCase(), rowNumber,
					e.getMessage(), e);
			allErrors.add(new RowError(rowNumber, List.of(createGeneralError("EXISTING_USER_ERROR",
					"Error processing existing " + userType.name().toLowerCase() + ": " + e.getMessage()))));
			return 0;
		}
	}

	/**
	 * Handle new user creation based on user type with save control
	 */
	public int handleNewUser(UserProfileDto dto, String academyId, int rowNumber, List<RowError> allErrors,
			List<String> userIdsToMap, Map<String, UserProfileDto> userIdToDtoMap, UserType userType, boolean saveData,
			boolean edit) {
		try {
			// In edit mode, we shouldn't be creating new users
			if (edit) {
				allErrors.add(new RowError(rowNumber, List.of(createGeneralError("INVALID_EDIT_OPERATION",
						"Cannot create new user in edit mode. User ID is required for editing."))));
				return 0;
			}

			if (saveData) {
				// Create new user when save is enabled
				ServiceResponse response = bulkUserService.create(dto);
				if (response.getHttpStatus().is2xxSuccessful()) {
					UserProfile newUser = (UserProfile) response.getBody();
					userIdsToMap.add(newUser.getId());
					userIdToDtoMap.put(newUser.getId(), dto);
					log.info("Created new {} {} to map to academy {}", userType.name().toLowerCase(), newUser.getId(),
							academyId);
					return 1;
				} else {
					allErrors.add(new RowError(rowNumber,
							List.of(createGeneralError("NEW_USER_ERROR", response.getMessage()))));
					return 0;
				}
			} else {
				// Simulate creation (validation passed, would create if save was enabled)
				log.info("Would create new {} for row {} (Save disabled)", userType.name().toLowerCase(), rowNumber);
				return 1; // Count as success for validation purposes
			}
		} catch (Exception e) {
			log.error("Error handling new {}: {}", userType.name().toLowerCase(), e.getMessage(), e);
			allErrors.add(new RowError(rowNumber, List.of(createGeneralError("NEW_USER_ERROR",
					"Error creating new " + userType.name().toLowerCase() + ": " + e.getMessage()))));
			return 0;
		}
	}

	/**
	 * Map users to academy based on user type
	 */
	public void mapUsersToAcademy(List<String> userIds, Map<String, UserProfileDto> userIdToDtoMap, String academyId,
			UserType userType, boolean edit) throws Exception {
		switch (userType) {
		case PLAYER -> traineeService.addTraineesToAcademy(userIds, academyId);
		case COACH -> {
			// Implement coach mapping to academy
			for (String userId : userIds) {
				UserProfile coach = userProfileRepo.findById(userId)
						.orElseThrow(() -> new RuntimeException("Coach not found: " + userId));
				UserProfileDto dto = userIdToDtoMap.get(userId);
				createCoachAcademyMapping(coach, dto, academyId, edit);
			}
			log.info("Successfully mapped {} coaches to academy {} (Edit: {})", userIds.size(), academyId, edit);
		}
		default -> throw new IllegalArgumentException("Unsupported user type for mapping: " + userType);
		}
	}

	// Updated method to handle edit mode
	public void createCoachAcademyMapping(UserProfile coach, UserProfileDto dto, String academyId, boolean edit) {
		// Check if mapping already exists for this coach and academy
		Optional<CoachAcademyMapping> existingMapping = coachAcademyMappingRepo
				.findByAcademy_IdAndCoachUserProfile_Id(academyId, coach.getId());

		if (existingMapping.isPresent()) {
			log.info("Coach-Academy mapping already exists for coach ID: {} and academy ID: {}", coach.getId(),
					academyId);

			// Update existing mapping with new data (always update in edit mode)
			CoachAcademyMapping mapping = existingMapping.get();
			mapping.setRoleId(dto.getRoleId());
			mapping.setDesignation(dto.getDesignation());
			mapping.setExperienceInMonths(dto.getExperienceInMonths());
			mapping.setStatus(Status.ACTIVE);
			mapping.setUpdatedOn(Timestamp.from(Instant.now())); // Add updated timestamp

			coachAcademyMappingRepo.save(mapping);
			log.info("Updated existing Coach-Academy mapping with ID: {} (Edit: {})", mapping.getId(), edit);
			return;
		}

		// Create new mapping if it doesn't exist
		CoachAcademyMapping mapping = new CoachAcademyMapping();
		mapping.setId(UUID.randomUUID().toString());
		mapping.setCoachUserProfile(coach);

		Academy academy = academyRepo.findById(academyId)
				.orElseThrow(() -> new ResourceNotFoundException("Academy not found with ID: " + academyId));
		mapping.setAcademy(academy);
		mapping.setCreatedOn(Timestamp.from(Instant.now()));

		mapping.setRoleId(dto.getRoleId());
		mapping.setDesignation(dto.getDesignation());
		mapping.setExperienceInMonths(dto.getExperienceInMonths());
		mapping.setStatus(Status.ACTIVE);

		coachAcademyMappingRepo.save(mapping);
		log.info("Created new Coach-Academy mapping with ID: {} for coach: {} and academy: {} (Edit: {})",
				mapping.getId(), coach.getId(), academyId, edit);
	}

	/**
	 * Generic database validation that adapts based on user type
	 *
	 * @param edit
	 */
	public void validateAgainstDatabase(UserProfileDto dto, int rowNumber, String academyId, List<String> rowErrors,
			UserType userType, boolean edit) {
		try {
			String userId = dto.getId();
			String phoneNumber = dto.getPhoneNumber();
			String email = dto.getEmailId();
			String name = dto.getDisplayName();
			Role role = dto.getRole();

			// If an ID is provided, check if the user exists.
			if (userId != null && !userId.trim().isEmpty()) {
				if (!userProfileRepo.existsById(userId)) {
					rowErrors.add(createGeneralError("USER_NOT_FOUND",
							"User with ID '" + userId + "' not found for editing."));
					return; // Stop validation if user doesn't exist
				}
			}

			if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
				validatePhoneNumberAgainstDatabase(dto, phoneNumber, name, role, academyId, rowErrors, userType, edit);
			}

			if (email != null && !email.trim().isEmpty()) {
				validateEmailAgainstDatabase(dto, email, phoneNumber, rowErrors, edit);
			}
		} catch (Exception e) {
			log.error("Database validation error for {} row {}: {}", userType.name().toLowerCase(), rowNumber,
					e.getMessage(), e);
			rowErrors.add(createGeneralError("DB_VALIDATION_ERROR", "Database validation failed: " + e.getMessage()));
		}
	}

	/**
	 * Validates that a phone number and name combination is unique, allowing
	 * multiple users with the same number but different names.
	 */
	public void validatePhoneNumberAgainstDatabase(UserProfileDto dto, String phoneNumber, String name, Role role,
			String academyId, List<String> rowErrors, UserType userType, boolean edit) {
		try {
			List<UserProfile> existingUsersWithPhone = userProfileRepo.findByPhoneNumberAndInactive(phoneNumber, false);

			for (UserProfile existingUser : existingUsersWithPhone) {
				String existingName = existingUser.getDisplayName();

				if (formatNameForComparison(name).equals(formatNameForComparison(existingName))) {
					// Same user (matching phone and name)
					if (existingUser.getUserType() != userType) {
						rowErrors.add(createFieldError("Phone Number", "TYPE_MISMATCH",
								"User with phone '" + phoneNumber + "' exists as " + existingUser.getUserType()
										+ ", cannot upload as " + userType));
						continue;
					}

					// Check if already mapped to this academy
					boolean isAlreadyMapped = isUserMappedToAcademy(existingUser.getId(), academyId, userType);
					if (isAlreadyMapped && !edit) {
						rowErrors.add(createFieldError("Phone Number", "ALREADY_MAPPED", userType.name() + " '" + name
								+ "' with phone '" + phoneNumber + "' is already mapped to this academy"));
					}
				}

				// else if (userType == UserType.PLAYER) {
				// // For players, be more strict about phone-name mismatches
				// rowErrors.add(createFieldError("Phone Number", "PHONE_NAME_MISMATCH",
				// "Phone number '" + phoneNumber + "' is already registered with a different
				// name ('"
				// + existingName + "'). Please verify the details."));
				// }
			}
		} catch (Exception e) {
			log.error("Error validating phone against database: {}", e.getMessage(), e);
			rowErrors.add(createGeneralError("DB_PHONE_CHECK_ERROR", "Database phone validation failed"));
		}
	}

	/**
	 * Check if user is already mapped to academy based on user type
	 */
	public boolean isUserMappedToAcademy(String userId, String academyId, UserType userType) {
		return switch (userType) {
		case COACH -> coachAcademyMappingRepo.existsByCoachUserProfileIdAndAcademyId(userId, academyId);
		case PLAYER -> traineeAcademyMappingRepo.existsByTraineeUserProfileIdAndAcademyId(userId, academyId);
		default -> false;
		};
	}

	public void buildDuplicateDetectionMaps(List<ParsedRow> parsedRows, Map<String, List<Integer>> phoneNumberToRows,
			Map<String, List<Integer>> emailToRows) {
		for (ParsedRow row : parsedRows) {
			if (row == null || row.hasParsingErrors()) {
				continue;
			}

			UserProfileDto dto = row.getUserDto();
			if (dto == null) {
				continue;
			}

			String phoneNumber = dto.getPhoneNumber();
			if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
				phoneNumberToRows.computeIfAbsent(phoneNumber, k -> new ArrayList<>()).add(row.getRowNumber());
			}

			String email = dto.getEmailId();
			if (email != null && !email.trim().isEmpty()) {
				emailToRows.computeIfAbsent(email.toLowerCase(), k -> new ArrayList<>()).add(row.getRowNumber());
			}
		}
	}

	public void validateExcelDuplicates(UserProfileDto dto, int rowNumber, Map<String, List<Integer>> phoneNumberToRows,
			Map<String, List<Integer>> emailToRows, List<String> rowErrors, List<ParsedRow> parsedRows, boolean edit) {
		String phoneNumber = dto.getPhoneNumber();
		String email = dto.getEmailId();
		String userId = dto.getId();

		if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
			List<Integer> rowsWithSamePhone = phoneNumberToRows.get(phoneNumber);
			if (rowsWithSamePhone != null && rowsWithSamePhone.size() > 1) {
				validatePhoneDuplicatesWithNames(dto, rowNumber, rowsWithSamePhone, parsedRows, rowErrors, edit);
			}
		}

		if (email != null && !email.trim().isEmpty()) {
			List<Integer> rowsWithSameEmail = emailToRows.get(email.toLowerCase());
			if (rowsWithSameEmail != null && rowsWithSameEmail.size() > 1) {
				validateEmailDuplicates(dto, rowNumber, rowsWithSameEmail, parsedRows, rowErrors, edit);
			}
		}
	}

	public void validatePhoneDuplicatesWithNames(UserProfileDto dto, int rowNumber, List<Integer> rowsWithSamePhone,
			List<ParsedRow> parsedRows, List<String> rowErrors, boolean edit) {
		String formattedCurrentName = formatNameForComparison(dto.getDisplayName());
		String phoneNumber = dto.getPhoneNumber();
		String currentUserId = dto.getId();

		for (ParsedRow otherRow : parsedRows) {
			if (otherRow != null && !otherRow.hasParsingErrors() && otherRow.getRowNumber() != rowNumber
					&& rowsWithSamePhone.contains(otherRow.getRowNumber())) {

				UserProfileDto otherDto = otherRow.getUserDto();
				if (otherDto != null) {
					// Skip validation if editing and both records have the same user ID
					if (edit && currentUserId != null && currentUserId.equals(otherDto.getId())) {
						continue;
					}

					String otherFormattedName = formatNameForComparison(otherDto.getDisplayName());
					if (formattedCurrentName.equals(otherFormattedName)) {
						String errorMessage = edit ? "Same person '" + dto.getDisplayName() + "' with phone '"
								+ phoneNumber
								+ "' found in multiple rows. When editing, same person cannot appear multiple times unless it's the same user being updated."
								: "Same person '" + dto.getDisplayName() + "' with phone '" + phoneNumber
										+ "' found. Same person cannot be entered multiple times.";

						rowErrors.add(createFieldError("Phone Number", "DUPLICATE_PERSON", errorMessage));
						break;
					}
				}
			}
		}
	}

	public void validateEmailDuplicates(UserProfileDto dto, int rowNumber, List<Integer> rowsWithSameEmail,
			List<ParsedRow> parsedRows, List<String> rowErrors, boolean edit) {
		String email = dto.getEmailId();
		String currentPhone = dto.getPhoneNumber();
		String currentUserId = dto.getId();

		for (int otherRowNum : rowsWithSameEmail) {
			if (otherRowNum != rowNumber) {
				ParsedRow otherRow = parsedRows.stream().filter(r -> r != null && r.getRowNumber() == otherRowNum)
						.findFirst().orElse(null);

				if (otherRow != null && otherRow.getUserDto() != null) {
					UserProfileDto otherDto = otherRow.getUserDto();
					String otherPhone = otherDto.getPhoneNumber();
					String otherUserId = otherDto.getId();

					// Skip validation if editing and both records have the same user ID
					if (edit && currentUserId != null && currentUserId.equals(otherUserId)) {
						continue;
					}

					if (!currentPhone.equals(otherPhone)) {
						String errorMessage = edit ? "Email '" + email
								+ "' is used with different phone numbers in Excel. "
								+ "When editing, same email can be used for same phone number but not for different phone numbers unless it's the same user being updated."
								: "Email '" + email + "' is used with different phone numbers in Excel. "
										+ "Same email can be used for same phone number but not for different phone numbers.";

						rowErrors.add(createFieldError("Email", "DUPLICATE_WITH_DIFFERENT_PHONE", errorMessage));
						break;
					}
				}
			}
		}
	}

	public void validateEmailAgainstDatabase(UserProfileDto dto, String email, String phoneNumber,
			List<String> rowErrors, boolean edit) {
		try {
			List<UserProfile> existingUsersWithEmail = userProfileRepo.findByEmailIdAndInactive(email, false);

			if (!existingUsersWithEmail.isEmpty()) {
				String currentUserId = dto.getId();

				for (UserProfile existingUser : existingUsersWithEmail) {
					// Skip validation if editing and it's the same user
					if (edit && currentUserId != null && currentUserId.equals(existingUser.getId())) {
						continue;
					}

					String existingPhone = existingUser.getPhoneNumber();
					if (!phoneNumber.equals(existingPhone)) {
						String errorMessage = edit ? "Email '" + email
								+ "' already exists for a different phone number (" + existingPhone
								+ ") in the database. When editing, same email cannot be used with different phone numbers unless it's the same user being updated."
								: "Email '" + email + "' already exists for a different phone number (" + existingPhone
										+ "). Same email cannot be used with different phone numbers.";

						rowErrors.add(createFieldError("Email", "EMAIL_PHONE_MISMATCH", errorMessage));
					}
				}
			}
		} catch (Exception e) {
			log.error("Error validating email against database: {}", e.getMessage(), e);
			rowErrors.add(createGeneralError("DB_EMAIL_CHECK_ERROR", "Database email validation failed"));
		}
	}

	public void validateRole(UserProfileDto dto, List<String> errors) {
		if (dto.getRole() == null) {
			errors.add(createFieldError("Role", "REQUIRED", "Role cannot be null"));
			return;
		}

		try {
			List<Roles> roles = rolesRepo.findByRoleName(dto.getRole().name());
			if (roles.isEmpty()) {
				errors.add(createFieldError("Role", "INVALID_VALUE", "Invalid role: " + dto.getRole().name()));
			} else {
				dto.setRoleId(roles.get(0).getId());
			}
		} catch (Exception e) {
			log.error("Error validating role: {}", e.getMessage(), e);
			errors.add(createGeneralError("ROLE_VALIDATION_ERROR", "Role validation failed"));
		}
	}

	public String formatPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}
		String digitsOnly = phoneNumber.replaceAll("\\D", "");
		return digitsOnly;
	}

	public void handleBatchMappingFailure(List<RowError> allErrors, List<String> userIds, String errorMessage) {
		for (int i = 0; i < userIds.size(); i++) {
			allErrors.add(new RowError(i + 1, List.of(createGeneralError("MAPPING_ERROR",
					"User created but failed to map to academy: " + errorMessage))));
		}
	}

	public String formatNameForComparison(String name) {
		if (name == null)
			return "";
		return name.trim().toLowerCase().replaceAll("\\s+", " ");
	}

	public ServiceResponse generateEnhancedResponse(List<RowError> allErrors, int success, BulkType type,
			List<ParsedRow> parsedRows, boolean saveData, List<CoursePaymentOptionDto> paymentOptions,
			String academyId) {

		try {
			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("success", allErrors.isEmpty());
			responseBody.put("successCount", success);
			responseBody.put("errorCount", allErrors.size());
			responseBody.put("totalProcessed", parsedRows.size());
			responseBody.put("timestamp", Instant.now().toString());

			// Create safe copies of data to avoid circular references
			responseBody.put("fileContent", parsedRows);

			if (type.equals(BulkType.PLAYER_ENROLLEMENTS)) {
				responseBody.put("paymentOptions", paymentOptions);
			} else if (type.equals(BulkType.PROGRAMS)) {
				responseBody.put("currency", Currency.values());

				// Get academy-specific sports if academyId is provided
				List<Sports> sportsList;
				if (academyId != null && !academyId.isBlank()) {
					ServiceResponse response = miscellaneousService.getSportsByAcademy(academyId);
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
					// No academyId provided, use all sports
					ServiceResponse response = miscellaneousService.getAllSports();
					sportsList = (List<Sports>) response.getBody();
				}

				responseBody.put("sports", sportsList);
				responseBody.put("visibility", Visibility.values());
				responseBody.put("skillLevels", SkillLevel.values());
				responseBody.put("ageCategory", AgeCategory.values());
				responseBody.put("scheduleType", ScheduleType.values());
				responseBody.put("paymentSchedules", PaymentSchedule.values());

			} else if (type.equals(BulkType.COACHES) || type.equals(BulkType.PLAYERS)) {
				responseBody.put("gender", Gender.values());
				if (type.equals(BulkType.COACHES)) {
					responseBody.put("roles", getAllowedRoles());
				}
			}

			if (!allErrors.isEmpty()) {
				// Format all errors with structured field information
				List<FormattedErrorEntry> formattedErrors = formatAllErrorsForDisplay(allErrors, parsedRows);

				// Create error report entries for Excel generation
				List<ErrorReportEntry> reportEntries = compileErrorReportEntries(parsedRows, allErrors);

				// Include both formatted errors and original data
				responseBody.put("errors", formattedErrors);
				responseBody.put("rawErrors", reportEntries);

				return ResponseBuilder.success(responseBody, ApiResponse.PARTIAL_UPLOAD, HttpStatus.ACCEPTED);
			}

			return ResponseBuilder.success(responseBody, ApiResponse.PARTIAL_UPLOAD, HttpStatus.ACCEPTED);

		} catch (StackOverflowError e) {
			log.error("StackOverflowError in generateEnhancedResponse - circular reference detected", e);
			return createSimpleErrorResponse("Data processing error - circular reference detected",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Error generating response: {}", e.getMessage(), e);
			return createErrorResponse("Error generating response: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Helper methods to create safe copies and avoid circular references
	public List<Map<String, String>> createSafeGenderList() {
		try {
			return Arrays.stream(Gender.values()).map(gender -> {
				Map<String, String> genderMap = new HashMap<>();
				genderMap.put("name", gender.name());
				genderMap.put("value", gender.toString());
				return genderMap;
			}).collect(Collectors.toList());
		} catch (Exception e) {
			log.warn("Error creating safe gender list: {}", e.getMessage());
			return new ArrayList<>();
		}
	}

	public Map<String, Object> convertPaymentOptionToSafeMap(CoursePaymentOptionDto option) {
		if (option == null)
			return new HashMap<>();

		Map<String, Object> safeOption = new HashMap<>();
		try {
			safeOption.put("id", option.getId());
			safeOption.put("name", option.getPaymentSchedule());
			safeOption.put("amount", option.getPaymentAmount());
			safeOption.put("currency", option.getCurrency());
			// Add other primitive fields as needed, avoid complex nested objects
		} catch (Exception e) {
			log.warn("Error converting payment option to safe map: {}", e.getMessage());
		}
		return safeOption;
	}

	public ServiceResponse createSimpleErrorResponse(String message, HttpStatus status) {
		Map<String, Object> simpleResponse = new HashMap<>();
		simpleResponse.put("success", false);
		simpleResponse.put("message", message);
		simpleResponse.put("timestamp", Instant.now().toString());
		return ResponseBuilder.error(message, status);
	}

	public static Enum<?>[] getAllowedRoles() {
		return Arrays.stream(Role.values()).filter(role -> role != Role.USER && role != Role.SUPER_ADMIN
				&& role != Role.ACADEMY_OWNER && role != Role.PLAYER).toArray(Role[]::new);
	}

	// Updated compileErrorReportEntries method for better error formatting
	public List<ErrorReportEntry> compileErrorReportEntries(List<ParsedRow> parsedRows, List<RowError> allErrors) {
		try {
			Map<Integer, List<String>> errorsByRow = allErrors.stream()
					.collect(Collectors.groupingBy(RowError::getRowNumber,
							Collectors.flatMapping(e -> e.getMessages().stream(), Collectors.toList())));

			return parsedRows.stream().filter(row -> errorsByRow.containsKey(row.getRowNumber())).map(row -> {
				List<String> formattedMessages = errorsByRow.get(row.getRowNumber()).stream()
						.map(this::formatErrorForExcel).collect(Collectors.toList());

				return new ErrorReportEntry(row.getRowNumber(), row.getOriginalData(), formattedMessages);
			}).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error compiling error report entries: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	// Format error message for Excel report (human-readable)
	public String formatErrorForExcel(String errorMessage) {
		if (errorMessage.startsWith("FIELD:")) {
			String[] parts = errorMessage.split(":", 4);
			if (parts.length >= 4) {
				return parts[1] + ": " + parts[3]; // "Field Name: Error Message"
			}
		} else if (errorMessage.startsWith("GENERAL:")) {
			String[] parts = errorMessage.split(":", 3);
			if (parts.length >= 3) {
				return parts[2]; // Just the error message
			}
		}
		return errorMessage; // Return as-is if not structured
	}

	// New method to format all errors for proper display
	public List<FormattedErrorEntry> formatAllErrorsForDisplay(List<RowError> allErrors, List<ParsedRow> parsedRows) {
		List<FormattedErrorEntry> formattedErrors = new ArrayList<>();

		for (RowError rowError : allErrors) {
			int rowNumber = rowError.getRowNumber();

			// Get original data for this row
			ParsedRow originalRow = parsedRows.stream().filter(row -> row.getRowNumber() == rowNumber).findFirst()
					.orElse(null);

			List<String> originalData = originalRow != null ? originalRow.getOriginalData() : new ArrayList<>();

			// Format each error message for this row
			List<FormattedError> rowFormattedErrors = new ArrayList<>();

			for (String errorMessage : rowError.getMessages()) {
				FormattedError formattedError = parseAndFormatError(errorMessage);
				rowFormattedErrors.add(formattedError);
			}

			formattedErrors.add(new FormattedErrorEntry(rowNumber, originalData, rowFormattedErrors));
		}

		return formattedErrors;
	}

	// Method to parse error string and create structured error object
	public FormattedError parseAndFormatError(String errorMessage) {
		if (errorMessage.startsWith("FIELD:")) {
			// Parse field-specific error: "FIELD:FieldName:ErrorCode:Message"
			String[] parts = errorMessage.split(":", 4);
			if (parts.length >= 4) {
				return new FormattedError("FIELD", parts[1], // Field name
						parts[2], // Error code
						parts[3], // Error message
						true // Is field specific
				);
			}
		} else if (errorMessage.startsWith("GENERAL:")) {
			// Parse general error: "GENERAL:ErrorCode:Message"
			String[] parts = errorMessage.split(":", 3);
			if (parts.length >= 3) {
				return new FormattedError("GENERAL", null, // No specific field
						parts[1], // Error code
						parts[2], // Error message
						false // Not field specific
				);
			}
		}

		// Fallback for unstructured errors
		return new FormattedError("GENERAL", null, "UNKNOWN_ERROR", errorMessage, false);
	}

	// Data classes for structured error response
	public static class FormattedErrorEntry {
		public int rowNumber;
		public List<String> originalData;
		public List<FormattedError> errors;

		public FormattedErrorEntry(int rowNumber, List<String> originalData, List<FormattedError> errors) {
			this.rowNumber = rowNumber;
			this.originalData = originalData;
			this.errors = errors;
		}

		// Getters
		public int getRowNumber() {
			return rowNumber;
		}

		public List<String> getOriginalData() {
			return originalData;
		}

		public List<FormattedError> getErrors() {
			return errors;
		}
	}

	public static class FormattedError {
		public String errorType; // "FIELD" or "GENERAL"
		public String fieldName; // Name of the field (null for general errors)
		public String errorCode; // Error code (REQUIRED, INVALID_FORMAT, etc.)
		public String message; // Human readable error message
		public boolean isFieldSpecific; // Whether this error is field-specific

		public FormattedError(String errorType, String fieldName, String errorCode, String message,
				boolean isFieldSpecific) {
			this.errorType = errorType;
			this.fieldName = fieldName;
			this.errorCode = errorCode;
			this.message = message;
			this.isFieldSpecific = isFieldSpecific;
		}

		// Getters
		public String getErrorType() {
			return errorType;
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public String getMessage() {
			return message;
		}

		public boolean isFieldSpecific() {
			return isFieldSpecific;
		}
	}

	public static class ValidatedRow {
		public final int rowNumber;
		public final UserProfileDto userDto;

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

	public static class ValidatedProgramRow {
		private final int rowNumber;
		private final CreateCourseDto courseDto;
		private final List<String> originalData;

		public ValidatedProgramRow(int rowNumber, CreateCourseDto dto, List<String> originalData) {
			this.rowNumber = rowNumber;
			this.courseDto = dto;
			this.originalData = originalData;
		}

		public int getRowNumber() {
			return rowNumber;
		}

		public CreateCourseDto getCreateCourseDto() {
			return courseDto;
		}

		public List<String> getOriginalData() {
			return originalData;
		}
	}

	public static class ValidatedProgramPlayerEnrollmentRow {
		public final int rowNumber;
		public final ProgramPlayerEnrollmentDto enrollmentDto;

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

}
