package com.playmotech.api.core.services.impl;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.modelmapper.internal.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.PaymentLedger;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserAuthDetails;
import com.playmotech.api.core.dao_postgres.UserDocuments;
import com.playmotech.api.core.dao_postgres.UserPreferredSportsMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UsersActionsMapping;
import com.playmotech.api.core.dto.CoachAcademyDetails;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.EnrollTraineeInCourseDto;
import com.playmotech.api.core.dto.InstallmentInfo;
import com.playmotech.api.core.dto.PlayerEnrollInCourseDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserAddEditDto;
import com.playmotech.api.core.dto.UserDocumentsDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.PaymentLedgerHelper;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.UserMapper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.AttendanceViewRepository;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CoachPerformanceViewRepository;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.DuePaymentsViewRepository;
import com.playmotech.api.core.repo.PaymentLedgerRepository;
import com.playmotech.api.core.repo.PaymentViewRepository;
import com.playmotech.api.core.repo.PendingPaymentDueViewRepository;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.repo.TraineePerformanceViewRepository;
import com.playmotech.api.core.repo.UserAuthDetailsRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.repo.UsersActionsMappingRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.UserAddEditDao;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.PlayerService;
import com.playmotech.api.core.utils.InstallmentUtil;
import com.playmotech.api.core.utils.PasswordGenerator;
import com.playmotech.api.core.views.AttendanceView;
import com.playmotech.api.core.views.CoachPerformanceReportView;
import com.playmotech.api.core.views.DuePaymentsView;
import com.playmotech.api.core.views.PaymentDetailsView;
import com.playmotech.api.core.views.TraineePerformanceReportView;

