package com.playmotech.api.core.services.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.reflect.TypeToken;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.DayOfWeek;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CourseCoachMapping;
import com.playmotech.api.core.dao_postgres.CoursePaymentOptionsMapping;
import com.playmotech.api.core.dao_postgres.CourseRuleAndRegulationsMapping;
import com.playmotech.api.core.dao_postgres.PaymentLedger;
import com.playmotech.api.core.dao_postgres.Schedule;
import com.playmotech.api.core.dao_postgres.ScheduleFile;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.EnrollTraineeInCourseDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.InstallmentInfo;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.ScheduleFileDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.TraineeDetailsDto;
import com.playmotech.api.core.dto.UpdateCourseDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.PaymentLedgerHelper;
import com.playmotech.api.core.repo.CourseCoachMappingRepo;
import com.playmotech.api.core.repo.CoursePaymentOptionsMappingRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.CourseRuleAndRegulationsMappingRepo;
import com.playmotech.api.core.repo.PaymentLedgerRepository;
import com.playmotech.api.core.repo.ScheduleFileRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.InstallmentUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseService implements ICourseService {

	private final CourseRepo courseRepo;
	private final IAcademyService academyService;
	private final IUserProfileService userProfileService;
	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	private final ModelMapper modelMapper = new ModelMapper();
	private final IPushNotificationService pushNotificationService;
	private final IStorageService storageService;
	private final CourseCoachMappingRepo courseCoachMappingRepo;
	private final CoursePaymentOptionsMappingRepo coursePaymentOptionsMappingRepo;
	private final CourseRuleAndRegulationsMappingRepo courseRuleAndRegulationsMappingRepo;
	private final ITraineeService traineeService;
	private final ScheduleFileRepo scheduleFileRepo;
	private final PaymentLedgerRepository ledgerRepository;

	private final InstallmentUtil installmentUtil;
	private final PaymentLedgerHelper ledgerHelper;

	@Value("${default.icons.course}")
	private String defaultIconUrlCourse;

	@Value("${courses-media-base-url}")
	private String coursesMediaBaseUrl;

	@Value("${storage.courses-media-bucket}")
	private String coursesMediaBucket;

	@Value("${default.icons.course.CRICKET}")
	private String defaultIconUrlCourseCricket;

	@Value("${default.icons.course.FOOTBALL}")
	private String defaultIconUrlCourseFootball;

	@Value("${default.icons.course.TENNIS}")
	private String defaultIconUrlCourseTennis;

	@Value("${default.icons.course.BASKETBALL}")
	private String defaultIconUrlCourseBasketball;

	@Value("${default.icons.course.VOLLEYBALL}")
	private String defaultIconUrlCourseVolleyball;

	@Value("${default.icons.course.YOGA}")
	private String defaultIconUrlCourseYoga;

	@Value("${default.icons.course.BADMINTON}")
	private String defaultIconUrlCourseBadminton;

	@Value("${default.icons.course.SKATING}")
	private String defaultIconUrlCourseSkating;

	@Value("${default.icons.course.SWIMMING}")
	private String defaultIconUrlCourseSwimming;

	@Value("${default.icons.course.TABLE_TENNIS}")
	private String defaultIconUrlCourseTableTennis;

	@Value("${default.icons.course.KARATE}")
	private String defaultIconUrlCourseKarate;

	@Value("${default.icons.course.CALISTHENICS}")
	private String defaultIconUrlCourseCalisthenics;

	@Value("${default.icons.course.PICKLEBALL}")
	private String defaultIconUrlCoursePickleball;

	@Autowired
	public CourseService(final CourseRepo courseRepo, final IAcademyService academyService,
			final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo, final IUserProfileService userProfileService,
			final IPushNotificationService pushNotificationService, final IStorageService storageService,
			final CourseCoachMappingRepo courseCoachMappingRepo,
			final CoursePaymentOptionsMappingRepo coursePaymentOptionsMappingRepo,
			final CourseRuleAndRegulationsMappingRepo courseRuleAndRegulationsMappingRepo,
			@Lazy final ITraineeService traineeService, final ScheduleFileRepo scheduleFileRepo,
			InstallmentUtil installmentUtil, PaymentLedgerHelper ledgerHelper,
			PaymentLedgerRepository ledgerRepository) {
		this.courseRepo = courseRepo;
		this.academyService = academyService;
		this.courseCoachMappingRepo = courseCoachMappingRepo;
		this.traineeCourseEnrollmentRepo = traineeCourseEnrollmentRepo;
		this.coursePaymentOptionsMappingRepo = coursePaymentOptionsMappingRepo;
		this.courseRuleAndRegulationsMappingRepo = courseRuleAndRegulationsMappingRepo;
		this.userProfileService = userProfileService;
		this.pushNotificationService = pushNotificationService;
		this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		this.storageService = storageService;
		this.traineeService = traineeService;
		this.scheduleFileRepo = scheduleFileRepo;
		this.ledgerRepository = ledgerRepository;
		this.installmentUtil = installmentUtil;
		this.ledgerHelper = ledgerHelper;
	}

	@Override
	public CourseDto createCourse(String academyId, String userId, CreateCourseDto createCourseDto, boolean isBulk)
			throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		List<Course> courses = courseRepo.findByAcademy_Id(academyId);
		if (!CollectionUtils.isEmpty(courses) && courses.stream().anyMatch(
				course -> course.getTitle().equalsIgnoreCase(createCourseDto.getTitle()) && !course.getInactive())) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Course with title already exists");
		}

		if (createCourseDto.getSchedule().getAmount() == null && (createCourseDto.getPaymentOptions() == null
				|| createCourseDto.getPaymentOptions().get(PaymentSchedule.FULL) == null)) {
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Full program fee amount is required.");
		}

		// Enhanced payment options filtering
		Map<PaymentSchedule, Long> paymentOptions = filterValidPaymentOptions(createCourseDto.getPaymentOptions(),
				createCourseDto.getSchedule().getAmount());
		createCourseDto.setPaymentOptions(paymentOptions);

		if (StringUtils.isEmpty(createCourseDto.getSchedule().getTimezone())) {
			createCourseDto.getSchedule().setTimezone(AppConstants.DEFAULT_TIMEZONE);
		}

		Course course = modelMapper.map(createCourseDto, Course.class);
		course.setAcademy(Academy.builder().id(academyId).build());
		course.setId(UUID.randomUUID().toString());
		course.setCreatedOn(Timestamp.from(Instant.now()));
		// Set sport-specific icon URL based on the selected sport
		// Initialize a map to hold sport-icon URL associations
		Map<Sports, String> sportIconMap = new HashMap<>();
		sportIconMap.put(Sports.CRICKET, defaultIconUrlCourseCricket);
		sportIconMap.put(Sports.FOOTBALL, defaultIconUrlCourseFootball);
		sportIconMap.put(Sports.TENNIS, defaultIconUrlCourseTennis);
		sportIconMap.put(Sports.BASKETBALL, defaultIconUrlCourseBasketball);
		sportIconMap.put(Sports.VOLLEYBALL, defaultIconUrlCourseVolleyball);
		sportIconMap.put(Sports.YOGA, defaultIconUrlCourseYoga);
		sportIconMap.put(Sports.BADMINTON, defaultIconUrlCourseBadminton);
		sportIconMap.put(Sports.SKATING, defaultIconUrlCourseSkating);
		sportIconMap.put(Sports.SWIMMING, defaultIconUrlCourseSwimming);
		sportIconMap.put(Sports.TABLE_TENNIS, defaultIconUrlCourseTableTennis);
		sportIconMap.put(Sports.KARATE, defaultIconUrlCourseKarate);
		sportIconMap.put(Sports.CALISTHENICS, defaultIconUrlCourseCalisthenics);
		sportIconMap.put(Sports.PICKLEBALL, defaultIconUrlCoursePickleball);

		// Set the icon URL based on the selected sport
		String iconUrl = sportIconMap.getOrDefault(course.getSport(), defaultIconUrlCourse);
		course.setIconUrl(iconUrl);

		course.setInactive(false);

		List<String> coaches = createCourseDto.getCoachUserIds();

		if (CollectionUtils.isEmpty(coaches)) {
			coaches = new ArrayList<>();
		}

		if (!coaches.contains(userId)) {
			coaches.add(userId);
		}

		if (StringUtils.isNotEmpty(createCourseDto.getCoachUserId())
				&& !coaches.contains(createCourseDto.getCoachUserId())) {
			coaches.add(createCourseDto.getCoachUserId());
		}

		course.setCourseCoachMappings(getCourseCoachMappings(course.getId(), coaches));

		if (course.getVisibility() == null) {
			course.setVisibility(Visibility.PRIVATE);
		}

		course.setSchedule(buildSchedule(course.getId(), createCourseDto));
		course.setPaymentOptions(buildCoursePaymentOptionsMapping(course.getId(), createCourseDto.getPaymentOptions(),
				createCourseDto.getSchedule().getCurrency()));
		course.setRuleAndRegulations(
				buildCourseRuleAndRegulationsMapping(course.getId(), createCourseDto.getSchedule()));

		if (createCourseDto.getScheduleFiles() != null && !ObjectUtils.isEmpty(createCourseDto.getScheduleFiles())) {
			ScheduleFileDto scheduleFileDto = createCourseDto.getScheduleFiles().stream().findFirst().get();
			// Fetch the complete ScheduleFile entity instead of creating a partial one
			Optional<ScheduleFile> scheduleFile = scheduleFileRepo.findById(scheduleFileDto.getId());
			if (scheduleFile.isPresent()) {
				course.setScheduleFile(scheduleFile.get());
			} else {
				log.error("Unable to find schedule based on the provided id: {}", scheduleFileDto.getId());
				log.error("Setting the schedule object in course entity to null");
				course.setSchedule(null);
			}

		}

		Course updatedCourse = courseRepo.save(course);

		if (!isBulk) {
			// Push notification logic (unchanged)
			Map<String, String> extraArgs = new HashMap<>();
			extraArgs.put("academyId", academyId);
			extraArgs.put("courseId", updatedCourse.getId());
			log.info("Sending push notification for new course added. Name: {}, Id: {}", updatedCourse.getTitle(),
					updatedCourse.getId());
			try {
				List<TraineeDetailsDto> traineeDetailsDtos = traineeService
						.getTraineesByAcademyIdAndNameAndPhoneNumber(academyId, null, null, null);
				for (TraineeDetailsDto traineeDetailsDto : traineeDetailsDtos) {
					if (StringUtils.isEmpty(traineeDetailsDto.getUserProfile().getAndroidFcmPushToken())) {
						continue;
					}
					pushNotificationService.sendMessageToPushToken(
							traineeDetailsDto.getUserProfile().getAndroidFcmPushToken(),
							NotificationType.LIVE_NOTIFICATION, "New program Added",
							String.format("New program %s added in %s", course.getTitle(), academyDto.getName()),
							"PROGRAM_DETAILS", CtaType.SCREEN, extraArgs);
				}
				pushNotificationService.addNotification(
						traineeDetailsDtos.stream().map(TraineeDetailsDto::getTraineeUserId).toList(),
						String.format("New program %s added in %s", course.getTitle(), academyDto.getName()),
						CtaType.SCREEN, "PROGRAM_DETAILS", extraArgs);
			} catch (ResourceException e) {
				throw new RuntimeException(e);
			}
		}
		//
		// pushNotificationService.sendMessageToTopic(String.format(PushNotifConstants.ACADEMY_SUBSCRIPTION_NAME,
		// academyId), NotificationType.LIVE_NOTIFICATION,
		// "New Course Added", String.format("New course %s added in %s",
		// course.getTitle(), academyDto.getName()), "PROGRAM_DETAILS", CtaType.SCREEN,
		// extraArgs);
		// });

		return adaptCourseDto(updatedCourse);
	}

	@Transactional
	@Override
	public CourseDto createCourseWithFile(String academyId, String userId, CreateCourseDto createCourseDto,
			boolean isBulk, MultipartFile file) throws ResourceException {
		try {
			if (file != null) {
				ScheduleFileDto scheduleFileDto = uploadScheduleFileToS3(userId, file);

				createCourseDto.setScheduleFiles(Set.of(scheduleFileDto));

				// scheduleFiles will not be null
				return this.createCourse(academyId, userId, createCourseDto, isBulk);
			} else {
				// should return scheduleFiles in dto as null
				return this.createCourse(academyId, userId, createCourseDto, isBulk);
			}
		} catch (IOException ioe) {
			log.error("Failed to upload the file to S3: {}", ioe);
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to upload the provided schedule file");
		}
	}

	@Transactional
	@Override
	public CourseDto updateCourse(String academyId, String courseId, UpdateCourseDto updateCourseDto)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Course> course = courseRepo.findByAcademy_IdAndId(academyId, courseId);
		if (course.isEmpty() || course.get().getInactive()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Course not found");
		}

		// Check for title conflict excluding current course
		List<Course> courses = courseRepo.findByAcademy_Id(academyId);
		if (!CollectionUtils.isEmpty(courses) && courses.stream().anyMatch(c -> !c.getId().equals(courseId)
				&& !c.getInactive() && c.getTitle().equalsIgnoreCase(updateCourseDto.getTitle()))) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Course with title already exists");
		}

		if (StringUtils.isNotEmpty(updateCourseDto.getDescription())) {
			course.get().setDescription(updateCourseDto.getDescription());
		}

		if (StringUtils.isNotEmpty(updateCourseDto.getTitle())) {
			course.get().setTitle(updateCourseDto.getTitle());
		}

		if (updateCourseDto.getVisibility() != null) {
			course.get().setVisibility(updateCourseDto.getVisibility());
		}

		if (updateCourseDto.getLevel() != null) {
			course.get().setLevel(updateCourseDto.getLevel());
		}

		if (updateCourseDto.getTotalMaxTrainees() != null && updateCourseDto.getTotalMaxTrainees() > 0) {
			course.get().setTotalMaxTrainees(updateCourseDto.getTotalMaxTrainees());
		}

		if (updateCourseDto.getMaxAge() != null) {
			course.get().setMaxAge(updateCourseDto.getMaxAge());
		}

		if (updateCourseDto.getMinAge() != null) {
			course.get().setMinAge(updateCourseDto.getMinAge());
		}

		if (updateCourseDto.getSport() != null) {
			course.get().setSport(updateCourseDto.getSport());
		}

		if (updateCourseDto.getRegistrationFee() != null) {
			course.get().setRegistrationFee(updateCourseDto.getRegistrationFee());
		}

		if (StringUtils.isNotEmpty(updateCourseDto.getAgeGroup())) {
			course.get().setAgeGroup(updateCourseDto.getAgeGroup());
		}

		if (updateCourseDto.getScheduleFiles() != null
				&& !CollectionUtils.isEmpty(updateCourseDto.getScheduleFiles())) {
			ScheduleFileDto scheduleFileDto = updateCourseDto.getScheduleFiles().stream().findFirst().get();

			// Fetch the complete ScheduleFile entity instead of creating a partial one
			Optional<ScheduleFile> scheduleFile = scheduleFileRepo.findById(scheduleFileDto.getId());
			if (scheduleFile.isPresent()) {
				course.get().setScheduleFile(scheduleFile.get());
			} else {
				log.error("Unable to find schedule file based on the provided id: {}", scheduleFileDto.getId());
				log.error("Setting the schedule file object in course entity to null");
				course.get().setScheduleFile(null);
			}
		} else {
			// This block should execute when and only when both multipart file and
			// scheduleFile in the dto are null
			course.get().setScheduleFile(null);
			log.info("This block executed because multipart file and scheduleFile are null");
		}

		if (!CollectionUtils.isEmpty(updateCourseDto.getCoachUserIds())) {
			if (!CollectionUtils.isEmpty(course.get().getCourseCoachMappings())) {
				courseCoachMappingRepo.deleteAll(course.get().getCourseCoachMappings());
			}
			List<String> coaches = updateCourseDto.getCoachUserIds();

			if (course.get().getCoachUserProfile() != null
					&& !coaches.contains(course.get().getCoachUserProfile().getId())) {
				coaches.add(course.get().getCoachUserProfile().getId());
			}
			course.get().setCourseCoachMappings(getCourseCoachMappings(course.get().getId(), coaches));
		}

		if (course.get().getVisibility() == null) {
			course.get().setVisibility(Visibility.PRIVATE);
		}

		if (updateCourseDto.getSchedule() != null) {
			if (StringUtils.isNotEmpty(updateCourseDto.getSchedule().getTimezone())) {
				course.get().getSchedule().setTimezone(updateCourseDto.getSchedule().getTimezone());
			}

			if (StringUtils.isNotEmpty(updateCourseDto.getSchedule().getStartDate())) {
				course.get().getSchedule().setStartDate(updateCourseDto.getSchedule().getStartDate());
			}

			if (StringUtils.isNotEmpty(updateCourseDto.getSchedule().getEndDate())) {
				course.get().getSchedule().setEndDate(updateCourseDto.getSchedule().getEndDate());
			}

			if (StringUtils.isNotEmpty(updateCourseDto.getSchedule().getStartTime())) {
				course.get().getSchedule().setStartTime(updateCourseDto.getSchedule().getStartTime());
			}

			if (StringUtils.isNotEmpty(updateCourseDto.getSchedule().getEndTime())) {
				course.get().getSchedule().setEndTime(updateCourseDto.getSchedule().getEndTime());
			}

			if (updateCourseDto.getSchedule().getAmount() != null) {
				course.get().getPaymentOptions().forEach(coursePaymentOptionsMapping -> {
					if (coursePaymentOptionsMapping.getPaymentSchedule() == PaymentSchedule.FULL) {
						coursePaymentOptionsMapping.setPaymentAmount(updateCourseDto.getSchedule().getAmount());
					}
				});
			}

			if (updateCourseDto.getSchedule().getCurrency() != null) {
				course.get().getPaymentOptions().forEach(coursePaymentOptionsMapping -> {
					if (coursePaymentOptionsMapping.getPaymentSchedule() == PaymentSchedule.FULL) {
						coursePaymentOptionsMapping.setCurrency(updateCourseDto.getSchedule().getCurrency());
					}
				});
			}

			if (!CollectionUtils.isEmpty(updateCourseDto.getPaymentOptions())) {
				if (!CollectionUtils.isEmpty(course.get().getPaymentOptions())) {
					coursePaymentOptionsMappingRepo.deleteAll(course.get().getPaymentOptions());
				}

				// Enhanced payment options filtering for update
				Long fullAmount = updateCourseDto.getSchedule().getAmount();
				Map<PaymentSchedule, Long> filteredPaymentOptions = filterValidPaymentOptions(
						updateCourseDto.getPaymentOptions(), fullAmount);
				updateCourseDto.setPaymentOptions(filteredPaymentOptions);

				course.get().setPaymentOptions(buildCoursePaymentOptionsMapping(course.get().getId(),
						updateCourseDto.getPaymentOptions(), updateCourseDto.getSchedule().getCurrency()));
			}

			if (!CollectionUtils.isEmpty(updateCourseDto.getSchedule().getRulesAndRegulations())) {
				if (!CollectionUtils.isEmpty(course.get().getRuleAndRegulations())) {
					courseRuleAndRegulationsMappingRepo.deleteAll(course.get().getRuleAndRegulations());
				}
				course.get().setRuleAndRegulations(
						buildCourseRuleAndRegulationsMapping(courseId, updateCourseDto.getSchedule()));
			}

			if (!CollectionUtils.isEmpty(updateCourseDto.getSchedule().getWeekdays())) {
				course.get().getSchedule()
						.setWeekdaysJson(AppConstants.GSON.toJson(updateCourseDto.getSchedule().getWeekdays()));
			}
		}

		try {
			return adaptCourseDto(courseRepo.save(course.get()));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	@Transactional
	@Override
	public CourseDto updateCourseWithFile(String userId, String academyId, String courseId,
			UpdateCourseDto updateCourseDto, MultipartFile file) throws ResourceException {
		try {
			if (file != null) {
				ScheduleFileDto scheduleFileDto = uploadScheduleFileToS3(userId, file);

				updateCourseDto.setScheduleFiles(new HashSet<>(Arrays.asList(scheduleFileDto)));

				// scheduleFiles will not be null
				return this.updateCourse(academyId, courseId, updateCourseDto);
			} else {
				// scheduleFiles should be either an object or null
				return this.updateCourse(academyId, courseId, updateCourseDto);
			}
		} catch (IOException ioe) {
			log.error("Failed to upload the file to S3: {}", ioe);
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to upload the provided schedule file");
		}
	}

	/**
	 * Uploads a schedule file to S3, saves in database and returns the
	 * ScheduleFileDto.
	 * 
	 * @param userId {@link String}
	 * @param file   {@link MultipartFile}
	 * @return {@link ScheduleFileDto}
	 * @throws IOException
	 */
	private ScheduleFileDto uploadScheduleFileToS3(String userId, MultipartFile file) throws IOException {
		FileObjectDto fileObjectDto = new FileObjectDto();
		fileObjectDto.setOriginalFilename(file.getOriginalFilename());
		fileObjectDto.setContent(file.getBytes());
		fileObjectDto.setContentType(file.getContentType());

		String originalFilename = fileObjectDto.getOriginalFilename();
		String ext = "";
		String fileNameWithoutExtension = "";

		if (originalFilename != null && originalFilename.contains(".")) {
			int dotIndex = originalFilename.lastIndexOf('.');
			ext = originalFilename.substring(dotIndex); // includes dot
			fileNameWithoutExtension = originalFilename.substring(0, dotIndex);
		} else {
			fileNameWithoutExtension = originalFilename != null ? originalFilename : "Schedule";
		}

		// Create timestamp string, e.g. "20250702T154530Z"
		String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"))
				.format(Instant.now());

		// Build key with filename, timestamp and extension
		String key = "schedules/" + fileNameWithoutExtension + "-" + timestamp + ext;

		log.info("Uploading schedule file to bucket: {}, key: {}", coursesMediaBucket, key);

		storageService.upload(coursesMediaBucket, key, fileObjectDto.getContent(), fileObjectDto.getContentType());

		String fileUrl = coursesMediaBaseUrl + key;
		log.info("File successfully uploaded. Accessible URL: {}", fileUrl);

		// Persist ScheduleFile entity
		ScheduleFile scheduleFile = ScheduleFile.builder().fileName(originalFilename).fileUrl(fileUrl).createdBy(userId)
				.build();

		scheduleFile = scheduleFileRepo.save(scheduleFile);

		ScheduleFileDto scheduleFileDto = toScheduleFileDto(scheduleFile);
		return scheduleFileDto;
	}

	/**
	 * Filters payment options to exclude values that are null, zero, or negative.
	 * Ensures FULL payment schedule is always present with the provided full
	 * amount.
	 * 
	 * @param paymentOptions The payment options map to filter
	 * @param fullAmount     The full amount to be used for FULL payment schedule
	 * @return Filtered map containing only valid payment options
	 */
	private Map<PaymentSchedule, Long> filterValidPaymentOptions(Map<PaymentSchedule, Long> paymentOptions,
			Long fullAmount) {

		Map<PaymentSchedule, Long> paymentScheduleMap = CollectionUtils.isEmpty(paymentOptions) ? new HashMap<>()
				: new HashMap<>(paymentOptions);

		// Ensure FULL payment schedule is present with the correct amount
		if (fullAmount != null && fullAmount > 0) {
			paymentScheduleMap.put(PaymentSchedule.FULL, fullAmount);
		}

		// Filter out entries with null, zero, or negative values
		return paymentScheduleMap.entrySet().stream().filter(entry -> entry.getValue() != null && entry.getValue() > 0)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public String updateCoursePicture(String academyId, String courseId, String userId, FileObjectDto fileObjectDto)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Course> course = courseRepo.findByAcademy_IdAndId(academyId, courseId);
		if (course.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Course not found");
		}

		if (course.get().getCourseCoachMappings().stream().noneMatch(
				courseCoachMapping -> courseCoachMapping.getCoachUserProfile().getId().equalsIgnoreCase(userId))) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User not authorized to update course picture");
		}

		if (fileObjectDto != null) {
			String prefix = "courses-media/" + academyId + "/" + UUID.randomUUID() + "_"
					+ fileObjectDto.getOriginalFilename();
			storageService.upload(coursesMediaBucket, prefix, fileObjectDto.getContent(),
					fileObjectDto.getContentType());
			course.get().setIconUrl(coursesMediaBaseUrl + prefix);
			courseRepo.save(course.get());
			return coursesMediaBaseUrl + prefix;
		}
		throw new ResourceException(ErrorCodes.INVALID_REQUEST, "FileObjectDto is null");
	}

	@Override
	public CourseDto getCourse(String academyId, String courseId) throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		Optional<Course> course = courseRepo.findByAcademy_IdAndId(academyId, courseId);

		// TODO ADD BACK AGAIN IN FUTURE ASAP
		if (course.isEmpty() || course.get().getInactive()) {
			// if (course.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Course not found");
		}

		// CourseDto courseDto = modelMapper.map(course.get(), CourseDto.class);
		// courseDto.setAcademy(modelMapper.map(academyDto, AcademyMinDto.class));
		return adaptCourseDto(course.get());
	}

	@Override
	public List<CourseDto> getCourses(String academyId, List<String> courseId, Boolean isActive, Visibility visibility)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		List<Course> courses = courseRepo.findByAcademy_IdAndIdIn(academyId, courseId).stream()
				.filter(course -> !course.getInactive()).toList();
		if (visibility != null) {
			courses = courses.stream().filter(course -> {
				if (visibility == Visibility.PUBLIC && course.getVisibility() == Visibility.PUBLIC) {
					return true;
				}
				return visibility == Visibility.PRIVATE
						&& (course.getVisibility() == null || course.getVisibility() == Visibility.PRIVATE);
			}).toList();
		}
		if (!CollectionUtils.isEmpty(courses)) {
			List<Course> courses1 = courses.stream().filter(course -> isActive == null || (course.getSchedule()
					.getType() != ScheduleType.CUSTOM
					&& ((isActive && !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
							|| (!isActive
									&& DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata")))))
					.collect(Collectors.toList());
			return sort(adaptCourseDtos(courses1));
		}
		return List.of();
	}

	@Override
	public List<CourseDto> getCoursesByAcademyId(String userId, String academyId, String traineeId, Sports sport,
			Boolean isActive, Visibility visibility, Boolean filterTournaments, Boolean excludeEnrolledCourses,
			String searchTxt, String orgId) throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		List<Course> courses;

		UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);
		if (!StringUtils.isEmpty(traineeId)) {
			List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo
					.findByTraineeUserProfile_IdAndAcademy_Id(traineeId, academyId).stream()
					.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus().equals(Status.ACTIVE))
					.toList();
			courses = traineeCourseEnrollments.stream().map(TraineeCourseEnrollment::getCourse).toList();
			// courses = courseRepo.findByIdIn(courseIds).stream().filter(course ->
			// !course.getInactive())
			// .filter(course -> isActive==null || (course.getSchedule().getType() !=
			// ScheduleType.CUSTOM
			// && ((isActive && !DateTimeUtils.isExpired(course.getSchedule().getEndDate(),
			// "Asia/Kolkata"))
			// || (!isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(),
			// "Asia/Kolkata"))))).toList();
			//
			if (isActive != null) {
				courses = courses.stream().filter(course -> !course.getInactive())
						.filter(course -> course.getSchedule().getType() != ScheduleType.CUSTOM && ((isActive
								&& !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
								|| (!isActive
										&& DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))))
						.toList();
			}

		} else {
			courses = courseRepo.findByAcademy_Id(academyId).stream().filter(course -> !course.getInactive()).filter(
					course -> (isActive == null || (course.getSchedule().getType() != ScheduleType.CUSTOM && ((isActive
							&& !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
							|| (!isActive
									&& DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata")))))
							&& (course.getVisibility() == Visibility.PRIVATE || course.getVisibility() == null))
					.toList();

			if (userProfileDto.getUserType() == UserType.PLAYER) {
				List<Course> publicCourses = courseRepo.findByVisibility(Visibility.PUBLIC).stream()
						.filter(course -> !course.getInactive())
						.filter(course -> isActive == null
								|| (course.getSchedule().getType() != ScheduleType.CUSTOM && ((isActive
										&& !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
										|| (!isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(),
												"Asia/Kolkata")))))
						.toList();

				if (!CollectionUtils.isEmpty(publicCourses)) {
					courses = new ArrayList<>(courses);
					courses.addAll(publicCourses);
					courses = courses.stream().collect(
							Collectors.toMap(Course::getId, item -> item, (existing, replacement) -> replacement))
							.values().stream().toList();
				}

				if (excludeEnrolledCourses != null && excludeEnrolledCourses) {
					List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo
							.findByTraineeUserProfile_IdAndAcademy_Id(userId, academyId).stream()
							.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus()
									.equals(Status.ACTIVE))
							.toList();
					List<Course> enrolledCourses = traineeCourseEnrollments.stream()
							.map(TraineeCourseEnrollment::getCourse).toList();
					courses = courses.stream().filter(course -> !enrolledCourses.contains(course)).toList();
				}

			} else if (userProfileDto.getUserType() == UserType.COACH) {
				List<Course> courseByCoachAndAcademy = courseRepo.findByAcademy_Id(academyId).stream()
						.filter(course -> !course.getInactive())
						.filter(course -> isActive == null
								|| (course.getSchedule().getType() != ScheduleType.CUSTOM && ((isActive
										&& !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
										|| (!isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(),
												"Asia/Kolkata")))))
						.toList();
				courseByCoachAndAcademy = courseByCoachAndAcademy.stream()
						.filter(course -> (!CollectionUtils.isEmpty(course.getCourseCoachMappings())
								&& course.getCourseCoachMappings().stream()
										// TODO: This failed on a scenario when Coach creates a program/course under an
										// academy.
										// When Academy Owner logs in, he won't be able to see the program created by
										// the coach
										.anyMatch(courseCoachMapping -> courseCoachMapping.getCoachUserProfile().getId()
												.equalsIgnoreCase(userId)))
								|| (course.getCoachUserProfile() != null
										&& course.getCoachUserProfile().getId().equalsIgnoreCase(userId)))
						.toList();
				if (!CollectionUtils.isEmpty(courseByCoachAndAcademy)) {
					courses = new ArrayList<>(courses);
					courses.addAll(courseByCoachAndAcademy);
					courses = courses.stream().collect(
							Collectors.toMap(Course::getId, item -> item, (existing, replacement) -> replacement))
							.values().stream().toList();
				}
			}
		}

		// As discussed on 30-06-2025, uncommeting this check, verified with Deep
		if (visibility != null) {
			courses = courses.stream().filter(course -> {
				if (visibility == Visibility.PUBLIC && course.getVisibility() == Visibility.PUBLIC)
					return true;
				return visibility == Visibility.PRIVATE
						&& (course.getVisibility() == null || course.getVisibility() == Visibility.PRIVATE);
			}).toList();
		}

		if (filterTournaments != null) {
			courses = courses.stream()
					.filter(course -> (filterTournaments && course.getSchedule().getType() == ScheduleType.TOURNAMENT)
							|| (!filterTournaments && course.getSchedule().getType() != ScheduleType.TOURNAMENT))
					.toList();
		}

		if (sport != null) {
			courses = courses.stream().filter(course -> course.getSport().equals(sport)).toList();
		}

		if (StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2) {
			courses = courses.stream()
					.filter(course -> course.getTitle().toLowerCase().contains(searchTxt.toLowerCase())).toList();
		}

		if (org.springframework.util.StringUtils.hasText(orgId)) {
			List<AcademyDto> academiesByOrg = academyService.getAcademiesByOrgId(orgId);
			Set<String> validAcademyIds = academiesByOrg.stream().map(AcademyDto::getId).collect(Collectors.toSet());

			if (!CollectionUtils.isEmpty(courses)) {
				courses = courses.stream().filter(course -> {
					String courseAcademyId = course.getAcademy().getId();
					return org.springframework.util.StringUtils.hasText(courseAcademyId)
							? validAcademyIds.contains(courseAcademyId)
							: false;
				}).collect(Collectors.toList());
			}
		}

		if (!CollectionUtils.isEmpty(courses)) {
			return sort(adaptCourseDtos(courses));
		}
		return List.of();
	}

	@Override
	public List<CourseDto> getCoursesByCoachUserId(String coachUserId, String academyId, Boolean isActive,
			Visibility visibility, Boolean filterTournaments, String searchTxt) throws ResourceException {
		List<Course> courses = StringUtils.isEmpty(academyId)
				? courseRepo.findCoursesByCoachId(coachUserId).stream().filter(course -> !course.getInactive()).toList()
				: courseRepo.findCoursesByCoachIdAndAcademyId(coachUserId, academyId).stream()
						.filter(course -> !course.getInactive()).toList();
		if (isActive != null) {
			courses = courses.stream().filter(course -> course.getSchedule().getType() != ScheduleType.CUSTOM
					&& (isActive && !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata")
							|| !isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata")))
					.toList();
		}
		if (visibility != null) {
			courses = courses.stream().filter(course -> {
				if (visibility == Visibility.PUBLIC && course.getVisibility() == Visibility.PUBLIC) {
					return true;
				}
				return visibility == Visibility.PRIVATE
						&& (course.getVisibility() == null || course.getVisibility() == Visibility.PRIVATE);
			}).toList();
		}

		if (filterTournaments != null) {
			courses = courses.stream()
					.filter(course -> (filterTournaments && course.getSchedule().getType() == ScheduleType.TOURNAMENT)
							|| (!filterTournaments && course.getSchedule().getType() != ScheduleType.TOURNAMENT))
					.toList();
		}

		if (StringUtils.isNotEmpty(searchTxt)) {
			courses = courses.stream()
					.filter(course -> course.getTitle().toLowerCase().contains(searchTxt.toLowerCase())).toList();
		}

		if (!CollectionUtils.isEmpty(courses)) {
			return sort(adaptCourseDtos(courses));
			// return courseDtos;
		}
		return List.of();
	}

	@Override
	public List<TraineeCourseEnrollmentDto> getEnrolledTrainees(String academyId, String courseId)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		getCourse(academyId, courseId);

		List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo.findByCourse_Id(courseId)
				.stream().filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus().equals(Status.ACTIVE))
				.toList();
		if (traineeCourseEnrollments.isEmpty()) {
			return List.of();
		}

		List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos = traineeCourseEnrollments.stream()
				.map(this::toTraineeCourseEnrollmentDto).toList();

		return sortTrainees(traineeCourseEnrollmentDtos);
	}

	@Override
	public List<CourseDto> getEnrolledCourses(String academyId, String traineeUserId, Boolean isActive,
			Visibility visibility) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo
				.findByTraineeUserProfile_IdAndAcademy_Id(traineeUserId, academyId).stream()
				.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus().equals(Status.ACTIVE)).toList();
		if (CollectionUtils.isEmpty(traineeCourseEnrollments)) {
			return List.of();
		}
		List<Course> courses = courseRepo
				.findByIdIn(traineeCourseEnrollments.stream()
						.map(traineeCourseEnrollment -> traineeCourseEnrollment.getCourse().getId()).toList())
				.stream().filter(course -> !course.getInactive())
				.filter(course -> isActive == null || (course.getSchedule().getType() != ScheduleType.CUSTOM
						&& ((isActive && !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
								|| (!isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(),
										"Asia/Kolkata")))))
				.toList();
		if (visibility != null) {
			courses = courses.stream().filter(course -> {
				if (visibility == Visibility.PUBLIC && course.getVisibility() == Visibility.PUBLIC) {
					return true;
				}
				return visibility == Visibility.PRIVATE
						&& (course.getVisibility() == null || course.getVisibility() == Visibility.PRIVATE);
			}).toList();
		}
		if (CollectionUtils.isEmpty(courses)) {
			return List.of();
		}

		return sort(adaptCourseDtos(courses));
		// return courseDtos;
	}

	@Override
	public List<TraineeCourseEnrollment> getEnrollmentsByTraineeUserId(String academyId, String traineeUserId)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		return traineeCourseEnrollmentRepo.findByTraineeUserProfile_IdAndAcademy_Id(traineeUserId, academyId).stream()
				.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus().equals(Status.ACTIVE)).toList();
	}

	@Override
	public List<TraineeCourseEnrollment> getEnrollmentsByCourseId(String academyId, String courseId)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		return traineeCourseEnrollmentRepo.findByCourse_IdAndAcademy_Id(courseId, academyId).stream()
				.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getStatus().equals(Status.ACTIVE)).toList();
	}

	@Override
	public List<TraineeCourseEnrollment> getEnrollmentsByAcademyId(String academyId) throws ResourceException {
		academyService.getAcademyById(academyId);
		return traineeCourseEnrollmentRepo.findByAcademy_Id(academyId);
	}

	@Override
	public List<TraineeCourseEnrollment> getEnrollmentsByAcademyId(String academyId, List<String> ids)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		return traineeCourseEnrollmentRepo.findByAcademy_IdAndIdIn(academyId, ids);
	}

	// @Override
	// public void enrollTraineesInCourse(String academyId, String courseId,
	// List<String> traineeUserIds) throws ResourceException {
	// academyService.getAcademyById(academyId);
	// CourseDto courseDto = getCourse(academyId, courseId);
	// if (CollectionUtils.isEmpty(traineeUserIds)) {
	// throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Trainee
	// user ids cannot be empty");
	// }
	//
	// PaymentSchedule paymentSchedule =
	// CollectionUtils.isEmpty(courseDto.getPaymentOptions())
	// || courseDto.getPaymentOptions().size() > 1? PaymentSchedule.FULL:
	// courseDto.getPaymentOptions().keySet().iterator().next();
	//
	// List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos =
	// getEnrolledTrainees(academyId, courseId);
	// List<String> enrolledTraineeUserIds =
	// traineeCourseEnrollmentDtos.stream().map(traineeCourseEnrollmentDto
	// -> traineeCourseEnrollmentDto.getUserProfile().getId()).toList();
	// List<TraineeCourseEnrollment> traineeCourseEnrollments = new ArrayList<>();
	// traineeUserIds.forEach(traineeUserId -> {
	// if (enrolledTraineeUserIds.contains(traineeUserId)) {
	// log.warn("Trainee with id {} already enrolled in course with id {}",
	// traineeUserId, courseId);
	// } else {
	// TraineeCourseEnrollment traineeCourseEnrollment = new
	// TraineeCourseEnrollment();
	// traineeCourseEnrollment.setId(UUID.randomUUID().toString());
	// traineeCourseEnrollment.setTraineeUserProfile(UserProfile.builder().id(traineeUserId).build());
	// traineeCourseEnrollment.setCourse(Course.builder().id(courseId).build());
	// traineeCourseEnrollment.setStatus(Status.ACTIVE);
	// traineeCourseEnrollment.setCreatedOn(Timestamp.from(Instant.now()));
	// traineeCourseEnrollment.setAcademy(Academy.builder().id(academyId).build());
	// traineeCourseEnrollment.setPaymentSchedule(paymentSchedule);
	// traineeCourseEnrollments.add(traineeCourseEnrollment);
	// }
	// });
	//
	// traineeCourseEnrollmentRepo.saveAll(traineeCourseEnrollments);
	// }

	@Override
	@Transactional
	public void enrollTraineesInCourseWithPaymentOptions(String academyId, String courseId,
			EnrollTraineeInCourseDto traineeUser) throws ResourceException {

		// Validate academy and course existence
		academyService.getAcademyById(academyId);
		CourseDto courseDto = getCourse(academyId, courseId);

		// Check if trainee is already enrolled
		List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos = getEnrolledTrainees(academyId, courseId);
		List<String> enrolledTraineeUserIds = traineeCourseEnrollmentDtos.stream()
				.map(traineeCourseEnrollmentDto -> traineeCourseEnrollmentDto.getUserProfile().getId()).toList();

		if (enrolledTraineeUserIds.contains(traineeUser.getTraineeUserId())) {
			log.error("Trainee with id {} already enrolled in course with id {}", traineeUser.getTraineeUserId(),
					courseId);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Trainee already enrolled in course");
		}

		// Validate payment schedule
		if (traineeUser.getPaymentSchedule() == null
				|| !courseDto.getPaymentOptions().containsKey(traineeUser.getPaymentSchedule())) {
			log.error("Invalid payment schedule for trainee with id {}", traineeUser.getTraineeUserId());
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Invalid payment schedule");
		}

		// Validate dates
		if (traineeUser.getJoiningDate().isAfter(traineeUser.getDueDate())) {
			log.error("Invalid trainee due date or joining date. Trainee ID: {}", traineeUser.getTraineeUserId());
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED,
					"Joining date cannot be after the due date");
		}

		try {
			// Calculate installments and due dates first
			TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();
			enrollmentDto.setAmount(traineeUser.getAmount());
			enrollmentDto.setJoiningDate(traineeUser.getJoiningDate());
			enrollmentDto.setDueDate(traineeUser.getDueDate());
			enrollmentDto.setPaymentSchedule(traineeUser.getPaymentSchedule());

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

			// Create and configure enrollment entity with all calculated values
			TraineeCourseEnrollment traineeCourseEnrollment = new TraineeCourseEnrollment();
			traineeCourseEnrollment.setId(UUID.randomUUID().toString());
			traineeCourseEnrollment
					.setTraineeUserProfile(UserProfile.builder().id(traineeUser.getTraineeUserId()).build());
			traineeCourseEnrollment.setCourse(Course.builder().id(courseId).build());
			traineeCourseEnrollment.setStatus(Status.ACTIVE);
			traineeCourseEnrollment.setCreatedOn(Timestamp.from(Instant.now()));
			traineeCourseEnrollment.setAcademy(Academy.builder().id(academyId).build());
			traineeCourseEnrollment.setPaymentSchedule(traineeUser.getPaymentSchedule());
			traineeCourseEnrollment.setAmount(traineeUser.getAmount());
			traineeCourseEnrollment.setJoiningDate(traineeUser.getJoiningDate());
			traineeCourseEnrollment.setDueDate(traineeUser.getDueDate());
			traineeCourseEnrollment.setUseForFuture(Boolean.FALSE);
			traineeCourseEnrollment.setDiscountAmount(0L);

			// Set calculated values for dues
			if (lastDueInstallment != null) {
				traineeCourseEnrollment.setDuesOn(lastDueInstallment.getDueDate());
				traineeCourseEnrollment.setFinalDueAmount(lastDueInstallment.getAmount());
			} else {
				traineeCourseEnrollment.setDuesOn(traineeUser.getDueDate());
				traineeCourseEnrollment.setFinalDueAmount(traineeUser.getAmount());
			}

			// Save enrollment entity once with all final values
			TraineeCourseEnrollment savedEnrollment = traineeCourseEnrollmentRepo.save(traineeCourseEnrollment);

			// Collect all ledger entries to save in batch
			List<PaymentLedger> ledgersToSave = new ArrayList<>();

			// Create past due ledger entries for installments before last due date
			for (InstallmentInfo pastInst : installmentsBeforeLastDue) {
				PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(academyId, courseId, savedEnrollment,
						pastInst);
				ledgersToSave.add(pastDueLedger);
			}

			// Create registration fee ledger entry
			PaymentLedger registrationFeeLedger = ledgerHelper.createRegistrationFeeLedger(academyId, courseId,
					savedEnrollment, courseDto);
			ledgersToSave.add(registrationFeeLedger);

			// Save all ledgers at once
			ledgerRepository.saveAll(ledgersToSave);

			log.info("Successfully enrolled trainee {} in course {} with payment schedule {}",
					traineeUser.getTraineeUserId(), courseId, traineeUser.getPaymentSchedule());

		} catch (Exception e) {
			log.error("Error during enrollment process for trainee {}: {}", traineeUser.getTraineeUserId(),
					e.getMessage(), e);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED,
					"Error during enrollment process: " + e.getMessage());
		}
	}

	@Override
	public void updateTraineeEnrollment(String academyId, String courseId, String enrollmentId,
			EnrollTraineeInCourseDto updateTraineeEnrollmentDto) throws ResourceException {

		// Validate academy and course exist
		academyService.getAcademyById(academyId);
		CourseDto courseDto = getCourse(academyId, courseId);

		// Find existing enrollment
		Optional<TraineeCourseEnrollment> existingEnrollmentOpt = traineeCourseEnrollmentRepo.findById(enrollmentId);
		if (existingEnrollmentOpt.isEmpty()) {
			log.error("Enrollment with id {} not found", enrollmentId);
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}

		TraineeCourseEnrollment existingEnrollment = existingEnrollmentOpt.get();

		// Validate enrollment belongs to the specified course and academy
		if (!existingEnrollment.getCourse().getId().equals(courseId)
				|| !existingEnrollment.getAcademy().getId().equals(academyId)) {
			log.error("Enrollment {} does not belong to course {} in academy {}", enrollmentId, courseId, academyId);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED,
					"Enrollment does not belong to specified course and academy");
		}

		// Validate enrollment is active
		if (!existingEnrollment.getStatus().equals(Status.ACTIVE)) {
			log.error("Cannot update inactive enrollment with id {}", enrollmentId);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Cannot update inactive enrollment");
		}

		// Validate payment schedule if provided
		if (updateTraineeEnrollmentDto.getPaymentSchedule() != null
				&& !courseDto.getPaymentOptions().containsKey(updateTraineeEnrollmentDto.getPaymentSchedule())) {
			log.error("Invalid payment schedule for enrollment with id {}", enrollmentId);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "Invalid payment schedule");
		}

		// Validate dates if both are provided
		LocalDate joiningDate = updateTraineeEnrollmentDto.getJoiningDate() != null
				? updateTraineeEnrollmentDto.getJoiningDate()
				: existingEnrollment.getJoiningDate();
		LocalDate dueDate = updateTraineeEnrollmentDto.getDueDate() != null ? updateTraineeEnrollmentDto.getDueDate()
				: existingEnrollment.getDueDate();

		if (joiningDate != null && dueDate != null && joiningDate.isAfter(dueDate)) {
			log.error("Invalid trainee due date or joining date for enrollment with id {}", enrollmentId);
			throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED,
					"Joining date cannot be after the due date");
		}

		// Update enrollment fields
		if (updateTraineeEnrollmentDto.getPaymentSchedule() != null) {
			existingEnrollment.setPaymentSchedule(updateTraineeEnrollmentDto.getPaymentSchedule());
		}
		if (updateTraineeEnrollmentDto.getJoiningDate() != null) {
			existingEnrollment.setJoiningDate(updateTraineeEnrollmentDto.getJoiningDate());
		}
		if (updateTraineeEnrollmentDto.getDueDate() != null) {
			existingEnrollment.setDueDate(updateTraineeEnrollmentDto.getDueDate());
		}

		// ✅ Only update finalDueAmount if new amount provided
		Long updatedAmount = updateTraineeEnrollmentDto.getAmount();
		if (updatedAmount != null) {
			existingEnrollment.setAmount(updatedAmount);
		}

		if (existingEnrollment.getDuesOn() == null) {
			existingEnrollment.setDuesOn(null);
		}

		// ✅ Only reset if blank/null
		Long discountAmount = existingEnrollment.getDiscountAmount();

		if (discountAmount == null || discountAmount == 0L) {
			existingEnrollment.setDiscountAmount(0L);
			existingEnrollment.setUseForFuture(Boolean.FALSE);
		} else {
			if (existingEnrollment.getUseForFuture() == null) {
				existingEnrollment.setUseForFuture(Boolean.FALSE);
			}
		}

		existingEnrollment.setStatus(Status.ACTIVE);

		// Save updated enrollment
		traineeCourseEnrollmentRepo.save(existingEnrollment);

		log.info("Successfully updated enrollment with id {} for course {} in academy {}", enrollmentId, courseId,
				academyId);
	}

	@Override
	public void unEnrollTraineeFromAllCourseInAcademy(String academyId, String traineeUserId) {
		log.info("Soft - Unenrolling trainee with id {} from all courses in academy with id {}", traineeUserId,
				academyId);
		traineeCourseEnrollmentRepo.markTraineeEnrollmentInactiveByTraineeUserIdAndAcademyId(academyId, traineeUserId);
	}

	@Override
	public void enrollmentExists(String enrollmentId) throws ResourceException {
		if (!traineeCourseEnrollmentRepo.existsById(enrollmentId)) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}
	}

	@Override
	public Pair<TraineeCourseEnrollmentDto, CourseDto> getByEnrollmentId(String enrollmentId) throws ResourceException {
		Optional<TraineeCourseEnrollment> traineeCourseEnrollment = traineeCourseEnrollmentRepo.findById(enrollmentId);
		if (traineeCourseEnrollment.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}
		// Required by Notification
		TraineeCourseEnrollmentDto traineeCourseEnrollmentDto = toTraineeCourseEnrollmentDto(
				traineeCourseEnrollment.get());
		return Pair.of(traineeCourseEnrollmentDto, getCourse(traineeCourseEnrollment.get().getAcademy().getId(),
				traineeCourseEnrollment.get().getCourse().getId()));
	}

	@Override
	public List<Pair<TraineeCourseEnrollmentDto, CourseDto>> getByEnrollmentIds(List<String> enrollmentId)
			throws ResourceException {
		List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo.findByIdIn(enrollmentId);
		if (CollectionUtils.isEmpty(traineeCourseEnrollments)) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}
		List<Pair<TraineeCourseEnrollmentDto, CourseDto>> traineeCourseEnrollmentDtos = new ArrayList<>();
		Map<String, Course> courseMap = courseRepo
				.findByAcademy_IdInAndIdIn(
						traineeCourseEnrollments.stream()
								.map(traineeCourseEnrollment -> traineeCourseEnrollment.getAcademy().getId()).toList(),
						traineeCourseEnrollments.stream()
								.map(traineeCourseEnrollment -> traineeCourseEnrollment.getCourse().getId()).toList())
				.stream().map(course -> Pair.of(course.getAcademy().getId() + course.getId(), course))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

		traineeCourseEnrollments.forEach(traineeCourseEnrollment -> {
			Course course = courseMap
					.get(traineeCourseEnrollment.getAcademy().getId() + traineeCourseEnrollment.getCourse().getId());
			CourseDto courseDto = adaptCourseDto(course);
			traineeCourseEnrollmentDtos.add(Pair.of(toTraineeCourseEnrollmentDto(traineeCourseEnrollment), courseDto));
		});
		return traineeCourseEnrollmentDtos;
	}

	@Override
	public List<TraineeCourseEnrollmentDto> getByEnrollmentIdsByEID(List<String> enrollmentId)
			throws ResourceException {
		List<TraineeCourseEnrollment> traineeCourseEnrollments = traineeCourseEnrollmentRepo.findByIdIn(enrollmentId);
		if (CollectionUtils.isEmpty(traineeCourseEnrollments)) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}
		List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos = traineeCourseEnrollments.stream()
				.map(this::toTraineeCourseEnrollmentDto).toList();
		// List<String> userIds =
		// traineeCourseEnrollmentDtos.stream().map(traineeCourseEnrollmentDto ->
		// traineeCourseEnrollmentDto.getTraineeUserProfile().getId()).toList();

		// Map<String, UserProfileDto> userProfileDtoMap =
		// userProfileService.getUserProfileByIds(userIds).stream()
		// .collect(Collectors.toMap(UserProfileDto::getId, userProfileDto ->
		// userProfileDto));

		// traineeCourseEnrollmentDtos.forEach(traineeCourseEnrollmentDto -> {
		// UserProfileDto userProfileDto =
		// userProfileDtoMap.get(traineeCourseEnrollmentDto.getTraineeUserProfile().getId());
		// traineeCourseEnrollmentDto.getTraineeUserProfile(userProfileDto);
		// });

		return traineeCourseEnrollmentDtos;
	}

	private List<CourseDto> sort(List<CourseDto> courseDtos) {
		try {
			return courseDtos.stream().sorted(Comparator.comparing(CourseDto::getTitle)).toList();
		} catch (Throwable t) {
			log.error("Error occurred while sorting courses", t);
		}
		return courseDtos;
	}

	private List<TraineeCourseEnrollmentDto> sortTrainees(
			List<TraineeCourseEnrollmentDto> traineeCourseEnrollmentDtos) {
		return traineeCourseEnrollmentDtos.stream()
				.sorted(Comparator.comparing(
						traineeCourseEnrollmentDto -> traineeCourseEnrollmentDto.getUserProfile().getDisplayName()))
				.toList();
	}

	private List<Course> filterCourseByStatus(List<Course> courses, Boolean isActive) {
		if (isActive == null) {
			return courses;
		}
		return courses.stream().filter(course -> course.getSchedule().getType() != ScheduleType.CUSTOM
				&& ((isActive && !DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))
						|| (!isActive && DateTimeUtils.isExpired(course.getSchedule().getEndDate(), "Asia/Kolkata"))))
				.toList();
	}

	@Override
	public List<CourseDto> adaptCourseDtos(List<Course> courses) {
		return courses.stream().map(this::adaptCourseDto).collect(Collectors.toList());
	}

	@Override
	public List<UserProfileMinDto> getCoachesByCourse(String courseId) {
		Optional<Course> course = courseRepo.findById(courseId);
		if (course.isEmpty() || course.get().getInactive()) {
			return List.of();
		}

		if (!CollectionUtils.isEmpty(course.get().getCourseCoachMappings())) {
			return course.get().getCourseCoachMappings().stream().map(CourseCoachMapping::getCoachUserProfile)
					.map(userProfile -> modelMapper.map(userProfile, UserProfileMinDto.class)).toList();
		}
		return List.of();
	}

	public CourseDto adaptCourseDto(Course course) {
		CourseDto courseDto = modelMapper.map(course, CourseDto.class);

		if (course.getScheduleFile() != null) {
			courseDto.setScheduleFiles(Set.of(toScheduleFileDto(course.getScheduleFile())));
		} else {
			courseDto.setScheduleFiles(null);
		}

		Map<PaymentSchedule, Long> paymentScheduleAmountMap = course.getPaymentOptions().stream().collect(Collectors
				.toMap(CoursePaymentOptionsMapping::getPaymentSchedule, CoursePaymentOptionsMapping::getPaymentAmount));
		courseDto.setPaymentOptions(paymentScheduleAmountMap);
		courseDto.getSchedule().setCurrency(course.getPaymentOptions().get(0).getCurrency());
		courseDto.getSchedule().setAmount(paymentScheduleAmountMap.get(PaymentSchedule.FULL));
		courseDto.setPaymentOptions(paymentScheduleAmountMap);
		courseDto.setAgeGroup(course.getAgeGroup());

		List<String> rulesAndRegulations = course.getRuleAndRegulations().stream()
				.map(CourseRuleAndRegulationsMapping::getRule).toList();
		courseDto.getSchedule().setRulesAndRegulations(rulesAndRegulations);
		courseDto.setRulesAndRegulations(rulesAndRegulations);

		courseDto.setCoaches(course.getCourseCoachMappings().stream().map(CourseCoachMapping::getCoachUserProfile)
				.map(userProfile -> modelMapper.map(userProfile, UserProfileMinDto.class)).toList());
		courseDto.setAcademy(modelMapper.map(course.getAcademy(), AcademyMinDto.class));
		courseDto.setAcademyId(course.getAcademy().getId());
		if (StringUtils.isNotEmpty(course.getSchedule().getCustomDatesJson())) {
			courseDto.getSchedule().setCustomDates(AppConstants.GSON.fromJson(course.getSchedule().getCustomDatesJson(),
					TypeToken.getParameterized(List.class, String.class).getType()));
		}

		if (StringUtils.isNotEmpty(course.getSchedule().getWeekdaysJson())) {
			courseDto.getSchedule().setWeekdays(AppConstants.GSON.fromJson(course.getSchedule().getWeekdaysJson(),
					TypeToken.getParameterized(List.class, DayOfWeek.class).getType()));
		}

		return courseDto;
	}

	private List<CourseCoachMapping> getCourseCoachMappings(String courseId, List<String> coachUserIds) {
		Timestamp now = Timestamp.from(Instant.now());
		return coachUserIds.stream()
				.map(coachUserId -> CourseCoachMapping.builder().createdOn(now)
						.course(Course.builder().id(courseId).build())
						.coachUserProfile(UserProfile.builder().id(coachUserId).build()).build())
				.collect(Collectors.toList());
	}

	private Schedule buildSchedule(String courseId, CreateCourseDto createCourseDto) {
		Schedule schedule = new Schedule();
		schedule.setCourse(Course.builder().id(courseId).build());
		schedule.setType(createCourseDto.getSchedule().getType());
		schedule.setStartDate(createCourseDto.getSchedule().getStartDate());
		schedule.setEndDate(createCourseDto.getSchedule().getEndDate());
		schedule.setStartTime(createCourseDto.getSchedule().getStartTime());
		schedule.setEndTime(createCourseDto.getSchedule().getEndTime());
		schedule.setTimezone(createCourseDto.getSchedule().getTimezone());
		if (!CollectionUtils.isEmpty(createCourseDto.getSchedule().getCustomDates())) {
			schedule.setCustomDatesJson(AppConstants.GSON.toJson(createCourseDto.getSchedule().getCustomDates()));
		}
		if (!CollectionUtils.isEmpty(createCourseDto.getSchedule().getWeekdays())) {
			schedule.setWeekdaysJson(AppConstants.GSON.toJson(createCourseDto.getSchedule().getWeekdays()));
		}
		return schedule;
	}

	private List<CourseRuleAndRegulationsMapping> buildCourseRuleAndRegulationsMapping(String courseId,
			ScheduleDto scheduleDto) {
		return scheduleDto.getRulesAndRegulations().stream().map(rule -> CourseRuleAndRegulationsMapping.builder()
				.rule(rule).course(Course.builder().id(courseId).build()).build()).collect(Collectors.toList());
	}

	private List<CoursePaymentOptionsMapping> buildCoursePaymentOptionsMapping(String courseId,
			Map<PaymentSchedule, Long> paymentOptions, Currency currency) {
		return paymentOptions.entrySet().stream()
				.map(entry -> CoursePaymentOptionsMapping.builder().paymentSchedule(entry.getKey())
						.paymentAmount(entry.getValue())
						.currency(currency == null ? AppConstants.DEFAULT_CURRENCY : currency)
						.course(Course.builder().id(courseId).build()).build())
				.collect(Collectors.toList());
	}

	private TraineeCourseEnrollmentDto toTraineeCourseEnrollmentDto(TraineeCourseEnrollment traineeCourseEnrollment) {
		TraineeCourseEnrollmentDto traineeCourseEnrollmentDto = new TraineeCourseEnrollmentDto();
		traineeCourseEnrollmentDto.setId(traineeCourseEnrollment.getId());
		traineeCourseEnrollmentDto.setTraineeUserId(traineeCourseEnrollment.getTraineeUserProfile().getId());
		traineeCourseEnrollmentDto
				.setUserProfile(modelMapper.map(traineeCourseEnrollment.getTraineeUserProfile(), UserProfileDto.class));
		traineeCourseEnrollmentDto.setPaymentSchedule(traineeCourseEnrollment.getPaymentSchedule());
		if (traineeCourseEnrollment.getAmount() == null) {
			traineeCourseEnrollment.getCourse().getPaymentOptions().stream()
					.filter(coursePaymentOptionsMapping -> coursePaymentOptionsMapping
							.getPaymentSchedule() == traineeCourseEnrollment.getPaymentSchedule())
					.findFirst().ifPresent(coursePaymentOptionsMapping -> traineeCourseEnrollmentDto
							.setAmount(coursePaymentOptionsMapping.getPaymentAmount()));
		} else {
			traineeCourseEnrollmentDto.setAmount(traineeCourseEnrollment.getAmount());
		}

		if (traineeCourseEnrollment.getDiscountAmount() == null) {
			traineeCourseEnrollmentDto.setDiscountAmount(0L);
		} else {
			traineeCourseEnrollmentDto.setDiscountAmount(traineeCourseEnrollment.getDiscountAmount());
		}

		if (traineeCourseEnrollment.getFinalDueAmount() != null) {
			traineeCourseEnrollmentDto.setFinalDueAmount(traineeCourseEnrollment.getFinalDueAmount());
		}

		if (traineeCourseEnrollment.getUseForFuture() != null) {
			traineeCourseEnrollmentDto.setUseForFuture(traineeCourseEnrollment.getUseForFuture());
		}

		if (traineeCourseEnrollment.getDuesOn() != null) {
			traineeCourseEnrollmentDto.setDuesOn(traineeCourseEnrollment.getDuesOn());
		}

		if (traineeCourseEnrollment.getDueDate() != null) {
			traineeCourseEnrollmentDto.setDueDate(traineeCourseEnrollment.getDueDate());
		}

		if (traineeCourseEnrollment.getJoiningDate() != null) {
			traineeCourseEnrollmentDto.setJoiningDate(traineeCourseEnrollment.getJoiningDate());
		}

		return traineeCourseEnrollmentDto;
	}

	@Override
	public ScheduleFileDto toScheduleFileDto(ScheduleFile scheduleFile) {
		ScheduleFileDto scheduleFileDto = new ScheduleFileDto();
		scheduleFileDto.setId(scheduleFile.getId());
		scheduleFileDto.setFileName(scheduleFile.getFileName());
		scheduleFileDto.setFileUrl(scheduleFile.getFileUrl());
		scheduleFileDto.setCreatedAt(scheduleFile.getCreatedAt().toLocalDateTime().toString());
		return scheduleFileDto;
	}
}
