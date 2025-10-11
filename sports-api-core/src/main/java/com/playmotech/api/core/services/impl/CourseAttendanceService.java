package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.gson.reflect.TypeToken;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CourseAttendance;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AttendanceDetailDto;
import com.playmotech.api.core.dto.CourseAttendanceDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.TraineeAttendanceDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.EmailSendException;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CourseAttendanceRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.OrgConfigRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.IAttendanceService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.validation.CoachRoleValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseAttendanceService implements IAttendanceService {
	@Value("${payments-base-url}")
	private String paymentsBaseUrl;

	@Value("${storage.payments-bucket}")
	private String paymentsBucket;

	@Value("${sendgrid.template.payment-reminder}")
	private String paymentReminderTemplateId;

	private final static String ATTENDANCE_NOTIFICATION_TITLE = "Attendance Marked";
	private final static String ATTENDANCE_NOTIFICATION_BODY = "Your attendance has been marked %s for program: %s";
	private final static String NOTIF_CTA_SCREEN = "TRAINEE_ATTENDANCE";
	private static final String TRAINEE_PAYMENT_SCREEN = "TRAINEE_PAYMENTS";
	private static final String COACH_PAYMENT_SCREEN = "TRAINEE_PAYMENT_DETAILS";
	private static final String PAYMENT_REMINDER_TEMPLATE_ID_KEY = "paymentReminderEmailTemplateId";

	private final IMailService mailService;
	private final OrgConfigRepo orgConfigRepo;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final AcademyRepo academyRepo;
	private final CourseRepo courseRepo;
	private final UserProfileRepo userProfileRepo;
	private final CourseAttendanceRepo courseAttendanceRepo;
	private final IUserProfileService userProfileService;
	private final ICourseService courseService;
	private final IPushNotificationService pushNotificationService;
	private final CoachIncentiveService coachIncentiveService;
	private final NewPaymentService paymentService;

	public CourseAttendanceService(final CourseAttendanceRepo courseAttendanceRepo,
			final UserProfileService userProfileService, final ICourseService courseService,
			final IPushNotificationService pushNotificationService, AcademyRepo academyRepo,
			NewPaymentService paymentService, UserProfileRepo userProfileRepo, CourseRepo courseRepo,
			OrgConfigRepo orgConfigRepo, IMailService mailService, CoachIncentiveService coachIncentiveService,
			CoachAcademyMappingRepo coachAcademyMappingRepo) {
		this.mailService = mailService;
		this.orgConfigRepo = orgConfigRepo;
		this.academyRepo = academyRepo;
		this.courseRepo = courseRepo;
		this.userProfileRepo = userProfileRepo;
		this.courseAttendanceRepo = courseAttendanceRepo;
		this.userProfileService = userProfileService;
		this.courseService = courseService;
		this.pushNotificationService = pushNotificationService;
		this.paymentService = paymentService;
		this.coachIncentiveService = coachIncentiveService;
		this.coachAcademyMappingRepo = coachAcademyMappingRepo;
	}

	// @Override
	// public void markAttendance(String academyId, String courseId, String date,
	// Map<String, Boolean> attendance) {
	// Authentication authentication =
	// SecurityContextHolder.getContext().getAuthentication();
	// UserDetail currentUser = (UserDetail) authentication.getPrincipal();
	// Long normalizedDate = Long.parseLong(date.replace("-", ""));

	// Optional<CourseAttendance> courseAttendanceOptional = courseAttendanceRepo
	// .findByCourse_IdAndNormalizedDate(courseId, normalizedDate);
	// Map<String, Boolean> existingAttendance = courseAttendanceOptional.isEmpty()
	// ? new HashMap<>()
	// :
	// AppConstants.GSON.fromJson(courseAttendanceOptional.get().getAttendanceJson(),
	// TypeToken.getParameterized(Map.class, String.class,
	// Boolean.class).getType());
	// compareAndSendNotifications(existingAttendance, attendance, academyId,
	// courseId);
	// if (courseAttendanceOptional.isPresent()) {
	// courseAttendanceOptional.get().setAttendanceJson(AppConstants.GSON.toJson(attendance));
	// courseAttendanceOptional.get().setCreatedOn(Timestamp.from(Instant.now()));
	// courseAttendanceOptional.get()
	// .setCreatedByUserProfile(UserProfile.builder().id(currentUser.getUserId()).build());
	// courseAttendanceRepo.save(courseAttendanceOptional.get());
	// } else {
	// CourseAttendance courseAttendance = new CourseAttendance();
	// courseAttendance.setId(UUID.randomUUID().toString());
	// courseAttendance.setCourse(Course.builder().id(courseId).build());
	// courseAttendance.setNormalizedDate(normalizedDate);
	// courseAttendance.setAttendanceJson(AppConstants.GSON.toJson(attendance));
	// courseAttendance.setAcademy(Academy.builder().id(academyId).build());
	// courseAttendance.setCreatedOn(Timestamp.from(Instant.now()));
	// courseAttendance.setCreatedByUserProfile(UserProfile.builder().id(currentUser.getUserId()).build());
	// courseAttendanceRepo.save(courseAttendance);
	// }

	// }

	// @Override
	// public List<TraineeAttendanceDto> getAttendance(String academyId, String
	// courseId, String date)
	// throws ResourceException {
	// Long normalizedDate = Long.parseLong(date.replace("-", ""));
	// Optional<CourseAttendance> courseAttendances =
	// courseAttendanceRepo.findByCourse_IdAndNormalizedDate(courseId,
	// normalizedDate);
	// List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos =
	// courseService.getEnrolledTrainees(academyId,
	// courseId);
	// Map<String, Boolean> courseAttendancesMap = courseAttendances.isEmpty()
	// || StringUtils.isEmpty(courseAttendances.get().getAttendanceJson()) ? new
	// HashMap<>()
	// : AppConstants.GSON.fromJson(courseAttendances.get().getAttendanceJson(),
	// TypeToken.getParameterized(Map.class, String.class,
	// Boolean.class).getType());
	// List<String> userIds = new ArrayList<>(courseAttendancesMap.keySet());
	// List<String> allEnrolledTrainees = traineeCourseEnrollmentDtos.stream()
	// .map(traineeCourseEnrollmentDto ->
	// traineeCourseEnrollmentDto.getUserProfile().getId()).toList();
	// if (!CollectionUtils.isEmpty(allEnrolledTrainees)) {
	// userIds.addAll(allEnrolledTrainees);
	// }

	// if (CollectionUtils.isEmpty(userIds)) {
	// return new ArrayList<>();
	// }

	// List<UserProfileDto> userProfileDtos =
	// userProfileService.getUserProfileByIds(userIds);

	// List<TraineeAttendanceDto> traineeAttendanceDtos = new ArrayList<>();
	// for (UserProfileDto userProfileDto : userProfileDtos) {
	// TraineeAttendanceDto traineeAttendanceDto = new TraineeAttendanceDto();
	// traineeAttendanceDto.setTraineeUserId(userProfileDto.getId());
	// traineeAttendanceDto.setName(userProfileDto.getDisplayName());
	// traineeAttendanceDto.setPhoneNumber(userProfileDto.getPhoneNumber());
	// traineeAttendanceDto.setAttended(courseAttendancesMap.containsKey(userProfileDto.getId())
	// && courseAttendancesMap.get(userProfileDto.getId()));
	// traineeAttendanceDto.setTraineeProfilePic(userProfileDto.getProfilePictureUrl());
	// Optional<CourseAttendanceDto> enrolledCourses =
	// getAttendanceByTrainee(academyId, userProfileDto.getId())
	// .stream()
	// .filter(courseAttendanceDto ->
	// courseAttendanceDto.getCourseId().equalsIgnoreCase(courseId))
	// .findFirst();
	// if (enrolledCourses.isPresent()) {
	// traineeAttendanceDto.setTotalSessions(enrolledCourses.get().getAttendance().size());
	// traineeAttendanceDto.setAttendedSessions((int)
	// enrolledCourses.get().getAttendance().values().stream()
	// .filter(Boolean::booleanValue).count());
	// }
	// traineeAttendanceDtos.add(traineeAttendanceDto);
	// }
	// return traineeAttendanceDtos;
	// }

	// @Override
	// public List<CourseAttendanceDto> getAttendanceByTrainee(String academyId,
	// String traineeUserId)
	// throws ResourceException {
	// List<CourseAttendanceDto> courseAttendanceDtos = new ArrayList<>();
	// List<CourseDto> enrolledCourses = courseService.getEnrolledCourses(academyId,
	// traineeUserId, null, null);

	// if (CollectionUtils.isEmpty(enrolledCourses)) {
	// return List.of();
	// }

	// for (CourseDto courseDto : enrolledCourses) {
	// List<Long> dates = getDatesTillNow(courseDto.getSchedule());
	// if (CollectionUtils.isEmpty(dates)) {
	// continue;
	// }

	// Map<Long, CourseAttendance> courseAttendances = courseAttendanceRepo
	// .findByCourse_IdAndNormalizedDateIn(courseDto.getId(), dates).stream()
	// .map(courseAttendance -> Map.entry(courseAttendance.getNormalizedDate(),
	// courseAttendance))
	// .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	// // if (CollectionUtils.isEmpty(courseAttendances))
	// // continue;

	// String startDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
	// ? courseDto.getSchedule().getCustomDates().get(0)
	// : courseDto.getSchedule().getStartDate();
	// String endDate = StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
	// ?
	// courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size()
	// - 1)
	// : courseDto.getSchedule().getEndDate();
	// CourseAttendanceDto courseAttendanceDto = new CourseAttendanceDto();
	// courseAttendanceDto.setCourseId(courseDto.getId());
	// courseAttendanceDto.setCourseTitle(courseDto.getTitle());
	// courseAttendanceDto.setCourseIconUrl(courseDto.getIconUrl());
	// courseAttendanceDto.setStartDate(startDate);
	// courseAttendanceDto.setEndDate(endDate);
	// for (Long date : dates) {
	// Map<String, Boolean> attendanceMap = courseAttendances.containsKey(date)
	// && StringUtils.isNotEmpty(courseAttendances.get(date).getAttendanceJson())
	// ? AppConstants.GSON.fromJson(courseAttendances.get(date).getAttendanceJson(),
	// TypeToken.getParameterized(Map.class, String.class, Boolean.class).getType())
	// : new HashMap<>();
	// if (courseAttendanceDto.getAttendance() == null) {
	// courseAttendanceDto.setAttendance(new HashMap<>());
	// }
	// if (courseAttendances.containsKey(date)) {
	// courseAttendanceDto.getAttendance().put(DateTimeUtils.convertLongDateToString(date),
	// attendanceMap.containsKey(traineeUserId) &&
	// attendanceMap.get(traineeUserId));
	// } else {
	// courseAttendanceDto.getAttendance().put(DateTimeUtils.convertLongDateToString(date),
	// false);
	// }
	// }
	// courseAttendanceDtos.add(courseAttendanceDto);
	// }
	// return courseAttendanceDtos;
	// }

	// public List<CourseAttendanceDto> getAttendanceBetweenDates(String academyId,
	// String startDate, String endDate)
	// throws ResourceException {
	// List<CourseAttendanceDto> courseAttendanceDtos = new ArrayList<>();
	// List<CourseAttendance> courseAttendances = courseAttendanceRepo
	// .findByAcademy_IdAndNormalizedDateGreaterThanEqualAndNormalizedDateLessThanEqual(academyId,
	// Long.parseLong(startDate.replace("-", "")),
	// Long.parseLong(endDate.replace("-", "")));
	// List<CourseAttendance> courseAttendances1 =
	// courseAttendances.stream().filter(
	// courseAttendance -> courseAttendance.getNormalizedDate() >=
	// Long.parseLong(startDate.replace("-", ""))
	// && courseAttendance.getNormalizedDate() <=
	// Long.parseLong(endDate.replace("-", "")))
	// .toList();
	// Map<Long, List<CourseAttendance>> courseAttendanceByDate =
	// courseAttendances1.stream()
	// .map(courseAttendance -> Map.entry(courseAttendance.getNormalizedDate(),
	// courseAttendance))
	// .collect(Collectors.groupingBy(Map.Entry::getKey,
	// Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

	// // List<CourseDto> courseDtos = courseService.getCourses(academyId,
	// // courseAttendances.stream().map(CourseAttendance::getCourseId).toList(),
	// null,
	// // null);
	// List<Course> courses =
	// courseAttendances.stream().map(CourseAttendance::getCourse).toList();

	// if (CollectionUtils.isEmpty(courses)) {
	// return List.of();
	// }

	// List<CourseDto> courseDtos = courseService.adaptCourseDtos(courses);

	// Map<String, List<TraineeCourseEnrollment>> courseTraineeMap =
	// courseService.getEnrollmentsByAcademyId(academyId)
	// .stream()
	// .collect(Collectors.groupingBy(traineeCourseEnrollment ->
	// traineeCourseEnrollment.getCourse().getId()));

	// for (CourseDto courseDto : courseDtos) {
	// List<Long> datesTillNow = getDatesTillNow(courseDto.getSchedule());
	// List<Long> dates = datesTillNow.stream().filter(date -> date >=
	// Long.parseLong(startDate.replace("-", ""))
	// && date <= Long.parseLong(endDate.replace("-", ""))).toList();
	// if (CollectionUtils.isEmpty(dates)) {
	// continue;
	// }
	// String courseStartDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
	// ? courseDto.getSchedule().getCustomDates().get(0)
	// : courseDto.getSchedule().getStartDate();
	// String courseEndDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
	// ?
	// courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size()
	// - 1)
	// : courseDto.getSchedule().getEndDate();

	// for (TraineeCourseEnrollment traineeCourseEnrollment :
	// courseTraineeMap.get(courseDto.getId())) {
	// CourseAttendanceDto courseAttendanceDto = new CourseAttendanceDto();
	// courseAttendanceDto.setCourseId(courseDto.getId());
	// courseAttendanceDto.setCourseTitle(courseDto.getTitle());
	// courseAttendanceDto.setCourseIconUrl(courseDto.getIconUrl());
	// courseAttendanceDto.setStartDate(courseStartDate);
	// courseAttendanceDto.setEndDate(courseEndDate);
	// for (Long date : dates) {
	// if (courseAttendanceDto.getAttendance() == null) {
	// courseAttendanceDto.setAttendance(new HashMap<>());
	// }
	// Optional<CourseAttendance> courseAttendanceOptional =
	// courseAttendanceByDate.containsKey(date)
	// ? courseAttendanceByDate.get(date).stream()
	// .filter(courseAttendance -> courseAttendance.getCourse().getId()
	// .equalsIgnoreCase(courseDto.getId()))
	// .findFirst()
	// : Optional.empty();
	// if (courseAttendanceOptional.isPresent()) {
	// Map<String, Boolean> attendance = StringUtils
	// .isNotEmpty(courseAttendanceOptional.get().getAttendanceJson())
	// ?
	// AppConstants.GSON.fromJson(courseAttendanceOptional.get().getAttendanceJson(),
	// TypeToken.getParameterized(Map.class, String.class, Boolean.class)
	// .getType())
	// : new HashMap<>();
	// courseAttendanceDto.getAttendance().put(DateTimeUtils.convertLongDateToString(date),
	// attendance.containsKey(traineeCourseEnrollment.getTraineeUserProfile().getId())
	// && attendance.get(traineeCourseEnrollment.getTraineeUserProfile().getId()));
	// } else {
	// courseAttendanceDto.getAttendance().put(DateTimeUtils.convertLongDateToString(date),
	// false);
	// }
	// }
	// courseAttendanceDtos.add(courseAttendanceDto);
	// }
	// }
	// return courseAttendanceDtos;
	// }

	public String getConfigValue(String key, List<OrganisationConfig> configs) {
		if (configs == null)
			return null;
		return configs.stream().filter(cfg -> key.equals(cfg.getKey())).map(OrganisationConfig::getValue).findFirst()
				.orElse(null);
	}

	private boolean getBooleanConfigValue(String key, List<OrganisationConfig> configs) {
		if (configs == null || configs.isEmpty()) {
			return false;
		}

		// Find the config with the matching key
		Optional<OrganisationConfig> configOptional = configs.stream().filter(config -> key.equals(config.getKey()))
				.findFirst();

		if (configOptional.isEmpty()) {
			return false;
		}

		Object value = configOptional.get().getValue();
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0; // treat non-zero as true
		} else if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}

		return false;
	}

	private double getNumericConfigValue(String key, Map<String, Object> configs) {
		if (configs == null || !configs.containsKey(key)) {
			return 0d;
		}

		Object value = configs.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue(); // ✅ safe conversion (handles Long, Integer, Double)
		} else if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (NumberFormatException e) {
				return 0d;
			}
		}
		return 0d;
	}

	@Override
	public ServiceResponse markAttendance(String academyId, String courseId, String date,
			Map<String, AttendanceDetailDto> attendance) {
		// Call V2 which has the full business logic
		ServiceResponse response = markAttendanceV2(academyId, courseId, date, attendance);

		// If successful, extract and return only the players list (original behavior)
		if (response.getStatus() == 201 && response.getBody() != null) {
			Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
			List<Map<String, Object>> players = (List<Map<String, Object>>) responseBody.get("players");
			return ResponseBuilder.success(players, response.getMessage(), HttpStatus.CREATED);
		}
		// For errors, return as-is
		return response;
	}

	@Override
	public ServiceResponse markAttendanceV2(String academyId, String courseId, String date,
			Map<String, AttendanceDetailDto> attendance) {
		try {
			log.info("📌 Starting attendance marking | academyId={}, courseId={}, date={}, totalEntries={}", academyId,
					courseId, date, attendance.size());

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			log.info("Current user performing operation: {}", currentUser.getUserId());

			Long normalizedDate = Long.parseLong(date.replace("-", ""));
			log.info("Normalized date parsed as: {}", normalizedDate);

			Optional<Academy> academy = academyRepo.findById(academyId);
			if (academy.isEmpty()) {
				log.info("❌ Academy not found for academyId={}", academyId);
				return ResponseBuilder.error("Academy not found", HttpStatus.NOT_FOUND);
			}

			Organisation org = academy.get().getOrg();
			boolean checkPendingDue = false;

			if (org != null) {
				checkPendingDue = getBooleanConfigValue("checkPendingDue", org.getConfigs());
				log.info("checkPendingDue={} for orgId={}", checkPendingDue, org.getId());
			} else {
				log.info("⚠️ Organisation is null for academyId={}, skipping pending dues check", academyId);
			}

			List<Map<String, Object>> playersWithPendingDues = new ArrayList<>();

			if (checkPendingDue) {
				log.info("🔍 Checking pending dues for present students...");
				for (Map.Entry<String, AttendanceDetailDto> entry : attendance.entrySet()) {
					String userId = entry.getKey();
					AttendanceDetailDto attendanceDetail = entry.getValue();

					if (attendanceDetail.isAttendance()) {
						log.info("Checking dues for userId={} (marked present)", userId);
						try {
							ServiceResponse duesResponse = paymentService.getDues(userId, courseId);
							log.info("Received duesResponse for userId={} | status={}", userId,
									duesResponse.getStatus());

							if (duesResponse.getStatus() == 200 && duesResponse.getBody() != null) {
								Map<String, Object> responseBody = (Map<String, Object>) duesResponse.getBody();
								Map<String, Object> duesData = responseBody;

								if (responseBody.containsKey("body") && responseBody.get("body") instanceof Map) {
									duesData = (Map<String, Object>) responseBody.get("body");
								}

								Double registrationFee = convertToDouble(duesData.get("registrationFee"));
								Double courseFee = convertToDouble(duesData.get("courseFee"));

								log.info("UserId={} dues → registrationFee={}, courseFee={}", userId, registrationFee,
										courseFee);

								if ((registrationFee != null && registrationFee > 0)
										|| (courseFee != null && courseFee > 0)) {
									Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
									if (userProfile.isPresent()) {
										UserProfile student = userProfile.get();

										Map<String, Object> studentDuesInfo = new HashMap<>();
										studentDuesInfo.put("name",
												student.getDisplayName() != null ? student.getDisplayName()
														: "Unknown");
										studentDuesInfo.put("phoneNumber",
												student.getPhoneNumber() != null ? student.getPhoneNumber() : "");
										studentDuesInfo.put("email",
												student.getEmailId() != null ? student.getEmailId() : "");
										studentDuesInfo.put("registrationFeePending",
												registrationFee != null ? registrationFee : 0.0);
										studentDuesInfo.put("courseFeePending", courseFee != null ? courseFee : 0.0);
										studentDuesInfo.put("totalDuesPending",
												(registrationFee != null ? registrationFee : 0.0)
														+ (courseFee != null ? courseFee : 0.0));
										studentDuesInfo.put("userId", userId);

										playersWithPendingDues.add(studentDuesInfo);
										log.info(
												"⚠️ Student {} (userId={}) has pending dues. Registration: ₹{}, Course: ₹{}",
												student.getDisplayName(), userId, registrationFee, courseFee);
									}

									sendPaymentReminderNotifications(userId, academyId, courseId, duesData);
								}
							}
						} catch (Exception e) {
							log.info("⚠️ Skipping dues check for userId={} due to error: {}", userId, e.getMessage());
						}
					}
				}
			}

			// Load or create attendance record
			Optional<CourseAttendance> courseAttendanceOptional = courseAttendanceRepo
					.findByCourse_IdAndNormalizedDate(courseId, normalizedDate);
			log.info("Existing attendance record found? {}", courseAttendanceOptional.isPresent());

			Map<String, AttendanceDetailDto> existingAttendance = courseAttendanceOptional.isEmpty() ? new HashMap<>()
					: AppConstants.GSON.fromJson(courseAttendanceOptional.get().getAttendanceJson(),
							new TypeToken<Map<String, AttendanceDetailDto>>() {
							}.getType());

			Map<String, Boolean> existingAttendanceMap = existingAttendance.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().isAttendance()));
			Map<String, Boolean> newAttendanceMap = attendance.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().isAttendance()));

			log.info("Comparing old vs new attendance states for notifications...");
			compareAndSendNotifications(existingAttendanceMap, newAttendanceMap, academyId, courseId);

			// Save new/updated record
			CourseAttendance attendanceRecord = courseAttendanceOptional.orElseGet(CourseAttendance::new);
			String attendanceId = attendanceRecord.getId() != null ? attendanceRecord.getId() : null;

			if (attendanceRecord.getId() == null) {
				log.info("Creating new CourseAttendance record for courseId={}, date={}", courseId, normalizedDate);
				attendanceRecord.setId(UUID.randomUUID().toString());
				attendanceRecord.setCourse(Course.builder().id(courseId).build());
				attendanceRecord.setNormalizedDate(normalizedDate);
				attendanceRecord.setAcademy(Academy.builder().id(academyId).build());
			}

			attendanceRecord.setAttendanceJson(AppConstants.GSON.toJson(attendance));
			attendanceRecord.setCreatedOn(Timestamp.from(Instant.now()));
			attendanceRecord.setCreatedByUserProfile(UserProfile.builder().id(currentUser.getUserId()).build());

			CourseAttendance savedAttendance = courseAttendanceRepo.save(attendanceRecord);

			Optional<CoachAcademyMapping> coachAcademyMappingOptional = coachAcademyMappingRepo
					.findByAcademy_IdAndCoachUserProfile_Id(academyId, currentUser.getUserId());

			if (coachAcademyMappingOptional.isPresent() &&
					coachAcademyMappingOptional.get().getRoleId() != null &&
					CoachRoleValidator.ACCEPTABLE_ROLE_IDS.contains(coachAcademyMappingOptional.get().getRoleId())) {
				log.info(
						"[CourseAttendanceService] The user is indeed a coach in the provided academy!!! Rewarding the coach with some points :)");
				CompletableFuture.runAsync(() -> {
					try {
						rewardCoach(currentUser.getUserId(), attendance, savedAttendance);
					} catch (ResourceException e) {
						log.error("Unable to reward coach: coachId: {}", currentUser.getUserId());
						log.error("[CourseAttendanceService] Check the (coach_point_transaction_error) table");
					}
				});
			} else {
				log.warn(
						"No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
			}

			log.info("✅ Attendance marking completed successfully for academyId={}, courseId={}, date={}", academyId,
					courseId, date);

			// V2 returns attendanceId + players
			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("attendanceId", attendanceId);
			responseBody.put("players", playersWithPendingDues);
			return ResponseBuilder.success(responseBody, "Attendance marked successfully", HttpStatus.CREATED);

		} catch (NumberFormatException e) {
			log.info("❌ Invalid date format received: {}", date);
			return ResponseBuilder.error("Invalid date format", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.info("❌ Unexpected error while marking attendance | academyId={}, courseId={}, date={}, error={}",
					academyId, courseId, date, e.getMessage());
			return ResponseBuilder.error("Failed to mark attendance: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void rewardCoach(String coachUserId, Map<String, AttendanceDetailDto> attendance,
			CourseAttendance savedAttendance) throws ResourceException {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("courseAttendanceId", savedAttendance.getId());
		metadata.put("academyId", savedAttendance.getAcademy() != null ? savedAttendance.getAcademy().getId() : null);
		metadata.put("courseId", savedAttendance.getCourse() != null ? savedAttendance.getCourse().getId() : null);
		metadata.put("createdOn",
				savedAttendance.getCreatedOn() != null ? savedAttendance.getCreatedOn().toString() : null);
		metadata.put("normalizedDate",
				savedAttendance.getNormalizedDate() != null ? savedAttendance.getNormalizedDate() : null);

		coachIncentiveService.awardPoints(coachUserId,
				IncentiveSourceType.ATTENDANCE,
				savedAttendance.getId(),
				savedAttendance.getAcademy().getId(),
				metadata);
	}

	/**
	 * Safely converts various Number types to Double Handles Long, Integer, Float,
	 * Double, BigDecimal, etc.
	 */
	private Double convertToDouble(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}

		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (NumberFormatException e) {
				log.warn("Could not parse string value to Double: {}", value);
				return null;
			}
		}

		log.warn("Unexpected type for numeric value: {} (type: {})", value, value.getClass().getSimpleName());
		return null;
	}

	private void sendPaymentReminderNotifications(String userId, String academyId, String courseId,
			Map<String, Object> duesData) {
		try {
			log.info("Preparing payment reminder notification for userId={}", userId);

			Optional<UserProfile> studentProfile = userProfileRepo.findById(userId);
			if (studentProfile.isEmpty()) {
				log.info("Student profile not found for userId={}", userId);
				return;
			}

			UserProfile student = studentProfile.get();
			Optional<Academy> academy = academyRepo.findById(academyId);
			Optional<Course> course = courseRepo.findById(courseId);

			String studentName = student.getDisplayName();
			String academyName = academy.map(Academy::getName).orElse("Academy");
			String courseName = course.map(Course::getTitle).orElse("Course");

			// Fixed: Use safe conversion method instead of direct casting
			Double registrationFee = convertToDouble(duesData.get("registrationFee"));
			Double courseFee = convertToDouble(duesData.get("courseFee"));
			Double totalDues = (registrationFee != null ? registrationFee : 0.0)
					+ (courseFee != null ? courseFee : 0.0);

			log.info("UserId={} total pending dues={}", userId, totalDues);

			String title = "Payment Reminder";
			String message = String.format(
					"You attended class at %s but have pending dues of ₹%.2f for %s. Please clear the payment.",
					academyName, totalDues, courseName);

			Map<String, String> extraArgs = new HashMap<>();
			extraArgs.put("traineeUserId", student.getId());
			extraArgs.put("academyId", academyId);
			extraArgs.put("totalDues", totalDues.toString());
			extraArgs.put("studentId", student.getId());
			extraArgs.put("studentName", studentName);
			// Fixed: Add email to extraArgs for sendEmailReminder
			if (student.getEmailId() != null) {
				extraArgs.put("playerEmailId", student.getEmailId());
			}

			log.info("Sending push + email notifications for {}", studentName);

			if (org.springframework.util.StringUtils.hasText(student.getAndroidFcmPushToken())) {
				log.info("Sending push notification to {}", studentName);
				pushNotificationService.sendMessageToPushToken(student.getAndroidFcmPushToken(),
						NotificationType.LIVE_NOTIFICATION, title, message, TRAINEE_PAYMENT_SCREEN, CtaType.SCREEN,
						extraArgs);

				pushNotificationService.addNotification(Collections.singletonList(student.getId()), message,
						CtaType.SCREEN, TRAINEE_PAYMENT_SCREEN, extraArgs);
			} else {
				log.info("No FCM token for {}, skipping push", studentName);
			}

			// Fixed: Create proper PaymentReminderDto with required data
			PaymentReminderDto paymentReminderDto = new PaymentReminderDto();
			paymentReminderDto.setTraineeUserId(userId);
			paymentReminderDto.setAcademyId(academyId);
			paymentReminderDto.setAmount(String.format("%.2f", totalDues));

			sendEmailReminder(paymentReminderDto, extraArgs, title, message, "");
		} catch (Exception e) {
			log.info("Error sending reminder for userId={} | {}", userId, e.getMessage());
		}
	}

	private void sendEmailReminder(PaymentReminderDto paymentReminderDto, Map<String, String> extraArgs, String title,
			String message, String daysText) {

		String recipientEmail = extraArgs.get("playerEmailId");

		if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
			log.info("No email address provided for trainee: {}", paymentReminderDto.getTraineeUserId());
			return;
		}

		try {
			// Get email template ID
			String templateId = getEmailTemplateId(paymentReminderDto.getAcademyId());

			if (templateId == null || templateId.trim().isEmpty()) {
				log.warn("Email template not configured for academy: {}", paymentReminderDto.getAcademyId());
				return;
			}

			// Build template data
			Map<String, String> templateData = new HashMap<>(extraArgs);
			templateData.put("amount", paymentReminderDto.getAmount());
			templateData.put("days", daysText);
			templateData.put("title", title);
			templateData.put("message", message);

			log.info("Sending email reminder to {} using template {}", recipientEmail, templateId);
			mailService.sendTemplateEmail(recipientEmail, title, templateId, templateData);
			log.info("Payment reminder email sent successfully to {}", recipientEmail);

		} catch (EmailSendException e) {
			log.error("Failed to send email reminder to {}: {}", recipientEmail, e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error sending email reminder to {}: {}", recipientEmail, e.getMessage());
		}
	}

	private String getEmailTemplateId(String academyId) {
		try {
			if (academyId == null || academyId.trim().isEmpty()) {
				log.warn("Academy ID is null or empty, using default template");
				return paymentReminderTemplateId;
			}

			Academy academy = academyRepo.findById(academyId).orElse(null);

			if (academy != null && academy.getOrg() != null) {
				Optional<OrganisationConfig> orgConfigOpt = orgConfigRepo.findByOrgIdAndKey(academy.getOrg().getId(),
						PAYMENT_REMINDER_TEMPLATE_ID_KEY);

				if (orgConfigOpt.isPresent() && StringUtils.isNotEmpty(orgConfigOpt.get().getValue())) {
					return orgConfigOpt.get().getValue();
				}
			}

			if (StringUtils.isEmpty(paymentReminderTemplateId)) {
				log.warn("Default payment reminder template ID not configured in properties");
			}

			return paymentReminderTemplateId;

		} catch (Exception e) {
			log.error("Error retrieving email template ID for academy {}: {}", academyId, e.getMessage());
			return paymentReminderTemplateId;
		}
	}

	@Override
	public List<TraineeAttendanceDto> getAttendance(String academyId, String courseId, String date)
			throws ResourceException {
		Long normalizedDate = Long.parseLong(date.replace("-", ""));
		Optional<CourseAttendance> courseAttendances = courseAttendanceRepo.findByCourse_IdAndNormalizedDate(courseId,
				normalizedDate);

		List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos = courseService.getEnrolledTrainees(academyId,
				courseId);

		LocalDate localDate = LocalDate.parse(date);
		traineeCourseEnrollmentDtos = traineeCourseEnrollmentDtos.stream()
				.filter(each -> each.getJoiningDate().isBefore(localDate) || each.getJoiningDate().isEqual(localDate))
				.toList();
		Map<String, AttendanceDetailDto> courseAttendancesMap = courseAttendances.isEmpty()
				|| StringUtils.isEmpty(courseAttendances.get().getAttendanceJson()) ? new HashMap<>()
						: AppConstants.GSON.fromJson(courseAttendances.get().getAttendanceJson(),
								new TypeToken<Map<String, AttendanceDetailDto>>() {
								}.getType());

		List<String> userIds = new ArrayList<>(courseAttendancesMap.keySet());

		List<String> allEnrolledTrainees = traineeCourseEnrollmentDtos.stream()
				.map(trainee -> trainee.getUserProfile().getId()).toList();

		if (!CollectionUtils.isEmpty(allEnrolledTrainees)) {
			userIds.addAll(allEnrolledTrainees);
		}

		if (CollectionUtils.isEmpty(userIds)) {
			return new ArrayList<>();
		}

		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds);

		List<TraineeAttendanceDto> traineeAttendanceDtos = new ArrayList<>();
		for (UserProfileDto userProfileDto : userProfileDtos) {
			TraineeAttendanceDto dto = new TraineeAttendanceDto();
			dto.setTraineeUserId(userProfileDto.getId());
			dto.setName(userProfileDto.getDisplayName());
			dto.setPhoneNumber(userProfileDto.getPhoneNumber());
			dto.setTraineeProfilePic(userProfileDto.getProfilePictureUrl());

			Optional<TraineeCourseEnrollmentDto> matchedEnrollment = traineeCourseEnrollmentDtos.stream()
					.filter(each -> each.getTraineeUserId().equals(userProfileDto.getId())).findFirst();

			matchedEnrollment.ifPresent(enrollment -> dto.setJoiningDate(enrollment.getJoiningDate()));

			AttendanceDetailDto detail = courseAttendancesMap.get(userProfileDto.getId());
			dto.setAttended(detail != null && detail.isAttendance());
			dto.setStartTime(detail != null ? detail.getStartTime() : null);
			dto.setEndTime(detail != null ? detail.getEndTime() : null);

			Optional<CourseAttendanceDto> enrolledCourse = getAttendanceByTrainee(academyId, userProfileDto.getId())
					.stream().filter(ca -> ca.getCourseId().equalsIgnoreCase(courseId)).findFirst();

			if (enrolledCourse.isPresent()) {
				dto.setTotalSessions(enrolledCourse.get().getAttendance().size());
				dto.setAttendedSessions((int) enrolledCourse.get().getAttendance().values().stream()
						.filter(Boolean::booleanValue).count());
			}

			traineeAttendanceDtos.add(dto);
		}

		return traineeAttendanceDtos;
	}

	@Override
	public List<CourseAttendanceDto> getAttendanceByTrainee(String academyId, String traineeUserId)
			throws ResourceException {

		List<CourseAttendanceDto> courseAttendanceDtos = new ArrayList<>();
		List<CourseDto> enrolledCourses = courseService.getEnrolledCourses(academyId, traineeUserId, null, null);

		if (CollectionUtils.isEmpty(enrolledCourses)) {
			return List.of();
		}

		for (CourseDto courseDto : enrolledCourses) {
			List<Long> dates = getDatesTillNow(courseDto.getSchedule());
			if (CollectionUtils.isEmpty(dates)) {
				continue;
			}

			Map<Long, CourseAttendance> courseAttendances = courseAttendanceRepo
					.findByCourse_IdAndNormalizedDateIn(courseDto.getId(), dates).stream()
					.map(courseAttendance -> Map.entry(courseAttendance.getNormalizedDate(), courseAttendance))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			String startDate = StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
					? courseDto.getSchedule().getCustomDates().get(0)
					: courseDto.getSchedule().getStartDate();
			String endDate = StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
					? courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size() - 1)
					: courseDto.getSchedule().getEndDate();

			CourseAttendanceDto courseAttendanceDto = new CourseAttendanceDto();
			courseAttendanceDto.setCourseId(courseDto.getId());
			courseAttendanceDto.setCourseTitle(courseDto.getTitle());
			courseAttendanceDto.setCourseIconUrl(courseDto.getIconUrl());
			courseAttendanceDto.setStartDate(startDate);
			courseAttendanceDto.setEndDate(endDate);

			for (Long date : dates) {
				Map<String, AttendanceDetailDto> attendanceMap = courseAttendances.containsKey(date)
						&& StringUtils.isNotEmpty(courseAttendances.get(date).getAttendanceJson())
								? AppConstants.GSON.fromJson(courseAttendances.get(date).getAttendanceJson(),
										new TypeToken<Map<String, AttendanceDetailDto>>() {
										}.getType())
								: new HashMap<>();

				if (courseAttendanceDto.getAttendance() == null) {
					courseAttendanceDto.setAttendance(new HashMap<>());
				}

				boolean isAttended = attendanceMap.containsKey(traineeUserId)
						&& attendanceMap.get(traineeUserId).isAttendance();

				courseAttendanceDto.getAttendance().put(DateTimeUtils.convertLongDateToString(date), isAttended);
			}

			courseAttendanceDtos.add(courseAttendanceDto);
		}

		return courseAttendanceDtos;
	}

	@Override
	public List<CourseAttendanceDto> getAttendanceBetweenDates(String academyId, String startDate, String endDate)
			throws ResourceException {

		List<CourseAttendanceDto> courseAttendanceDtos = new ArrayList<>();

		List<CourseAttendance> courseAttendances = courseAttendanceRepo
				.findByAcademy_IdAndNormalizedDateGreaterThanEqualAndNormalizedDateLessThanEqual(academyId,
						Long.parseLong(startDate.replace("-", "")), Long.parseLong(endDate.replace("-", "")));

		List<CourseAttendance> filteredCourseAttendances = courseAttendances.stream()
				.filter(ca -> ca.getNormalizedDate() >= Long.parseLong(startDate.replace("-", ""))
						&& ca.getNormalizedDate() <= Long.parseLong(endDate.replace("-", "")))
				.toList();

		Map<Long, List<CourseAttendance>> courseAttendanceByDate = filteredCourseAttendances.stream()
				.map(ca -> Map.entry(ca.getNormalizedDate(), ca)).collect(Collectors.groupingBy(Map.Entry::getKey,
						Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

		List<Course> courses = courseAttendances.stream().map(CourseAttendance::getCourse).toList();

		if (CollectionUtils.isEmpty(courses)) {
			return List.of();
		}

		List<CourseDto> courseDtos = courseService.adaptCourseDtos(courses);

		Map<String, List<TraineeCourseEnrollment>> courseTraineeMap = courseService.getEnrollmentsByAcademyId(academyId)
				.stream().collect(Collectors.groupingBy(e -> e.getCourse().getId()));

		for (CourseDto courseDto : courseDtos) {
			List<Long> datesTillNow = getDatesTillNow(courseDto.getSchedule());
			List<Long> relevantDates = datesTillNow.stream()
					.filter(date -> date >= Long.parseLong(startDate.replace("-", ""))
							&& date <= Long.parseLong(endDate.replace("-", "")))
					.toList();

			if (CollectionUtils.isEmpty(relevantDates)) {
				continue;
			}

			String courseStartDate = StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
					? courseDto.getSchedule().getCustomDates().get(0)
					: courseDto.getSchedule().getStartDate();

			String courseEndDate = StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
					? courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size() - 1)
					: courseDto.getSchedule().getEndDate();

			for (TraineeCourseEnrollment enrollment : courseTraineeMap.get(courseDto.getId())) {
				CourseAttendanceDto dto = new CourseAttendanceDto();
				dto.setCourseId(courseDto.getId());
				dto.setCourseTitle(courseDto.getTitle());
				dto.setCourseIconUrl(courseDto.getIconUrl());
				dto.setStartDate(courseStartDate);
				dto.setEndDate(courseEndDate);
				dto.setAttendance(new HashMap<>());

				for (Long date : relevantDates) {
					Optional<CourseAttendance> courseAttendanceOptional = courseAttendanceByDate.containsKey(date)
							? courseAttendanceByDate.get(date).stream()
									.filter(ca -> ca.getCourse().getId().equalsIgnoreCase(courseDto.getId()))
									.findFirst()
							: Optional.empty();

					String traineeId = enrollment.getTraineeUserProfile().getId();

					if (courseAttendanceOptional.isPresent()) {
						Map<String, AttendanceDetailDto> attendanceMap = StringUtils
								.isNotEmpty(courseAttendanceOptional.get().getAttendanceJson())
										? AppConstants.GSON.fromJson(courseAttendanceOptional.get().getAttendanceJson(),
												new TypeToken<Map<String, AttendanceDetailDto>>() {
												}.getType())
										: new HashMap<>();

						boolean isAttended = attendanceMap.containsKey(traineeId)
								&& attendanceMap.get(traineeId).isAttendance();

						dto.getAttendance().put(DateTimeUtils.convertLongDateToString(date), isAttended);
					} else {
						dto.getAttendance().put(DateTimeUtils.convertLongDateToString(date), false);
					}
				}

				courseAttendanceDtos.add(dto);
			}
		}

		return courseAttendanceDtos;
	}

	private List<Long> getDatesTillNow(ScheduleDto schedule) {
		if (schedule.getType() == ScheduleType.REGULAR) {
			return DateTimeUtils
					.dateBetween(schedule.getStartDate(), schedule.getEndDate(), true, true, false,
							schedule.getTimezone())
					.stream().map(date -> Long.parseLong(date.replace("-", ""))).toList();
		} else if (schedule.getType() == ScheduleType.CUSTOM) {
			return schedule.getCustomDates().stream().map(date -> Long.parseLong(date.replace("-", ""))).toList();
		} else if (schedule.getType() == ScheduleType.WEEKDAYS) {
			return DateTimeUtils
					.dateBetween(schedule.getStartDate(), schedule.getEndDate(), true, true, true,
							schedule.getTimezone())
					.stream().map(date -> Long.parseLong(date.replace("-", ""))).toList();
		} else if (schedule.getType() == ScheduleType.WEEKENDS) {
			return DateTimeUtils
					.weekendsBetween(schedule.getStartDate(), schedule.getEndDate(), true, true, schedule.getTimezone())
					.stream().map(date -> Long.parseLong(date.replace("-", ""))).toList();
		} else {
			return List.of();
		}
	}

	private void compareAndSendNotifications(Map<String, Boolean> existingAttendance,
			Map<String, Boolean> newAttendance, String academyId, String courseId) {
		CompletableFuture.runAsync(() -> {
			for (Map.Entry<String, Boolean> entry : newAttendance.entrySet()) {
				if (!existingAttendance.containsKey(entry.getKey()) || (existingAttendance.containsKey(entry.getKey())
						&& existingAttendance.get(entry.getKey()) != entry.getValue())) {
					sendNotification(entry.getKey(), academyId, courseId, !entry.getValue());
				}
			}
		});

	}

	private void sendNotification(String userId, String academyId, String courseId, boolean markedAbsent) {
		CompletableFuture.runAsync(() -> {
			try {
				UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);
				if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
					return;
				}
				CourseDto courseDto = courseService.getCourse(academyId, courseId);
				Map<String, String> extraArgs = new HashMap<>();
				extraArgs.put("courseId", courseId);
				extraArgs.put("traineeUserId", userId);
				extraArgs.put("academyId", academyId);
				log.info("Sending attendance notification to user: " + userProfileDto.getDisplayName() + " for course: "
						+ courseId + ", " + courseDto.getTitle());
				pushNotificationService.sendMessageToPushToken(
						userProfileDto.getAndroidFcmPushToken(), NotificationType.LIVE_NOTIFICATION,
						ATTENDANCE_NOTIFICATION_TITLE, String.format(ATTENDANCE_NOTIFICATION_BODY,
								markedAbsent ? "absent" : "present", courseDto.getTitle()),
						NOTIF_CTA_SCREEN, CtaType.SCREEN, extraArgs);
				pushNotificationService.addNotification(
						List.of(userId), String.format(ATTENDANCE_NOTIFICATION_BODY,
								markedAbsent ? "absent" : "present", courseDto.getTitle()),
						CtaType.SCREEN, NOTIF_CTA_SCREEN, extraArgs);
			} catch (ResourceException e) {
				log.error("Error sending notification to user: " + userId + " for course: " + courseId, e);
			}

		});
	}

}