import jakarta.transaction.Transactional;
// Lombok imports
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

	private final UsersActionsMappingRepo usersActionsMappingRepository;
	private final CoachAcademyMappingRepo coachAcademyMappingRepository;
	private final AcademyRepo academyRepository;
	private final UserProfileRepo userProfileRepository;
	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;

	private final PendingPaymentDueViewRepository paymentViewRepository;

	private final AttendanceViewRepository attendanceViewRepository;

	private final TraineePerformanceViewRepository performanceViewRepository;

	private final PaymentViewRepository paymentRepository;
	private final CoachPerformanceViewRepository coachPerformanceViewRepository;

	private final UserAuthDetailsRepo userAuthDetailsRepo;

	private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;

	private final PaymentLedgerRepository ledgerRepository;

	private final CourseRepo courseRepo;

	private final ICourseService courseService;

	private final ITraineeService traineeService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private final DuePaymentsViewRepository duePaymentsViewRepository;

	private final UserProfileHelper validationHelper;

	private final InstallmentUtil installmentUtil;
	private final PaymentLedgerHelper ledgerHelper;

	@Value("${default.icons.male}")
	private String defaultProfilePictureUrlMale;

	@Value("${default.icons.female}")
	private String defaultProfilePictureUrlFemale;

	@Value("${storage.users-media-bucket}")
	private String usersMediaBucket;

	@Value("${users-media-base-url}")
	private String usersMediaBaseUrl;

	@Value("${otp-bypass}")
	private Boolean otpByPass;

	@Value("${otp-bypass-username}")
	private List<String> otpByPassUsernames;

	@Value("${otp-bypass-email}")
	private List<String> otpByPassEmails;

	@Override
	public ServiceResponse getPlayerById(String identifier, String userId, String academyDomain) {
		log.info("Fetching player with ID: {}", identifier);

		Optional<UserProfile> optionalUser = userProfileRepository.findById(identifier);
		if (optionalUser.isEmpty()) {
			log.warn("Player not found with ID: {}", identifier);
			return ResponseBuilder.badRequest(ApiResponse.USER_NOT_FOUND);
		}

		UserProfile user = optionalUser.get();
		// Map to DTO
		UserAddEditDao playerDto = toDto(user);

		// Manually fetch academy mappings
		List<TraineeAcademyMapping> mappings = traineeAcademyMappingRepo.findByTraineeUserProfile_Id(identifier);
		List<PlayerEnrollInCourseDto> enrollments = new ArrayList<>();

		for (TraineeAcademyMapping mapping : mappings) {
			Academy academy = mapping.getAcademy();

			// Apply domain filter only if academyDomain is provided (i.e., user is not
			// super admin)
			if (academyDomain != null && !academyDomain.isEmpty()) {
				// Get organization from the academy
				Organisation org = academy.getOrg();
				// Skip if organization doesn't exist or domain doesn't match
				if (org == null || org.getDomainUrl() == null || !org.getDomainUrl().equals(academyDomain)) {
					continue;
				}
			}

			PlayerEnrollInCourseDto academyDto = new PlayerEnrollInCourseDto();
			academyDto.setId(mapping.getId());
			academyDto.setAcademyId(academy.getId());
			academyDto.setAcademyStatus(mapping.getStatus());

			// Fetch program enrollments for this academy specifically
			List<TraineeCourseEnrollment> programEnrollments = traineeCourseEnrollmentRepo
					.findByTraineeUserProfile_IdAndAcademy_Id(identifier, academy.getId());

			List<EnrollTraineeInCourseDto> programDtos = programEnrollments.stream().map(program -> {
				EnrollTraineeInCourseDto dto = new EnrollTraineeInCourseDto();
				dto.setId(program.getId());
				dto.setProgramId(program.getCourse().getId());
				dto.setProgramStatus(program.getStatus());
				dto.setTraineeUserId(identifier);
				dto.setAmount(program.getAmount());
				dto.setJoiningDate(program.getJoiningDate());
				dto.setDueDate(program.getDueDate());
				dto.setPaymentSchedule(program.getPaymentSchedule());
				return dto;
			}).collect(Collectors.toList());

			academyDto.setEnrollInPrograms(programDtos);
			enrollments.add(academyDto);
		}

		playerDto.setEnrollInAcademies(enrollments);
		return ResponseBuilder.success(playerDto, ApiResponse.LIST_FETCHED_SUCCESSFULLY);
	}

	@Override
	public ServiceResponse getPlayerKpiById(String identifier, String userId, String academyDomain) {
		log.info("Fetching player with ID: {}", identifier);

		Optional<UserProfile> optionalUser = userProfileRepository.findById(identifier);
		if (optionalUser.isEmpty()) {
			log.warn("Player not found with ID: {}", identifier);
			return ResponseBuilder.badRequest(ApiResponse.USER_NOT_FOUND);
		}

		// Fetch all payment view records for the player
		List<DuePaymentsView> paymentDetailsViews = duePaymentsViewRepository
				.findByTraineeUserId(optionalUser.get().getId());

		// Apply domain filter only if academyDomain is provided (i.e., user is not
		// super admin)
		List<DuePaymentsView> filteredPayments;
		if (academyDomain != null && !academyDomain.isEmpty()) {
			filteredPayments = paymentDetailsViews.stream()
					.filter(p -> academyDomain.equalsIgnoreCase(p.getDomainUrl())).toList();
		} else {
			filteredPayments = paymentDetailsViews; // Super admin sees all payments
		}

		List<AttendanceView> allAttendance = attendanceViewRepository.findByPlayerId(identifier);

		// Apply domain filter to attendance as well if needed
		List<AttendanceView> filteredAttendance;
		if (academyDomain != null && !academyDomain.isEmpty()) {
			// Assuming AttendanceView has a domain field - adjust as needed
			filteredAttendance = allAttendance.stream().filter(a -> academyDomain.equalsIgnoreCase(a.getDomainUrl()))
					.toList();
		} else {
			filteredAttendance = allAttendance; // Super admin sees all attendance
		}

		// Filter by domain URL for attendance status count
		Map<String, Long> attendanceStatusCount = filteredAttendance.stream().filter(a -> a.getStatus() != null)
				.collect(Collectors.groupingBy(AttendanceView::getStatus, Collectors.counting()));

		// Aggregate totals
		BigDecimal totalCourseFeeCollected = BigDecimal.ZERO;
		BigDecimal totalRegistrationFeeCollected = BigDecimal.ZERO;
		BigDecimal pendingCourseFee = BigDecimal.ZERO;
		BigDecimal pendingRegistrationFee = BigDecimal.ZERO;

		BigDecimal futurePendingCourseFee = BigDecimal.ZERO;
		BigDecimal futurePendingRegistrationFee = BigDecimal.ZERO;

		LocalDate today = LocalDate.now();

		for (DuePaymentsView payment : filteredPayments) {
			// Paid
			totalCourseFeeCollected = totalCourseFeeCollected
					.add(payment.getCourseTotalPaid() != null ? BigDecimal.valueOf(payment.getCourseTotalPaid())
							: BigDecimal.ZERO);

			totalRegistrationFeeCollected = totalRegistrationFeeCollected.add(
					payment.getRegistrationTotalPaid() != null ? BigDecimal.valueOf(payment.getRegistrationTotalPaid())
							: BigDecimal.ZERO);

			// Dues ON or BEFORE today (already due)
			if (payment.getDuesOn() != null && !payment.getDuesOn().isAfter(today)) {
				pendingCourseFee = pendingCourseFee
						.add(payment.getTotalCourseDue() != null ? BigDecimal.valueOf(payment.getTotalCourseDue())
								: BigDecimal.ZERO);

				pendingRegistrationFee = pendingRegistrationFee.add(payment.getRegistrationTotalPending() != null
						? BigDecimal.valueOf(payment.getRegistrationTotalPending())
						: BigDecimal.ZERO);
			}

			// Dues AFTER today (future dues)
			if (payment.getDuesOn() != null && payment.getDuesOn().isAfter(today)) {
				futurePendingCourseFee = futurePendingCourseFee
						.add(payment.getTotalCourseDue() != null ? BigDecimal.valueOf(payment.getTotalCourseDue())
								: BigDecimal.ZERO);

				futurePendingRegistrationFee = futurePendingRegistrationFee
						.add(payment.getRegistrationTotalPending() != null
								? BigDecimal.valueOf(payment.getRegistrationTotalPending())
								: BigDecimal.ZERO);
			}
		}

		UserAddEditDao playerDto = toDto(optionalUser.get());

		// Create response payload
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("playerDetails", playerDto);
		responseData.put("totalCourseFeeCollected", totalCourseFeeCollected);
		responseData.put("totalRegistrationFeeCollected", totalRegistrationFeeCollected);
		responseData.put("pendingCourseFee", pendingCourseFee); // Dues on or before today
		responseData.put("pendingRegistrationFee", pendingRegistrationFee); // Dues on or before today
		responseData.put("futurePendingCourseFee", futurePendingCourseFee); // Dues after today
		responseData.put("futurePendingRegistrationFee", futurePendingRegistrationFee); // Dues after today
		responseData.put("attendanceStatusCount", attendanceStatusCount);

		return ResponseBuilder.success(responseData, ApiResponse.LIST_FETCHED_SUCCESSFULLY);
	}

	public static UserAddEditDao toDto(UserProfile user) {
		UserAddEditDao dto = new UserAddEditDao();

		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setDisplayName(user.getDisplayName());
		dto.setDob(user.getDob());
		dto.setUserType(user.getUserType());
		dto.setEmailId(user.getEmailId());
		dto.setPhoneNumber(user.getPhoneNumber());
		dto.setProfilePictureUrl(user.getProfilePictureUrl());
		dto.setGender(user.getGender());
		dto.setExperienceInMonths(user.getExperienceInMonths());
		dto.setAddressLine1(user.getAddressLine1());
		dto.setAddressLine2(user.getAddressLine2());
		dto.setPincode(user.getPincode());
		dto.setCity(user.getCity());
		dto.setState(user.getState());
		dto.setCountry(user.getCountry());
		return dto;
	}

	@Override
	@Transactional
	public ServiceResponse addPlayer(UserAddEditDto userDto, String userId) {
		return addUser(userDto, userId, UserType.PLAYER);
	}

	@Override
	@Transactional
	public ServiceResponse addCoach(UserAddEditDto userDto, String userId) {
		return addUser(userDto, userId, UserType.COACH);
	}

	@Transactional
	public ServiceResponse addUser(UserAddEditDto userDto, String userId, UserType userType) {
		try {
			Optional<UserProfile> userAdding = userProfileRepository.findById(userId);

			if (userAdding.isEmpty()) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_CREDENTIALS);
			}

			// Validate user profile constraints before creating new user
			// This ensures display name uniqueness per phone number and email uniqueness
			// across phone numbers
			try {
				validationHelper.validateUserProfile(userDto.getPhoneNumber(), userDto.getDisplayName(),
						userDto.getEmailId(), null // No user to exclude since this is a new user
				);
			} catch (ResourceException e) {
				log.warn("User profile validation failed during add: {}", e.getMessage());
				return ResponseBuilder.badRequest(e.getMessage());
			}

			List<UserProfile> existingProfiles = userProfileRepository
					.findByUsernameAndInactive(userDto.getPhoneNumber(), false);

			// This check will be after model mapper (in request body primaryAccount won't
			// be present, which then will be set to false)

			UserProfile profileToSave = UserMapper.mapPlayerDtoToUser(userDto);

			UserAuthDetails authDetails = null;
			// If existing profile exists, then use one of theirs authDetails object and set
			// primaryAccount of new user to false.
			if (!CollectionUtils.isEmpty(existingProfiles)) {
				Optional<UserAuthDetails> optionalAuthDetails = existingProfiles.stream()
						.map(UserProfile::getAuthDetails).filter(Objects::nonNull).findFirst();

				if (optionalAuthDetails.isPresent()) {
					authDetails = optionalAuthDetails.get();
					profileToSave.setPrimaryAccount(false);
				}
			}

			if (authDetails == null) {
				log.info("No existing authDetails found for username '{}'. Creating new authDetails.",
						userDto.getUsername());
				authDetails = new UserAuthDetails();
				profileToSave.setPrimaryAccount(true);
			}

			profileToSave.setUserType(userType);

			applyDefaultValues(profileToSave, userDto);

			if (userDto.getPreferredSports() != null && !userDto.getPreferredSports().isEmpty()) {
				profileToSave.setPreferredSports(
						getUserPreferredSportsMapping(profileToSave.getId(), userDto.getPreferredSports(), List.of()));
			}

			if (ObjectUtils.isNotEmpty(userDto.getUploadedDocuments())) {
				profileToSave
						.setUserDocuments(mapDocumentsDtoToDocumnet(userDto.getUploadedDocuments(), profileToSave));
			}

			if (!StringUtils.hasText(userDto.getPassword())) {
				userDto.setPassword(
						PasswordGenerator.generateDefaultPassword(userDto.getPhoneNumber(), userDto.getDisplayName()));
				authDetails.setDefaultPassword(true);
			}

			authDetails.setPasswordHashed(
					passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512(userDto.getPassword())));
			authDetails.setOtpHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512("1234")));

			try {
				UserAuthDetails savedUserAuth = userAuthDetailsRepo.save(authDetails);
				profileToSave.setAuthDetails(savedUserAuth);
			} catch (DuplicateKeyException duplicateKeyException) {
				throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, duplicateKeyException.getMessage());
			}

			UserProfile savedUser = userProfileRepository.save(profileToSave);

			if (userDto.getUserActions() != null) {
				addEditUserActions(savedUser, userDto.getUserActions());
			}

			if (userType == UserType.COACH) {
				if (!CollectionUtils.isEmpty(userDto.getAcademyId())) {
					mapCoachToAcademy(savedUser, userDto);
				}

				if (userDto.getAcademyCoaches() != null && !userDto.getAcademyCoaches().isEmpty()
						&& userDto.getAcademyCoaches().stream()
								.allMatch(dto -> dto.getAcademyId() != null && !dto.getAcademyId().isBlank())) {
					mapCoachToAcademyWithDesignation(savedUser, userDto);
				}
			}

			if (userType == UserType.PLAYER) {
				// First, handle academy mappings if academy IDs exist
				List<String> academyIds = extractAcademyIds(userDto);

				if (!academyIds.isEmpty()) {
					List<String> playerIds = List.of(savedUser.getId());
					try {
						for (String academyId : academyIds) {
							traineeService.addTraineesToAcademy(playerIds, academyId);
							log.info("Added player {} to academy {}", savedUser.getId(), academyId);
						}
					} catch (Exception e) {
						log.error("Failed to add player to academy: {}", e.getMessage(), e);
						return ResponseBuilder.error("Player created but failed to add to academy: " + e.getMessage(),
								HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}

				// Only process course enrollments if they exist
				if (userDto.getEnrollInAcademies() != null && !userDto.getEnrollInAcademies().isEmpty()
						&& hasValidCourseEnrollments(userDto.getEnrollInAcademies())) {
					ServiceResponse enrollmentResponse = processPlayerCourseEnrollments(userDto.getEnrollInAcademies(),
							savedUser);
					if (!enrollmentResponse.getHttpStatus().is2xxSuccessful()) {
						return enrollmentResponse;
					}
				}
			}

			return ResponseBuilder.success(ApiResponse.DATA_ADDED_SUCCESSFULLY);
		} catch (Exception e) {
			log.error("Exception occurred while adding user: {}", e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Extracts all academy IDs from the enrollment DTOs
	 */
	private List<String> extractAcademyIds(UserAddEditDto userDto) {
		if (userDto.getEnrollInAcademies() == null || userDto.getEnrollInAcademies().isEmpty()) {
			// If there are no enrollment DTOs but there's a direct academyId property, use
			// that
			if (userDto.getAcademyId() != null && !userDto.getAcademyId().isEmpty()) {
				return userDto.getAcademyId();
			}
			return List.of();
		}

		return userDto.getEnrollInAcademies().stream().map(PlayerEnrollInCourseDto::getAcademyId)
				.filter(id -> id != null && !id.isBlank()).distinct().collect(Collectors.toList());
	}

	/**
	 * Process player course enrollments across multiple academies and courses
	 *
	 * @param enrollInCourseDtos List of PlayerEnrollInCourseDto containing
	 *                           enrollment details
	 * @param playerUser         The player user profile
	 * @return ServiceResponse indicating success or failure
	 */
	private ServiceResponse processPlayerCourseEnrollments(List<PlayerEnrollInCourseDto> enrollInCourseDtos,
			UserProfile playerUser) {
		log.info("🔁 Starting enrollment process for player: {}", playerUser.getId());

		List<PaymentLedger> allLedgersToSave = new ArrayList<>();

		for (PlayerEnrollInCourseDto academyCourseDto : enrollInCourseDtos) {
			String academyId = academyCourseDto.getAcademyId();
			log.info("🏫 Processing academyId: {}", academyId);

			Optional<Academy> academyOpt = academyRepository.findById(academyId);
			if (academyOpt.isEmpty()) {
				log.error("❌ Academy with id {} not found", academyId);
				return ResponseBuilder.badRequest("Academy with id " + academyId + " not found");
			}

			if (academyCourseDto.getEnrollInPrograms() != null && !academyCourseDto.getEnrollInPrograms().isEmpty()) {
				for (EnrollTraineeInCourseDto traineeEnrollment : academyCourseDto.getEnrollInPrograms()) {
					String courseId = traineeEnrollment.getProgramId();
					log.info("📘 Processing courseId: {}", courseId);

					Optional<Course> courseOpt = courseRepo.findById(courseId);
					if (courseOpt.isEmpty()) {
						log.error("❌ Course with id {} not found", courseId);
						return ResponseBuilder.badRequest("Course with id " + courseId + " not found");
					}

					Course course = courseOpt.get();
					if (!course.getAcademy().getId().equals(academyId)) {
						log.error("❌ Course {} does not belong to academy {}", courseId, academyId);
						return ResponseBuilder
								.badRequest("Course " + courseId + " does not belong to academy " + academyId);
					}

					CourseDto courseDto;
					try {
						log.debug("📦 Fetching course details for validation...");
						courseDto = courseService.getCourse(academyId, courseId);
					} catch (Exception e) {
						log.error("❌ Error retrieving course details for id {}: {}", courseId, e.getMessage());
						return ResponseBuilder.badRequest("Error retrieving course details: " + e.getMessage());
					}

					if (traineeEnrollment.getPaymentSchedule() == null
							|| !courseDto.getPaymentOptions().containsKey(traineeEnrollment.getPaymentSchedule())) {
						log.error("❌ Invalid payment schedule {} for trainee {}",
								traineeEnrollment.getPaymentSchedule(), traineeEnrollment.getTraineeUserId());
						return ResponseBuilder.badRequest(
								"Invalid payment schedule for trainee: " + traineeEnrollment.getTraineeUserId());
					}

					if (traineeEnrollment.getJoiningDate().isAfter(traineeEnrollment.getDueDate())) {
						log.error("❌ Joining date {} is after due date {} for trainee {}",
								traineeEnrollment.getJoiningDate(), traineeEnrollment.getDueDate(),
								traineeEnrollment.getTraineeUserId());
						return ResponseBuilder.badRequest("Joining date cannot be after due date for trainee: "
								+ traineeEnrollment.getTraineeUserId());
					}

					traineeEnrollment.setTraineeUserId(playerUser.getId());

					// Simple check: if enrollment ID is present, it's an edit; otherwise, create
					// new
					if (traineeEnrollment.getId() != null) {
						// Edit existing enrollment
						ServiceResponse updateResponse = updateExistingEnrollment(traineeEnrollment, courseDto,
								academyId, courseId, allLedgersToSave);

						if (!updateResponse.getHttpStatus().is2xxSuccessful()) {
							return updateResponse;
						}
					} else {
						// Create new enrollment
						ServiceResponse createResponse = createNewEnrollment(traineeEnrollment, academyId, courseId,
								courseDto, playerUser, allLedgersToSave);

						if (!createResponse.getHttpStatus().is2xxSuccessful()) {
							return createResponse;
						}
					}
				}
			} else {
				log.warn("⚠️ No enrollments provided for academyId: {}", academyId);
			}
		}

		if (!allLedgersToSave.isEmpty()) {
			try {
				log.info("💾 Saving {} ledger entries to database...", allLedgersToSave.size());
				ledgerRepository.saveAll(allLedgersToSave);
			} catch (Exception e) {
				log.error("❌ Error saving ledger entries: {}", e.getMessage());
				return ResponseBuilder.internalServerError("Error saving ledger entries: " + e.getMessage());
			}
		} else {
			log.info("ℹ️ No ledger entries to save.");
		}

		log.info("✅ Enrollment process completed successfully for player: {}", playerUser.getId());
		return ResponseBuilder.success(ApiResponse.PLAYERS_ENROLLED_SUCCESSFULLY);
	}

	private ServiceResponse updateExistingEnrollment(EnrollTraineeInCourseDto traineeEnrollment, CourseDto courseDto,
			String academyId, String courseId, List<PaymentLedger> allLedgersToSave) {

		log.info("🔁 Updating existing enrollment with ID: {}", traineeEnrollment.getId());

		// Find existing enrollment by ID
		Optional<TraineeCourseEnrollment> existingEnrollmentOpt = traineeCourseEnrollmentRepo
				.findById(traineeEnrollment.getId());

		if (existingEnrollmentOpt.isEmpty()) {
			log.error("❌ Enrollment with id {} not found", traineeEnrollment.getId());
			return ResponseBuilder.badRequest("Enrollment with id " + traineeEnrollment.getId() + " not found");
		}

		TraineeCourseEnrollment existingEnrollment = existingEnrollmentOpt.get();

		existingEnrollment.setStatus(traineeEnrollment.getProgramStatus());
		existingEnrollment.setAmount(traineeEnrollment.getAmount());
		existingEnrollment.setJoiningDate(traineeEnrollment.getJoiningDate());
		existingEnrollment.setDueDate(traineeEnrollment.getDueDate());
		existingEnrollment.setPaymentSchedule(traineeEnrollment.getPaymentSchedule());

		try {
			TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();
			enrollmentDto.setAmount(traineeEnrollment.getAmount());
			enrollmentDto.setJoiningDate(traineeEnrollment.getJoiningDate());
			enrollmentDto.setDueDate(traineeEnrollment.getDueDate());
			enrollmentDto.setPaymentSchedule(traineeEnrollment.getPaymentSchedule());

			if (existingEnrollment.getDuesOn() == null) {
				existingEnrollment.setDuesOn(null);
				existingEnrollment.setFinalDueAmount(0L);
			}

			Long discountAmount = traineeEnrollment.getScheduleDiscountAmount();
			Boolean useDiscount = traineeEnrollment.getUseDiscountForFuture();

			existingEnrollment.setDiscountAmount(discountAmount != null ? discountAmount : 0L);
			existingEnrollment.setUseForFuture(Boolean.TRUE.equals(useDiscount));

			TraineeCourseEnrollment savedEnrollment = traineeCourseEnrollmentRepo.save(existingEnrollment);

			Long ledgerDiscount = traineeEnrollment.getOverallDiscountAmount();

			PaymentLedger discountLedger = createDiscountLedgerEntry(academyId, courseId,
					savedEnrollment.getTraineeUserProfile(), savedEnrollment, ledgerDiscount,
					savedEnrollment.getJoiningDate());
			if (discountLedger != null)
				allLedgersToSave.add(discountLedger);

		} catch (Exception e) {
			log.error("❌ Error calculating installments for existing enrollment: {}", e.getMessage());
			return ResponseBuilder.internalServerError("Error calculating installments: " + e.getMessage());
		}

		return ResponseBuilder.success("Enrollment updated successfully");
	}

	private ServiceResponse createNewEnrollment(EnrollTraineeInCourseDto traineeEnrollment, String academyId,
			String courseId, CourseDto courseDto, UserProfile playerUser, List<PaymentLedger> allLedgersToSave) {

		log.info("🆕 Creating new enrollment for trainee {} (reason: {})", playerUser.getId(),
				"previous enrollment was inactive");

		try {
			TraineeCourseEnrollment enrollment = createTraineeCourseEnrollment(traineeEnrollment, academyId, courseId);

			TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();
			enrollmentDto.setAmount(traineeEnrollment.getAmount());
			enrollmentDto.setJoiningDate(traineeEnrollment.getJoiningDate());
			enrollmentDto.setDueDate(traineeEnrollment.getDueDate());
			enrollmentDto.setPaymentSchedule(traineeEnrollment.getPaymentSchedule());

			Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
					.calculateInstallmentsAndDueDates(enrollmentDto, courseDto, enrollmentDto.getPaymentSchedule());

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

			if (lastDueInstallment != null) {
				enrollment.setDuesOn(lastDueInstallment.getDueDate());
				enrollment.setFinalDueAmount(lastDueInstallment.getAmount());
			} else {
				enrollment.setDuesOn(traineeEnrollment.getDueDate());
				enrollment.setFinalDueAmount(traineeEnrollment.getAmount());
			}

			TraineeCourseEnrollment savedEnrollment = traineeCourseEnrollmentRepo.save(enrollment);

			Long ledgerDiscount = traineeEnrollment.getOverallDiscountAmount();

			// For new enrollments, always create past due ledgers and registration fee
			if (!installmentsBeforeLastDue.isEmpty()) {
				log.info("📄 Creating {} past due ledger(s) for new enrollment...", installmentsBeforeLastDue.size());
				for (InstallmentInfo pastInst : installmentsBeforeLastDue) {
					PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(academyId, courseId, savedEnrollment,
							pastInst);
					allLedgersToSave.add(pastDueLedger);
				}
			}

			PaymentLedger regFeeLedger = createRegistrationFeeLedgerEntry(academyId, courseId,
					savedEnrollment.getTraineeUserProfile(), savedEnrollment, courseDto,
					savedEnrollment.getJoiningDate());
			allLedgersToSave.add(regFeeLedger);

			PaymentLedger discountLedger = createDiscountLedgerEntry(academyId, courseId,
					savedEnrollment.getTraineeUserProfile(), savedEnrollment, ledgerDiscount,
					savedEnrollment.getJoiningDate());
			if (discountLedger != null)
				allLedgersToSave.add(discountLedger);

		} catch (Exception e) {
			log.error("❌ Error calculating installments for new enrollment: {}", e.getMessage());
			return ResponseBuilder.internalServerError("Error calculating installments: " + e.getMessage());
		}

		return ResponseBuilder.success("New enrollment created successfully");
	}

	private PaymentLedger createDiscountLedgerEntry(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long discountAmount, LocalDate joiningDate) {

		if (discountAmount == null || discountAmount <= 0) {
			return null; // nothing to create
		}

		PaymentLedger discountLedger = new PaymentLedger();
		discountLedger.setUser(UserProfile.builder().id(traineeUser.getId()).build());
		discountLedger.setEnrollment(enrollment);
		discountLedger.setAcademy(Academy.builder().id(academyId).build());
		discountLedger.setProgram(Course.builder().id(courseId).build());
		discountLedger.setEffectiveDate(joiningDate);
		discountLedger.setLedgerType(PaymentLedger.LedgerType.CREDIT);
		discountLedger.setEntryType(PaymentLedger.PaymentEntryType.DISCOUNT);
		discountLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.PENDING);
		discountLedger.setAmount(discountAmount.doubleValue());
		discountLedger.setRemainingAmount(discountAmount.doubleValue());
		discountLedger.setCategory(PaymentCategory.COURSE_FEE);
		discountLedger.setIsReversal(Boolean.FALSE);
		discountLedger.setIsLocked(Boolean.FALSE);
		discountLedger.setDescription("Course discount applied");

		log.info("Discount ledger entry created for trainee id {}", traineeUser.getId());
		return discountLedger;
	}

	private PaymentLedger createRegistrationFeeLedgerEntry(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, CourseDto courseDto, LocalDate joiningDate) {

		Long regFeeAmount = courseDto.getRegistrationFee() != null ? courseDto.getRegistrationFee() : 0L;

		PaymentLedger regFeeLedger = new PaymentLedger();
		regFeeLedger.setUser(UserProfile.builder().id(traineeUser.getId()).build());
		regFeeLedger.setEnrollment(enrollment);
		regFeeLedger.setAcademy(Academy.builder().id(academyId).build());
		regFeeLedger.setProgram(Course.builder().id(courseId).build());
		regFeeLedger.setEffectiveDate(joiningDate);
		regFeeLedger.setLedgerType(PaymentLedger.LedgerType.DEBIT);
		regFeeLedger.setEntryType(PaymentLedger.PaymentEntryType.REGISTRATION);

		boolean hasRegFee = regFeeAmount > 0;

		regFeeLedger.setEntryStatus(
				hasRegFee ? PaymentLedger.PaymentEntryStatus.PENDING : PaymentLedger.PaymentEntryStatus.SETTLED);
		double amount = hasRegFee ? regFeeAmount.doubleValue() : 0.0;

		regFeeLedger.setAmount(amount);
		regFeeLedger.setRemainingAmount(amount);

		regFeeLedger.setCategory(PaymentCategory.REGISTRATION_FEE);
		regFeeLedger.setIsReversal(Boolean.FALSE);
		regFeeLedger.setIsLocked(Boolean.FALSE);
		regFeeLedger.setDescription("Course registration fee");

		log.info("Registration fee ledger entry created for trainee id {}", traineeUser.getId());
		return regFeeLedger;
	}

	/**
	 * Create a TraineeCourseEnrollment entity from enrollment DTO
	 */
	private TraineeCourseEnrollment createTraineeCourseEnrollment(EnrollTraineeInCourseDto traineeEnrollment,
			String academyId, String courseId) {

		TraineeCourseEnrollment enrollment = new TraineeCourseEnrollment();
		enrollment.setId(UUID.randomUUID().toString());
		enrollment.setTraineeUserProfile(UserProfile.builder().id(traineeEnrollment.getTraineeUserId()).build());
		enrollment.setCourse(Course.builder().id(courseId).build());
		enrollment.setAcademy(Academy.builder().id(academyId).build());
		enrollment.setStatus(Status.ACTIVE);
		enrollment.setCreatedOn(Timestamp.from(Instant.now()));
		enrollment.setPaymentSchedule(traineeEnrollment.getPaymentSchedule());
		enrollment.setAmount(traineeEnrollment.getAmount());
		enrollment.setJoiningDate(traineeEnrollment.getJoiningDate());
		enrollment.setDueDate(traineeEnrollment.getDueDate());

		Long discountAmount = traineeEnrollment.getScheduleDiscountAmount();
		Boolean useDiscount = traineeEnrollment.getUseDiscountForFuture();

		if (discountAmount == null || discountAmount == 0L) {
			enrollment.setDiscountAmount(0L);
			enrollment.setUseForFuture(Boolean.FALSE);
		} else {
			enrollment.setDiscountAmount(discountAmount);
			// Only update useForFuture if a value is actually provided
			if (useDiscount != null) {
				enrollment.setUseForFuture(useDiscount);
			} else if (useDiscount == null) {
				enrollment.setUseForFuture(Boolean.FALSE);
			}
		}

		return enrollment;
	}

	private void applyDefaultValues(UserProfile profile, UserAddEditDto userProfileDto) {
		log.debug("Applying default values for new user");

		profile.setId(UUID.randomUUID().toString());

		profile.setRole(Role.USER);

		profile.setCreatedOn(Timestamp.from(Instant.now()));

		// Set user type
		profile.setUserType(userProfileDto.getUserType() == null ? UserType.PLAYER : UserType.COACH);

		// Set role
		profile.setRole(userProfileDto.getRole() == null ? Role.USER : userProfileDto.getRoleType());

		// Set username if not provided
		if (userProfileDto.getUsername() == null || userProfileDto.getUsername().isBlank()) {
			profile.setUsername(userProfileDto.getPhoneNumber());
		}

		// Set password or default
//		if (userProfileDto.getPassword() != null && !userProfileDto.getPassword().isBlank()) {
//			String tempass = PasswordGenerator.generateDefaultPassword(userProfileDto.getPhoneNumber(),
//					userProfileDto.getDisplayName());
//			profile.setPasswordHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512(tempass)));
//			profile.setDefaultPassword(true);
//
//		} else {
//			String tempass = PasswordGenerator.generateDefaultPassword(userProfileDto.getPhoneNumber(),
//					userProfileDto.getDisplayName());
//			profile.setPasswordHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512(tempass)));
//			profile.setDefaultPassword(true);
//		}

		// Set default OTP
//		profile.setOtpHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512("1234")));

		// Set profile picture based on gender
		if (userProfileDto.getGender() == Gender.MALE) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlMale);
		} else if (userProfileDto.getGender() == Gender.FEMALE) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlFemale);
		}

		// Set default values for all boolean fields to avoid null values
		profile.setInactive(false);
//		profile.setDefaultPassword(true);
//		profile.setPhoneNumberVerified(false);
//		profile.setEmailIdVerified(false);
//		profile.setOtpUsed(false);
	}

	private void addEditUserActions(UserProfile profileToSave, String userActions) {
		UsersActionsMapping usersActionsMapping = new UsersActionsMapping();

		if (StringUtils.hasText(userActions.trim())) {
			Optional<UsersActionsMapping> optionalUser = usersActionsMappingRepository.findByUser(profileToSave);
			if (optionalUser.isPresent()) {
				usersActionsMapping = optionalUser.get();
				usersActionsMapping.setActions(userActions);
				usersActionsMappingRepository.save(usersActionsMapping);
			} else {
				usersActionsMapping.setUser(profileToSave);
				usersActionsMapping.setActions(userActions);
				usersActionsMappingRepository.save(usersActionsMapping);
			}
		}

	}

	@Transactional
	public List<CoachAcademyMapping> mapCoachToAcademyWithDesignation(UserProfile savedUser,
			UserAddEditDto userProfileDto) {

		// Basic validation
		if (savedUser == null || userProfileDto == null || userProfileDto.getAcademyCoaches() == null
				|| userProfileDto.getAcademyCoaches().isEmpty()) {
			return List.of();
		}

		// Create a map of academyId -> CoachAcademyDto for quick access
		Map<String, CoachAcademyDetails> incomingMappingMap = userProfileDto.getAcademyCoaches().stream()
				.collect(Collectors.toMap(CoachAcademyDetails::getAcademyId, dto -> dto));

		Set<String> newAcademyIds = incomingMappingMap.keySet();

		// Fetch all existing mappings for the user
		List<CoachAcademyMapping> existingMappings = coachAcademyMappingRepository.findByCoachUserProfile(savedUser);
		Set<String> existingAcademyIds = existingMappings.stream().map(mapping -> mapping.getAcademy().getId())
				.collect(Collectors.toSet());

		// Identify mappings to delete
		List<CoachAcademyMapping> toDelete = existingMappings.stream()
				.filter(mapping -> !newAcademyIds.contains(mapping.getAcademy().getId())).toList();

		if (!toDelete.isEmpty()) {
			coachAcademyMappingRepository.deleteAll(toDelete);
		}

		// Fetch academies
		List<Academy> academies = academyRepository.findAllById(newAcademyIds);

		// Map or update coach-academy mappings
		List<CoachAcademyMapping> updatedMappings = academies.stream().map(academy -> {
			Optional<CoachAcademyMapping> existingMapping = coachAcademyMappingRepository
					.findByCoachUserProfileAndAcademy(savedUser, academy);

			CoachAcademyMapping mapping = existingMapping.orElseGet(CoachAcademyMapping::new);

			if (existingMapping.isEmpty()) {
				mapping.setId(UUID.randomUUID().toString());
				mapping.setCreatedOn(Timestamp.from(Instant.now()));
			}

			CoachAcademyDetails coachDto = incomingMappingMap.get(academy.getId());

			mapping.setAcademy(academy);
			mapping.setCoachUserProfile(savedUser);
			mapping.setDesignation(coachDto.getDesignation());
			mapping.setExperienceInMonths(userProfileDto.getExperienceInMonths());
			mapping.setStatus(coachDto.getStatus());
			mapping.setUpdatedOn(Timestamp.from(Instant.now()));

			return mapping;
		}).toList();

		return coachAcademyMappingRepository.saveAll(updatedMappings);
	}

	public Map<String, String> mapAcademyDesignations(List<String> academyIds, UserAddEditDto userProfileDto) {
		if (academyIds == null || academyIds.isEmpty() || userProfileDto == null) {
			return Map.of();
		}

		List<Academy> academies = academyRepository.findAllById(academyIds);
		return academies.stream().collect(Collectors.toMap(Academy::getId, academy -> userProfileDto.getDesignation()));
	}

	private UserDocuments mapDocumentsDtoToDocumnet(UserDocumentsDto dtos, UserProfile profileToSave) {
		UserDocuments docu = new UserDocuments();
		BeanUtils.copyProperties(dtos, docu);
		docu.setUser(profileToSave);
		return docu;
	}

	private List<UserPreferredSportsMapping> getUserPreferredSportsMapping(String id, List<Sports> sports,
			List<UserPreferredSportsMapping> existingPreferredSports) {
		Map<Sports, UserPreferredSportsMapping> existingSports = CollectionUtils.isEmpty(existingPreferredSports)
				? new HashMap<>()
				: existingPreferredSports.stream().collect(Collectors.toMap(UserPreferredSportsMapping::getSport,
						userPreferredSportsMapping -> userPreferredSportsMapping));

		return sports.stream().map(sport -> {
			if (existingSports.containsKey(sport)) {
				return existingSports.get(sport);
			}
			UserPreferredSportsMapping userPreferredSportsMapping = new UserPreferredSportsMapping();
			userPreferredSportsMapping.setUserProfile(UserProfile.builder().id(id).build());
			userPreferredSportsMapping.setSport(sport);
			return userPreferredSportsMapping;
		}).toList();
	}

	private void mapCoachToAcademy(UserProfile savedUser, UserAddEditDto userProfileDto) {
		List<Academy> academyList = academyRepository.findAllById(userProfileDto.getAcademyId());

		if (CollectionUtils.isEmpty(academyList)) {
			return;
		}

		List<CoachAcademyMapping> listCoachAcademyEntity = academyList.stream().map(academy -> {
			CoachAcademyMapping coachAcademyMapping = new CoachAcademyMapping();
			coachAcademyMapping.setId(UUID.randomUUID().toString());
			coachAcademyMapping.setAcademy(academy);
			coachAcademyMapping.setCoachUserProfile(savedUser);
			coachAcademyMapping.setCreatedOn(Timestamp.from(Instant.now()));
			coachAcademyMapping.setDesignation(userProfileDto.getDesignation());
			coachAcademyMapping.setExperienceInMonths(userProfileDto.getExperienceInMonths());
			coachAcademyMapping.setStatus(Status.ACTIVE);
			coachAcademyMapping.setRoleId(userProfileDto.getRole().getId());
			return coachAcademyMapping;
		}).toList();

		coachAcademyMappingRepository.saveAll(listCoachAcademyEntity);
	}

	@Override
	@Transactional
	public ServiceResponse editPlayer(UserAddEditDto userProfileDto, String userId) {
		return editUser(userProfileDto, userId, UserType.PLAYER);
	}

	@Override
	@Transactional
	public ServiceResponse editCoach(UserAddEditDto userProfileDto, String userId) {
		return editUser(userProfileDto, userId, UserType.COACH);
	}

	@Transactional
	public ServiceResponse editUser(UserAddEditDto userProfileDto, String userId, UserType userType) {
		try {
			// Validate requesting user exists
			Optional<UserProfile> userEditing = userProfileRepository.findById(userId);
			if (userEditing.isEmpty()) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_CREDENTIALS);
			}

			// Validate user to be edited exists
			if (userProfileDto.getId() == null || userProfileDto.getId().isBlank()) {
				return ResponseBuilder.badRequest("User ID is required for editing");
			}

			Optional<UserProfile> userToEdit = userProfileRepository.findById(userProfileDto.getId());
			if (userToEdit.isEmpty()) {
				return ResponseBuilder.notFound("User with ID " + userProfileDto.getId() + " not found");
			}

			UserProfile profileToUpdate = userToEdit.get();

			// Validate user profile constraints before updating
			// This ensures display name uniqueness per phone number and email uniqueness
			// across phone numbers
			try {
				validationHelper.validateUserProfile(
						userProfileDto.getPhoneNumber() != null ? userProfileDto.getPhoneNumber()
								: profileToUpdate.getPhoneNumber(),
						userProfileDto.getDisplayName() != null ? userProfileDto.getDisplayName()
								: profileToUpdate.getDisplayName(),
						userProfileDto.getEmailId() != null ? userProfileDto.getEmailId()
								: profileToUpdate.getEmailId(),
						profileToUpdate.getId() // Exclude current user from validation
				);
			} catch (ResourceException e) {
				log.warn("User profile validation failed during edit: {}", e.getMessage());
				return ResponseBuilder.badRequest(e.getMessage());
			}

			// Update basic user profile fields
			updateUserProfileFields(profileToUpdate, userProfileDto);

			// Handle preferred sports if provided
			if (userProfileDto.getPreferredSports() != null && !userProfileDto.getPreferredSports().isEmpty()) {
				profileToUpdate.setPreferredSports(getUserPreferredSportsMapping(profileToUpdate.getId(),
						userProfileDto.getPreferredSports(), profileToUpdate.getPreferredSports()));
			}

			// Handle uploaded documents if provided
			if (ObjectUtils.isNotEmpty(userProfileDto.getUploadedDocuments())) {
				profileToUpdate.setUserDocuments(
						mapDocumentsDtoToDocumnet(userProfileDto.getUploadedDocuments(), profileToUpdate));
			}

			// Update user actions if provided
			if (userProfileDto.getUserActions() != null) {
				addEditUserActions(profileToUpdate, userProfileDto.getUserActions());
			}

			// Save updated user profile
			UserProfile savedUser = userProfileRepository.save(profileToUpdate);

			// Handle type-specific updates
			if (userType == UserType.COACH) {
				handleCoachSpecificUpdates(savedUser, userProfileDto);
			} else if (userType == UserType.PLAYER) {
				handlePlayerSpecificUpdates(savedUser, userProfileDto);
			}

			return ResponseBuilder.success(ApiResponse.DATA_UPDATED_SUCCESSFULLY);
		} catch (Exception e) {
			log.error("Exception occurred while updating user: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	@Transactional
	public ServiceResponse deletePlayer(String id, String userId) {
		try {
			// Validate that the user exists
			Optional<UserProfile> userToDelete = userProfileRepository.findById(id);
			if (userToDelete.isEmpty()) {
				return ResponseBuilder.notFound("User with id " + id + " not found");
			}

			// Validate that the requesting user exists
			Optional<UserProfile> requestingUser = userProfileRepository.findById(userId);
			if (requestingUser.isEmpty()) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_CREDENTIALS);
			}

			UserProfile user = userToDelete.get();

			// Set user to inactive instead of deleting from database
			user.setInactive(true);

			userProfileRepository.save(user);

			return ResponseBuilder.success(ApiResponse.USER_INACTIVATED_SUCCESSFULLY);
		} catch (Exception e) {
			log.error("Exception occurred while deleting user: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Updates the basic fields of a user profile
	 */
	private void updateUserProfileFields(UserProfile profileToUpdate, UserAddEditDto userProfileDto) {
		// Update basic fields
		if (StringUtils.hasText(userProfileDto.getDisplayName())) {
			profileToUpdate.setDisplayName(userProfileDto.getDisplayName());
		}

		if (StringUtils.hasText(userProfileDto.getEmailId())) {
			profileToUpdate.setEmailId(userProfileDto.getEmailId());
		}

		if (StringUtils.hasText(userProfileDto.getPhoneNumber())) {
			profileToUpdate.setPhoneNumber(userProfileDto.getPhoneNumber());
		}

		if (StringUtils.hasText(userProfileDto.getProfilePictureUrl())) {
			profileToUpdate.setProfilePictureUrl(userProfileDto.getProfilePictureUrl());
		}

		if (StringUtils.hasText(userProfileDto.getPincode())) {
			profileToUpdate.setPincode(userProfileDto.getPincode());
		}

		if (StringUtils.hasText(userProfileDto.getCity())) {
			profileToUpdate.setCity(userProfileDto.getCity());
		}

		if (StringUtils.hasText(userProfileDto.getState())) {
			profileToUpdate.setState(userProfileDto.getState());
		}

		if (StringUtils.hasText(userProfileDto.getCountry())) {
			profileToUpdate.setCountry(userProfileDto.getCountry());
		}

		if (StringUtils.hasText(userProfileDto.getAddressLine1())) {
			profileToUpdate.setAddressLine1(userProfileDto.getAddressLine1());
		}

		if (StringUtils.hasText(userProfileDto.getAddressLine2())) {
			profileToUpdate.setAddressLine2(userProfileDto.getAddressLine2());
		}

		if (userProfileDto.getExperienceInMonths() != null) {
			profileToUpdate.setExperienceInMonths(userProfileDto.getExperienceInMonths());
		}

		if (userProfileDto.getRoleType() != null) {
			profileToUpdate.setRole(userProfileDto.getRoleType());
		}

		if (StringUtils.hasText(userProfileDto.getDob())) {
			profileToUpdate.setDob(userProfileDto.getDob());
		}

		if (userProfileDto.getGender() != null) {
			profileToUpdate.setGender(userProfileDto.getGender());
		}

//		// Update password if provided
//		if (StringUtils.hasText(userProfileDto.getPassword())) {
//			String tempass = PasswordGenerator.generateDefaultPassword(userProfileDto.getPhoneNumber(),
//					userProfileDto.getDisplayName());
//			profileToUpdate
//					.setPasswordHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512(tempass)));
//			profileToUpdate.setDefaultPassword(true);
//		}

	}

	/**
	 * Handles coach-specific updates including academy mappings
	 */
	private void handleCoachSpecificUpdates(UserProfile savedUser, UserAddEditDto userProfileDto) {
		// First handle academy-coach mappings with detailed information if provided
		if (userProfileDto.getAcademyCoaches() != null && !userProfileDto.getAcademyCoaches().isEmpty()) {
			mapCoachToAcademyWithDesignation(savedUser, userProfileDto);
		}

		// Handle inactive academy IDs
		if (userProfileDto.getInactiveAcademyIds() != null && !userProfileDto.getInactiveAcademyIds().isEmpty()) {
			deactivateCoachAcademyMappings(savedUser, userProfileDto.getInactiveAcademyIds());
		}
	}

	/**
	 * Handles player-specific updates including academy and program mappings
	 */
	/**
	 * Handles player-specific updates including academy and program mappings
	 */
	private void handlePlayerSpecificUpdates(UserProfile savedUser, UserAddEditDto userProfileDto) {
		try {
			// Handle academy status updates via enrollInAcademies
			if (userProfileDto.getEnrollInAcademies() != null && !userProfileDto.getEnrollInAcademies().isEmpty()) {
				// First update academy statuses based on provided values
				updateAcademyStatuses(savedUser.getId(), userProfileDto.getEnrollInAcademies());

				// Then process course enrollments only if they contain valid data
				if (hasValidCourseEnrollments(userProfileDto.getEnrollInAcademies())) {
					// Extract academyIds from enrollInAcademies
					List<String> academyIds = userProfileDto.getEnrollInAcademies().stream()
							.map(PlayerEnrollInCourseDto::getAcademyId).filter(id -> id != null && !id.isBlank())
							.collect(Collectors.toList());

					// Check existing academy mappings
					List<TraineeAcademyMapping> existingMappings = traineeAcademyMappingRepo
							.findByTraineeUserProfile_Id(savedUser.getId());
					Set<String> existingAcademyIds = existingMappings.stream().map(m -> m.getAcademy().getId())
							.collect(Collectors.toSet());

					// Add trainee to new academies
					List<String> academiesToAdd = academyIds.stream().filter(id -> !existingAcademyIds.contains(id))
							.collect(Collectors.toList());

					if (!academiesToAdd.isEmpty()) {
						for (String academyId : academiesToAdd) {
							try {
								traineeService.addTraineesToAcademy(List.of(savedUser.getId()), academyId);
							} catch (ResourceException e) {
								log.error("Failed to add trainee to academy: {}", e.getMessage(), e);
							}
						}
					}

					// Process course enrollments
					ServiceResponse enrollmentResponse = processPlayerCourseEnrollments(
							userProfileDto.getEnrollInAcademies(), savedUser);
					if (!enrollmentResponse.getHttpStatus().is2xxSuccessful()) {
						throw new RuntimeException("Failed to process course enrollments");
					}
				}
			}

			// Handle inactive academy IDs
			if (userProfileDto.getInactiveAcademyIds() != null && !userProfileDto.getInactiveAcademyIds().isEmpty()) {
				deactivatePlayerAcademyMappings(savedUser, userProfileDto.getInactiveAcademyIds());
			}

			// Handle inactive program IDs
			if (userProfileDto.getInactiveProgramIds() != null && !userProfileDto.getInactiveProgramIds().isEmpty()) {
				deactivatePlayerProgramEnrollments(savedUser, userProfileDto.getInactiveProgramIds());
			}
		} catch (Exception e) {
			log.error("Error in handlePlayerSpecificUpdates for user {}: {}", savedUser.getId(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Checks if the list of PlayerEnrollInCourseDto has any valid course
	 * enrollments
	 */
	private boolean hasValidCourseEnrollments(List<PlayerEnrollInCourseDto> enrollments) {
		if (enrollments == null || enrollments.isEmpty()) {
			return false;
		}

		return enrollments.stream().anyMatch(
				enrollment -> enrollment.getEnrollInPrograms() != null && !enrollment.getEnrollInPrograms().isEmpty());
	}

	/**
	 * Updates academy statuses for a trainee based on provided academy mappings
	 *
	 * @param userId          The ID of the user/trainee
	 * @param academyMappings List of academy enrollments with status information
	 */
	private void updateAcademyStatuses(String userId, List<PlayerEnrollInCourseDto> academyMappings) {
		if (academyMappings == null || academyMappings.isEmpty()) {
			return;
		}

		log.info("Updating academy statuses for user: {} with {} academy mappings", userId, academyMappings.size());

		List<TraineeAcademyMapping> existingMappings = traineeAcademyMappingRepo.findByTraineeUserProfile_Id(userId);
		Map<String, TraineeAcademyMapping> mappingsByAcademyId = existingMappings.stream()
				.collect(Collectors.toMap(mapping -> mapping.getAcademy().getId(), mapping -> mapping));

		List<TraineeAcademyMapping> mappingsToUpdate = new ArrayList<>();

		for (PlayerEnrollInCourseDto academyMapping : academyMappings) {
			String academyId = academyMapping.getAcademyId();
			Status academyStatus = academyMapping.getAcademyStatus();

			if (academyId != null && !academyId.isBlank() && academyStatus != null) {
				log.debug("Processing academy mapping: academyId={}, status={}", academyId, academyStatus);

				TraineeAcademyMapping existingMapping = mappingsByAcademyId.get(academyId);

				if (existingMapping != null) {
					log.debug("Found existing mapping for academyId={}, updating status from {} to {}", academyId,
							existingMapping.getStatus(), academyStatus);

					existingMapping.setStatus(academyStatus);
					mappingsToUpdate.add(existingMapping);
				} else {
					log.debug("No existing mapping found for academyId={}, checking if academy exists", academyId);

					// Check if academy exists
					Optional<Academy> academyOpt = academyRepository.findById(academyId);
					if (academyOpt.isPresent()) {
						Optional<UserProfile> userOpt = userProfileRepository.findById(userId);

						if (userOpt.isPresent()) {
							// Create new mapping with the specified status
							TraineeAcademyMapping newMapping = new TraineeAcademyMapping();

							// SOLUTION 1: Generate UUID for the ID
							newMapping.setId(UUID.randomUUID().toString());

							newMapping.setTraineeUserProfile(userOpt.get());
							newMapping.setAcademy(academyOpt.get());
							newMapping.setStatus(academyStatus);
							newMapping.setCreatedOn(Timestamp.from(Instant.now()));

							log.info("Creating new mapping for academyId={} with status {}", academyId, academyStatus);
							mappingsToUpdate.add(newMapping);
						}
					} else {
						log.warn("Academy with ID {} not found, skipping status update", academyId);
					}
				}
			}
		}

		if (!mappingsToUpdate.isEmpty()) {
			log.info("Saving {} updated trainee-academy mappings", mappingsToUpdate.size());
			traineeAcademyMappingRepo.saveAll(mappingsToUpdate);
		} else {
			log.info("No trainee-academy mappings to update");
		}
	}

	/**
	 * Deactivates coach-academy mappings for the given academy IDs
	 */
	private void deactivateCoachAcademyMappings(UserProfile coach, List<String> academyIds) {
		if (coach == null || CollectionUtils.isEmpty(academyIds)) {
			return;
		}

		log.info("Deactivating coach {} from academies: {}", coach.getId(), academyIds);

		try {

			// Find coach-academy mappings
			List<CoachAcademyMapping> mappingsToUpdate = coachAcademyMappingRepository
					.findByAcademy_IdInAndCoachUserProfile_Id(academyIds, coach.getId());

			// Set status to inactive
			mappingsToUpdate.forEach(mapping -> {
				mapping.setStatus(Status.INACTIVE);
				mapping.setUpdatedOn(Timestamp.from(Instant.now()));
			});

			coachAcademyMappingRepository.saveAll(mappingsToUpdate);

			log.info("Successfully deactivated {} coach-academy mappings", mappingsToUpdate.size());
		} catch (Exception e) {
			log.error("Error deactivating coach-academy mappings: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Deactivates player in the specified academies
	 */
	private void deactivatePlayerAcademyMappings(UserProfile player, List<String> academyIds) {
		if (player == null || CollectionUtils.isEmpty(academyIds)) {
			return;
		}

		log.info("Deactivating player {} from academies: {}", player.getId(), academyIds);

		try {
			List<TraineeCourseEnrollment> enrollmentsToUpdate = traineeCourseEnrollmentRepo
					.findByAcademy_IdInAndTraineeUserProfile_Id(academyIds, player.getId());

			enrollmentsToUpdate.forEach(enrollment -> {
				enrollment.setStatus(Status.INACTIVE);
			});

			traineeCourseEnrollmentRepo.saveAll(enrollmentsToUpdate);

			log.info("Successfully deactivated player in {} course enrollments across academies",
					enrollmentsToUpdate.size());
		} catch (Exception e) {
			log.error("Error deactivating player academy mappings: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Deactivates player enrollments in the specified programs
	 */
	private void deactivatePlayerProgramEnrollments(UserProfile player, List<String> programIds) {
		if (player == null || CollectionUtils.isEmpty(programIds)) {
			return;
		}

		log.info("Deactivating player {} from programs: {}", player.getId(), programIds);

		try {

			List<TraineeCourseEnrollment> enrollmentsToUpdate = traineeCourseEnrollmentRepo
					.findByCourse_IdInAndTraineeUserProfile_Id(programIds, player.getId());

			enrollmentsToUpdate.forEach(enrollment -> {
				enrollment.setStatus(Status.INACTIVE);
			});

			traineeCourseEnrollmentRepo.saveAll(enrollmentsToUpdate);

			log.info("Successfully deactivated {} player program enrollments", enrollmentsToUpdate.size());
		} catch (Exception e) {
			log.error("Error deactivating player program enrollments: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public ServiceResponse getCoachKpiById(String coachId, String userId, String academyDomain) {
		log.info("Fetching coach KPI data for coach ID: {}", coachId);

		// Validate coach exists
		Optional<UserProfile> optionalCoach = userProfileRepository.findById(coachId);
		if (optionalCoach.isEmpty()) {
			log.warn("Coach not found with ID: {}", coachId);
			return ResponseBuilder.badRequest(ApiResponse.USER_NOT_FOUND);
		}

		try {
			// Fetch all data for the coach
			List<AttendanceView> allAttendance = attendanceViewRepository.findByCoachId(coachId);
			List<PaymentDetailsView> allPayments = paymentRepository.findByCoachId(coachId);
			List<CoachPerformanceReportView> allReports = coachPerformanceViewRepository.findByCoachId(coachId);

			// NEW: Fetch attendance marked by this coach
			List<AttendanceView> attendanceMarkedByCoach = attendanceViewRepository.findByMarkedById(coachId);

			// NEW: Fetch trainee performance reports created by this coach
			List<TraineePerformanceReportView> traineeReportsCreatedByCoach = performanceViewRepository
					.findByCoachId(coachId);

			// NEW: Fetch performance feedback received by this coach (assuming coach_id is
			// the recipient)
			List<CoachPerformanceReportView> performanceFeedbackForCoach = coachPerformanceViewRepository
					.findByCoachId(coachId);

			// Handle null collections
			allAttendance = allAttendance != null ? allAttendance : new ArrayList<>();
			allPayments = allPayments != null ? allPayments : new ArrayList<>();
			allReports = allReports != null ? allReports : new ArrayList<>();
			attendanceMarkedByCoach = attendanceMarkedByCoach != null ? attendanceMarkedByCoach : new ArrayList<>();
			traineeReportsCreatedByCoach = traineeReportsCreatedByCoach != null ? traineeReportsCreatedByCoach
					: new ArrayList<>();
			performanceFeedbackForCoach = performanceFeedbackForCoach != null ? performanceFeedbackForCoach
					: new ArrayList<>();

			// Apply domain filter if specified
			List<AttendanceView> filteredAttendance = filterByDomain(allAttendance, academyDomain);
			List<PaymentDetailsView> filteredPayments = filterByDomain(allPayments, academyDomain);
			List<CoachPerformanceReportView> filteredReports = filterByDomain(allReports, academyDomain);
			List<AttendanceView> filteredAttendanceMarkedByCoach = filterByDomain(attendanceMarkedByCoach,
					academyDomain);
			List<TraineePerformanceReportView> filteredTraineeReports = filterByDomain(traineeReportsCreatedByCoach,
					academyDomain);
			List<CoachPerformanceReportView> filteredPerformanceFeedback = filterByDomain(performanceFeedbackForCoach,
					academyDomain);

			UserAddEditDao coachDto = toDto(optionalCoach.get());

			// Calculate KPIs with new metrics
			CoachKpiData kpiData = calculateCoachKpis(filteredAttendance, filteredPayments, filteredReports,
					filteredAttendanceMarkedByCoach, filteredTraineeReports, filteredPerformanceFeedback);

			// Create response payload
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("coachDetails", coachDto);
			responseData.put("kpiSummary", kpiData);

			return ResponseBuilder.success(responseData, ApiResponse.LIST_FETCHED_SUCCESSFULLY);

		} catch (Exception e) {
			log.error("Error fetching coach KPI data for coach ID: {}", coachId, e);
			return ResponseBuilder.internalServerError("Failed to fetch coach KPI data");
		}
	}

	private CoachKpiData calculateCoachKpis(List<AttendanceView> attendance, List<PaymentDetailsView> payments,
			List<CoachPerformanceReportView> reports, List<AttendanceView> attendanceMarkedByCoach,
			List<TraineePerformanceReportView> traineeReports, List<CoachPerformanceReportView> performanceFeedback) {

		CoachKpiData kpi = new CoachKpiData();

		// Initialize with default values
		kpi.setTotalStudentsCoached(0);
		kpi.setTotalSessions(0);
		kpi.setTotalUniqueSessions(0);
		kpi.setTotalRevenueGenerated(BigDecimal.ZERO);
		kpi.setSuccessfulPayments(0);
		kpi.setTotalPerformanceReports(0);
		kpi.setStudentsWithReports(0);
		kpi.setProgramsCoached(0);
		kpi.setAcademiesCoached(0);

		// NEW: Initialize new metrics
		kpi.setAttendanceMarkedByCoach(0);
		kpi.setPlayerPerformanceReportsCreated(0);
		kpi.setPerformanceFeedbackReceived(0);

		// Student Analytics
		calculateStudentMetrics(kpi, attendance);

		// Attendance Analytics
		calculateAttendanceMetrics(kpi, attendanceMarkedByCoach);

		// Financial Analytics
		calculateFinancialMetrics(kpi, payments);

		// Performance Analytics
		calculatePerformanceMetrics(kpi, reports);

		// Program Analytics
		calculateProgramMetrics(kpi, attendance);

		// NEW: Additional Analytics
		calculateAttendanceMarkedMetrics(kpi, attendanceMarkedByCoach);
		calculateTraineePerformanceMetrics(kpi, traineeReports);
		calculatePerformanceFeedbackMetrics(kpi, performanceFeedback);

		return kpi;
	}

	// NEW: Calculate attendance marked by coach metrics
	private void calculateAttendanceMarkedMetrics(CoachKpiData kpi, List<AttendanceView> attendanceMarkedByCoach) {
		if (attendanceMarkedByCoach == null || attendanceMarkedByCoach.isEmpty()) {
			kpi.setAttendanceMarkedByCoachBreakdown(new HashMap<>());
			return;
		}

		// Count unique attendance records by avoiding duplicate normalized dates per
		// player
		Set<String> uniquePlayerDateCombinations = attendanceMarkedByCoach.stream()
				.filter(a -> a.getPlayerId() != null && !a.getPlayerId().trim().isEmpty())
				.filter(a -> a.getNormalizedDate() != null)
				.map(a -> a.getPlayerId() + "_" + a.getNormalizedDate().toString()).collect(Collectors.toSet());

		// Unique sessions: based on markedAt (datetime), programId, and branchId
		long uniqueSessions = attendanceMarkedByCoach.stream()
				.filter(a -> a.getMarkedAt() != null && a.getProgramId() != null && a.getBranchId() != null)
				.map(a -> a.getNormalizedDate() + "-" + a.getProgramId() + "-" + a.getMarkedAt().toString()).distinct()
				.count();

		kpi.setAttendanceMarkedByCoach((int) uniqueSessions);

		// Breakdown by status
		Map<String, Long> attendanceMarkedBreakdown = attendanceMarkedByCoach.stream()
				.filter(a -> a.getStatus() != null && !a.getStatus().trim().isEmpty())
				.collect(Collectors.groupingBy(AttendanceView::getStatus, Collectors.counting()));

		kpi.setAttendanceMarkedByCoachBreakdown(attendanceMarkedBreakdown);
	}

	// NEW: Calculate trainee performance reports created by coach
	private void calculateTraineePerformanceMetrics(CoachKpiData kpi,
			List<TraineePerformanceReportView> traineeReports) {
		if (traineeReports == null || traineeReports.isEmpty()) {
			kpi.setPlayerPerformanceReportsByStatus(new HashMap<>());
			return;
		}

		kpi.setPlayerPerformanceReportsCreated(traineeReports.size());

		// Breakdown by report status
		Map<String, Long> reportsByStatus = traineeReports.stream()
				.filter(r -> r != null && r.getReportStatus() != null && !r.getReportStatus().trim().isEmpty())
				.collect(Collectors.groupingBy(TraineePerformanceReportView::getReportStatus, Collectors.counting()));

		kpi.setPlayerPerformanceReportsByStatus(reportsByStatus);

		// Count unique players with reports created by this coach
		Set<String> playersWithReports = traineeReports.stream()
				.filter(r -> r != null && r.getPlayerId() != null && !r.getPlayerId().trim().isEmpty())
				.map(TraineePerformanceReportView::getPlayerId).collect(Collectors.toSet());

		kpi.setPlayersWithPerformanceReports(playersWithReports.size());
	}

	// NEW: Calculate performance feedback received by coach
	private void calculatePerformanceFeedbackMetrics(CoachKpiData kpi,
			List<CoachPerformanceReportView> performanceFeedback) {
		if (performanceFeedback == null || performanceFeedback.isEmpty()) {
			kpi.setPerformanceFeedbackByStatus(new HashMap<>());
			return;
		}

		kpi.setPerformanceFeedbackReceived(performanceFeedback.size());

		// Breakdown by feedback status
		Map<String, Long> feedbackByStatus = performanceFeedback.stream()
				.filter(f -> f != null && f.getReportStatus() != null && !f.getReportStatus().trim().isEmpty())
				.collect(Collectors.groupingBy(CoachPerformanceReportView::getReportStatus, Collectors.counting()));

		kpi.setPerformanceFeedbackByStatus(feedbackByStatus);

		// Monthly feedback trend (last 12 months)
		LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
		Map<String, Long> monthlyFeedbackTrend = performanceFeedback.stream().filter(f -> f.getCreatedOn() != null)
				.filter(f -> f.getCreatedOn().isAfter(twelveMonthsAgo))
				.collect(Collectors.groupingBy(
						f -> f.getCreatedOn().getYear() + "-" + String.format("%02d", f.getCreatedOn().getMonthValue()),
						Collectors.counting()));

		kpi.setMonthlyFeedbackTrend(monthlyFeedbackTrend);
	}

	private void calculateStudentMetrics(CoachKpiData kpi, List<AttendanceView> attendance) {
		if (attendance == null || attendance.isEmpty()) {
			kpi.setStudentsByAgeCategory(new HashMap<>());
			return;
		}

		Set<String> uniqueStudents = attendance.stream().map(AttendanceView::getPlayerId).filter(Objects::nonNull)
				.filter(id -> !id.trim().isEmpty()).collect(Collectors.toSet());

		kpi.setTotalStudentsCoached(uniqueStudents.size());

		// Students by age category
		Map<String, Object> studentsByAgeCategory = attendance.stream()
				.filter(a -> a.getAgeCategory() != null && !a.getAgeCategory().trim().isEmpty())
				.filter(a -> a.getPlayerId() != null && !a.getPlayerId().trim().isEmpty())
				.collect(Collectors.groupingBy(AttendanceView::getAgeCategory, Collectors.mapping(
						AttendanceView::getPlayerId, Collectors.collectingAndThen(Collectors.toSet(), Set::size))));

		kpi.setStudentsByAgeCategory(studentsByAgeCategory);
	}

	private void calculateAttendanceMetrics(CoachKpiData kpi, List<AttendanceView> attendance) {
		if (attendance == null || attendance.isEmpty()) {
			kpi.setAttendanceStatusBreakdown(new HashMap<>());
			return;
		}

		// Unique sessions: based on markedAt (datetime), programId, and branchId
		long uniqueSessions = attendance.stream()
				.filter(a -> a.getMarkedAt() != null && a.getProgramId() != null && a.getBranchId() != null)
				.map(a -> a.getNormalizedDate() + "-" + a.getProgramId() + "-" + a.getMarkedAt().toString()).distinct()
				.count();

		kpi.setTotalUniqueSessions((int) uniqueSessions);

		kpi.setTotalSessions(attendance.size());

		// Attendance rate calculation
		Map<String, Long> statusCount = attendance.stream()
				.filter(a -> a.getStatus() != null && !a.getStatus().trim().isEmpty())
				.collect(Collectors.groupingBy(AttendanceView::getStatus, Collectors.counting()));

		kpi.setAttendanceStatusBreakdown(statusCount);
	}

	private void calculateFinancialMetrics(CoachKpiData kpi, List<PaymentDetailsView> payments) {
		if (payments == null || payments.isEmpty()) {
			kpi.setRevenueByCategory(new HashMap<>());
			kpi.setMonthlyRevenueTrend(new HashMap<>());
			return;
		}

		// Filter successful payments once
		List<PaymentDetailsView> successfulPayments = payments.stream()
				.filter(p -> p != null && "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
				.filter(p -> p.getAmount() != null).collect(Collectors.toList());

		// Total revenue from successful payments
		BigDecimal totalRevenue = successfulPayments.stream().map(PaymentDetailsView::getAmount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		kpi.setTotalRevenueGenerated(totalRevenue);
		kpi.setSuccessfulPayments(successfulPayments.size());

		// Revenue by payment category
		Map<String, BigDecimal> revenueByCategory = successfulPayments.stream()
				.filter(p -> p.getPaymentCategory() != null && !p.getPaymentCategory().trim().isEmpty())
				.collect(Collectors.groupingBy(PaymentDetailsView::getPaymentCategory,
						Collectors.reducing(BigDecimal.ZERO, PaymentDetailsView::getAmount, BigDecimal::add)));

		kpi.setRevenueByCategory(revenueByCategory);

		// Monthly revenue trend
		LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
		Map<String, BigDecimal> monthlyRevenue = successfulPayments.stream().filter(p -> p.getTransactionTime() != null)
				.filter(p -> p.getTransactionTime().isAfter(twelveMonthsAgo))
				.collect(Collectors.groupingBy(
						p -> p.getTransactionTime().getYear() + "-"
								+ String.format("%02d", p.getTransactionTime().getMonthValue()),
						Collectors.reducing(BigDecimal.ZERO, PaymentDetailsView::getAmount, BigDecimal::add)));

		kpi.setMonthlyRevenueTrend(monthlyRevenue);
	}

	private void calculatePerformanceMetrics(CoachKpiData kpi, List<CoachPerformanceReportView> reports) {
		if (reports == null) {
			reports = new ArrayList<>();
		}

		kpi.setTotalPerformanceReports(reports.size());

		if (reports.isEmpty()) {
			kpi.setReportsByStatus(new HashMap<>());
			return;
		}

		// Reports by status
		Map<String, Long> reportsByStatus = reports.stream()
				.filter(r -> r != null && r.getReportStatus() != null && !r.getReportStatus().trim().isEmpty())
				.collect(Collectors.groupingBy(CoachPerformanceReportView::getReportStatus, Collectors.counting()));

		kpi.setReportsByStatus(reportsByStatus);

		// Unique students with reports
		Set<String> studentsWithReports = reports.stream().filter(r -> r != null)
				.map(CoachPerformanceReportView::getPlayerId).filter(Objects::nonNull)
				.filter(id -> !id.trim().isEmpty()).collect(Collectors.toSet());

		kpi.setStudentsWithReports(studentsWithReports.size());
	}

	private void calculateProgramMetrics(CoachKpiData kpi, List<AttendanceView> attendance) {
		if (attendance == null || attendance.isEmpty()) {
			kpi.setStudentsByProgram(new HashMap<>());
			return;
		}

		// Programs coached
		Set<String> programsCoached = attendance.stream().map(AttendanceView::getProgramId).filter(Objects::nonNull)
				.filter(id -> !id.trim().isEmpty()).collect(Collectors.toSet());

		kpi.setProgramsCoached(programsCoached.size());

		// Students by program
		Map<String, Object> studentsByProgram = attendance.stream()
				.filter(a -> a.getProgram() != null && !a.getProgram().trim().isEmpty())
				.filter(a -> a.getPlayerId() != null && !a.getPlayerId().trim().isEmpty())
				.collect(Collectors.groupingBy(AttendanceView::getProgram, Collectors.mapping(
						AttendanceView::getPlayerId, Collectors.collectingAndThen(Collectors.toSet(), Set::size))));

		kpi.setStudentsByProgram(studentsByProgram);

		// Academies
		Set<String> academiesCoached = attendance.stream().map(AttendanceView::getAcademyId).filter(Objects::nonNull)
				.filter(id -> !id.trim().isEmpty()).collect(Collectors.toSet());

		kpi.setAcademiesCoached(academiesCoached.size());
	}

	private <T> List<T> filterByDomain(List<T> items, String academyDomain) {
		if (items == null || items.isEmpty() || academyDomain == null || academyDomain.trim().isEmpty()) {
			return items != null ? items : new ArrayList<>();
		}

		return items.stream().filter(item -> {
			if (item == null)
				return false;
			try {
				Method getDomainUrl = item.getClass().getMethod("getDomainUrl");
				String domainUrl = (String) getDomainUrl.invoke(item);
				return domainUrl != null && academyDomain.equalsIgnoreCase(domainUrl.trim());
			} catch (NoSuchMethodException e) {
				log.debug("getDomainUrl method not found for item: {}", item.getClass().getSimpleName());
				return true; // Include item if method doesn't exist
			} catch (Exception e) {
				log.warn("Could not filter by domain for item: {}, error: {}", item.getClass().getSimpleName(),
						e.getMessage());
				return true; // Include item on error to be safe
			}
		}).collect(Collectors.toList());
	}

	// Enhanced KPI Data Transfer Object
	@Data
	public static class CoachKpiData {
		// Student Metrics
		private int totalStudentsCoached = 0;
		private Map<String, Object> studentsByAgeCategory = new HashMap<>();
		private Map<String, Object> studentsByProgram = new HashMap<>();

		// Attendance Metrics

		private int totalUniqueSessions = 0;
		private int totalSessions = 0;
		private Map<String, Long> attendanceStatusBreakdown = new HashMap<>();

		// Financial Metrics
		private BigDecimal totalRevenueGenerated = BigDecimal.ZERO;
		private int successfulPayments = 0;
		private Map<String, BigDecimal> revenueByCategory = new HashMap<>();
		private Map<String, BigDecimal> monthlyRevenueTrend = new HashMap<>();

		// Performance Metrics
		private int totalPerformanceReports = 0;
		private int studentsWithReports = 0;
		private Map<String, Long> reportsByStatus = new HashMap<>();

		// Program Metrics
		private int programsCoached = 0;
		private int academiesCoached = 0;

		// NEW: Additional Coach-specific Metrics

		// Attendance Marked by Coach
		private int attendanceMarkedByCoach = 0;
		private Map<String, Long> attendanceMarkedByCoachBreakdown = new HashMap<>();

		// Player Performance Reports Created by Coach
		private int playerPerformanceReportsCreated = 0;
		private int playersWithPerformanceReports = 0;
		private Map<String, Long> playerPerformanceReportsByStatus = new HashMap<>();

		// Performance Feedback Received by Coach
		private int performanceFeedbackReceived = 0;
		private Map<String, Long> performanceFeedbackByStatus = new HashMap<>();
		private Map<String, Long> monthlyFeedbackTrend = new HashMap<>();
	}

}