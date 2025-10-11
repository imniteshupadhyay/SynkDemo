package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.AttributeType;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.constants.PushNotifConstants;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.CoachPerformanceReport;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.TraineePerformanceReport;
import com.playmotech.api.core.dao_postgres.TraineePerformanceReportMediaMapping;
import com.playmotech.api.core.dao_postgres.UserExpertiseMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.Attribute;
import com.playmotech.api.core.dto.Category;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CourseMinDto;
import com.playmotech.api.core.dto.EditTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.SubmitTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.ToggleTraineeStatusDto;
import com.playmotech.api.core.dto.TraineeDetailsDto;
import com.playmotech.api.core.dto.TraineePaymentDetailsDto;
import com.playmotech.api.core.dto.TraineePerformanceReportDto;
import com.playmotech.api.core.dto.TraineePerformanceReportPdfDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.mapper.PlayerMapper;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CoachPerformanceRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineePerformanceRepo;
import com.playmotech.api.core.repo.TraineeViewRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.TraineeViewDao;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.specification.TraineeSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.CoachRoleValidator;
import com.playmotech.api.core.views.TraineeView;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TraineeService implements ITraineeService {
    @Value("${performance-media-base-url}")
    private String performanceMediaBaseUrl;

    @Value("${storage.performance-media-bucket}")
    private String performanceMediaBucket;

    @Value("${performance-report-template-config-path}")
    private String performanceReportTemplateConfigPath;

    @Value("${performance-report-url}")
    private String performanceReportUrl;

    private static final String PERF_REPORT_SUBMITTED_TITLE = "Performance Report Submitted";
    private static final String PERF_REPORT_SUBMITTED_BODY = "Your coach has submitted a performance report. Click to review it.";
    private static final String PLAYMO_LOGO_S3_URL = "https://sta-sportsrally-static-assets.s3.us-east-1.amazonaws.com/defaults/PlayMo_Logo.png";
    private final IUserProfileService userProfileService;
    private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
    private final IAcademyService academyService;
    private final ICourseService courseService;
    private final IPaymentService paymentService;
    private final TraineePerformanceRepo traineePerformanceRepo;
    private final IStorageService storageService;
    private final IPushNotificationService notificationService;
    private final UserProfileRepo userProfileRepo;
    private final AcademyDomainUtil academyDomainUtil;
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;
    private final CoachPerformanceRepo coachPerformanceRepo;
    private final TraineeViewRepo traineeViewRepo;
    private final NewPaymentService newPaymentService;
    private final TemplateEngine templateEngine;
    private final CoachIncentiveService coachIncentiveService;
    private final ModelMapper modelMapper = new ModelMapper();

    public TraineeService(IUserProfileService userProfileService, TraineeAcademyMappingRepo traineeAcademyMappingRepo,
            IStorageService storageService, IAcademyService academyService, ICourseService courseService,
            IPaymentService paymentService, TraineePerformanceRepo traineePerformanceRepo,
            IPushNotificationService notificationService, UserProfileRepo userProfileRepo,
            AcademyDomainUtil academyDomainUtil, CoachAcademyMappingRepo coachAcademyMappingRepo,
            TraineeViewRepo traineeViewRepo, CoachPerformanceRepo coachPerformanceRepo, TemplateEngine templateEngine,
            NewPaymentService newPaymentService, CoachIncentiveService coachIncentiveService) {
        this.userProfileService = userProfileService;
        this.traineeAcademyMappingRepo = traineeAcademyMappingRepo;
        this.academyService = academyService;
        this.courseService = courseService;
        this.paymentService = paymentService;
        this.traineePerformanceRepo = traineePerformanceRepo;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.userProfileRepo = userProfileRepo;
        this.academyDomainUtil = academyDomainUtil;
        this.coachAcademyMappingRepo = coachAcademyMappingRepo;
        this.coachPerformanceRepo = coachPerformanceRepo;
        this.traineeViewRepo = traineeViewRepo;
        this.newPaymentService = newPaymentService;
        this.templateEngine = templateEngine;
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        this.coachIncentiveService = coachIncentiveService;
    }

    @Override
    public void addTraineesToAcademy(List<String> traineeUserIds, String academyId) throws ResourceException {
        AcademyDto academyDto = academyService.getAcademyById(academyId);
        if (academyDto == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
        }

        List<String> existingTraineesUsersInAcademy = traineeAcademyMappingRepo
                .findByAcademy_IdAndTraineeUserProfile_IdIn(academyId, traineeUserIds).stream()
                .map(traineeAcademyMapping -> traineeAcademyMapping.getTraineeUserProfile().getId()).toList();
        List<String> notifyUsers = new ArrayList<>();
        List<TraineeAcademyMapping> traineeAcademyMappings = new ArrayList<>();
        for (String traineeUserId : traineeUserIds) {
            if (existingTraineesUsersInAcademy.contains(traineeUserId)) {
                continue;
            }
            TraineeAcademyMapping traineeAcademyMapping = new TraineeAcademyMapping();
            traineeAcademyMapping.setId(UUID.randomUUID().toString());
            traineeAcademyMapping.setAcademy(Academy.builder().id(academyId).build());
            traineeAcademyMapping.setTraineeUserProfile(UserProfile.builder().id(traineeUserId).build());
            traineeAcademyMapping.setCreatedOn(Timestamp.from(Instant.now()));
            traineeAcademyMapping.setLastStatusUpdateTime(Timestamp.from(Instant.now()));
            traineeAcademyMapping.setStatus(Status.ACTIVE);
            traineeAcademyMappings.add(traineeAcademyMapping);
            notifyUsers.add(traineeUserId);
        }

        traineeAcademyMappingRepo.saveAll(traineeAcademyMappings);

        CompletableFuture.runAsync(() -> {
            if (!notifyUsers.isEmpty()) {
                try {
                    List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(notifyUsers);
                    for (UserProfileDto userProfileDto : userProfileDtos) {
                        if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
                            continue;
                        }
                        log.info("Subscribing user to academy topic: {}", userProfileDto.getId());
                        notificationService.subscribeToTopic(userProfileDto.getAndroidFcmPushToken(),
                                String.format(PushNotifConstants.ACADEMY_SUBSCRIPTION_NAME, academyId));
                    }
                } catch (ResourceException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    @Override
    public List<TraineeAcademyMapping> getEnrollmentsByAcademyId(String academyId, Long startEpoch, Long endEpoch)
            throws ResourceException {
        academyService.getAcademyById(academyId);
        return endEpoch != null
                ? traineeAcademyMappingRepo.findByAcademy_IdAndCreatedOnLessThanAndCreatedOnGreaterThanEqual(academyId,
                        Timestamp.from(Instant.ofEpochSecond(endEpoch)),
                        Timestamp.from(Instant.ofEpochSecond(startEpoch)))
                : traineeAcademyMappingRepo.findByAcademy_IdAndCreatedOnGreaterThanEqual(academyId,
                        Timestamp.from(Instant.ofEpochSecond(startEpoch)));
    }

    @Override
    public List<TraineeAcademyMapping> getTraineeAcademyMappingByTrainee(String traineeUserId)
            throws ResourceException {
        return traineeAcademyMappingRepo.findByTraineeUserProfile_Id(traineeUserId).stream()
                .filter(traineeAcademyMapping -> traineeAcademyMapping.getStatus() == Status.ACTIVE).toList();
    }

    @Override
    public TraineeDetailsDto getTraineeById(String academyId, String traineeUserId) throws ResourceException {
        academyService.getAcademyById(academyId);
        Optional<TraineeAcademyMapping> traineeAcademyMapping = traineeAcademyMappingRepo
                .findByAcademy_IdAndTraineeUserProfile_Id(academyId, traineeUserId);
        if (traineeAcademyMapping.isPresent() && traineeAcademyMapping.get().getStatus() == Status.ACTIVE) {
            TraineeDetailsDto traineeDetailsDto = modelMapper.map(traineeAcademyMapping.get(), TraineeDetailsDto.class);
            traineeDetailsDto.setUserProfile(userProfileService.getUserProfileById(traineeUserId));
            return traineeDetailsDto;
        }
        throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Trainee not found. ID: " + traineeUserId);
    }

    @Override
    public List<TraineeDetailsDto> getTraineesByAcademyIdAndNameAndPhoneNumber(String academyId, String name,
            String phone, String searchTxt) throws ResourceException {
        AcademyDto academyDto = academyService.getAcademyById(academyId);
        if (academyDto == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
        }

        List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo.findByAcademy_Id(academyId);

        if (!CollectionUtils.isEmpty(traineeAcademyMappings) && StringUtils.isNotEmpty(searchTxt)
                && searchTxt.length() > 2) {
            traineeAcademyMappings = traineeAcademyMappings.stream().filter(traineeAcademyMapping -> {
                UserProfile userProfile = traineeAcademyMapping.getTraineeUserProfile();
                return userProfile.getDisplayName().toLowerCase().contains(searchTxt.toLowerCase())
                        || userProfile.getUsername().toLowerCase().contains(searchTxt.toLowerCase());
            }).toList();
        }

        if (CollectionUtils.isEmpty(traineeAcademyMappings)) {
            return new ArrayList<>();
        }

        List<TraineeDetailsDto> traineeDetailDtos = traineeAcademyMappings.stream().map(traineeAcademyMapping -> {
            TraineeDetailsDto traineeDetailsDto = new TraineeDetailsDto();
            traineeDetailsDto.setTraineeUserId(traineeAcademyMapping.getTraineeUserProfile().getId());
            traineeDetailsDto.setStatus(traineeAcademyMapping.getStatus());

            traineeDetailsDto.setUserProfile(
                    modelMapper.map(traineeAcademyMapping.getTraineeUserProfile(), UserProfileDto.class));

            List<UserExpertiseMapping> userExpertiseMappings = traineeAcademyMapping.getTraineeUserProfile()
                    .getExpertiseLevel();
            if (!CollectionUtils.isEmpty(userExpertiseMappings)) {
                traineeDetailsDto.getUserProfile().setExpertiseLevel(userExpertiseMappings.stream()
                        .collect(Collectors.toMap(UserExpertiseMapping::getSport, UserExpertiseMapping::getExpertise)));
            }
            return traineeDetailsDto;
        }).toList();

        return sort(traineeDetailDtos);
    }

    @Transactional
    @Override
    public void removeTraineesFromAcademy(List<String> traineeUserIds, String academyId) throws ResourceException {
        AcademyDto academyDto = academyService.getAcademyById(academyId);
        if (academyDto == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
        }

        List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                .findByAcademy_IdAndTraineeUserProfile_IdIn(academyId, traineeUserIds);
        traineeAcademyMappingRepo.deleteAll(traineeAcademyMappings);
        traineeAcademyMappings
                .forEach(traineeAcademyMapping -> courseService.unEnrollTraineeFromAllCourseInAcademy(academyId,
                        traineeAcademyMapping.getTraineeUserProfile().getId()));
        CompletableFuture.runAsync(() -> {
            if (!traineeAcademyMappings.isEmpty()) {
                try {
                    List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(traineeUserIds);
                    for (UserProfileDto userProfileDto : userProfileDtos) {
                        if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
                            continue;
                        }
                        log.info("Unsubscribing user from academy topic: {}", userProfileDto.getId());
                        notificationService.unsubscribeToTopic(userProfileDto.getAndroidFcmPushToken(),
                                String.format(PushNotifConstants.ACADEMY_SUBSCRIPTION_NAME, academyId));
                    }

                } catch (ResourceException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    @Override
    public void toggleTraineeStatusInAcademy(String academyId, ToggleTraineeStatusDto toggleTraineeStatusDto)
            throws ResourceException {
        AcademyDto academyDto = academyService.getAcademyById(academyId);
        if (academyDto == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
        }
        List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                .findByAcademy_IdAndTraineeUserProfile_IdIn(academyId, toggleTraineeStatusDto.getTraineeUserIds());
        traineeAcademyMappings.forEach(traineeAcademyMapping -> {
            traineeAcademyMapping.setStatus(toggleTraineeStatusDto.getStatus());
            traineeAcademyMapping.setLastStatusUpdateTime(Timestamp.from(Instant.now()));
        });
        traineeAcademyMappingRepo.saveAll(traineeAcademyMappings);
    }

    @Override
    public List<TraineePaymentDetailsDto> getTraineePayments(String academyId, String traineeUserId,
            PaymentCategory category) throws ResourceException {
        academyService.getAcademyById(academyId);
        List<TraineePaymentDetailsDto> traineePaymentDetailsDtos = new ArrayList<>();
        List<CourseDto> courseDtos = courseService.getEnrolledCourses(academyId, traineeUserId, null, null);
        Map<String, CourseDto> courseDtoMap = courseDtos.stream()
                .collect(Collectors.toMap(CourseDto::getId, courseDto -> courseDto));
        List<TraineeCourseEnrollment> traineeCourseEnrollments = courseService.getEnrollmentsByTraineeUserId(academyId,
                traineeUserId);

        for (TraineeCourseEnrollment traineeCourseEnrollment : traineeCourseEnrollments) {
            PaymentDetailsDto paymentDetailsDto = paymentService.getPaymentDetails(traineeCourseEnrollment.getId(),
                    category);
            TraineePaymentDetailsDto traineePaymentDetailsDto = new TraineePaymentDetailsDto();
            traineePaymentDetailsDto.setEnrollmentId(traineeCourseEnrollment.getId());
            traineePaymentDetailsDto.setCourseId(courseDtoMap.get(traineeCourseEnrollment.getCourse().getId()).getId());
            traineePaymentDetailsDto
                    .setCourseName(courseDtoMap.get(traineeCourseEnrollment.getCourse().getId()).getTitle());
            traineePaymentDetailsDto.setCourseScheduleType(
                    courseDtoMap.get(traineeCourseEnrollment.getCourse().getId()).getSchedule().getType());
            traineePaymentDetailsDto.setCurrency(paymentDetailsDto.getCurrency());
            traineePaymentDetailsDto.setIsDuePaymentPending(paymentDetailsDto.getIsDue());
            traineePaymentDetailsDto.setDueAmount(paymentDetailsDto.getNextDueAmount());
            traineePaymentDetailsDto.setPaymentHistory(paymentDetailsDto.getPaymentHistory());
            traineePaymentDetailsDto.setIsInProgress(paymentDetailsDto.getIsInProgress());
            traineePaymentDetailsDtos.add(traineePaymentDetailsDto);
        }
        return sortTraineePaymentDetailsDto(traineePaymentDetailsDtos);
    }

    @Override
    public List<TraineePaymentDetailsDto> getNewTraineePayments(String academyId, String traineeUserId,
            PaymentCategory category) throws ResourceException {

        // ✅ Validate academy existence
        academyService.getAcademyById(academyId);

        List<TraineePaymentDetailsDto> traineePaymentDetailsDtos = new ArrayList<>();

        // ✅ Get all enrolled courses and map by ID
        List<CourseDto> courseDtos = courseService.getEnrolledCourses(academyId, traineeUserId, null, null);
        Map<String, CourseDto> courseDtoMap = courseDtos.stream()
                .collect(Collectors.toMap(CourseDto::getId, courseDto -> courseDto));

        // ✅ Get all enrollments for the trainee
        List<TraineeCourseEnrollment> traineeCourseEnrollments = courseService.getEnrollmentsByTraineeUserId(academyId,
                traineeUserId);

        // ✅ Iterate over enrollments and build response
        for (TraineeCourseEnrollment traineeCourseEnrollment : traineeCourseEnrollments) {

            ServiceResponse serviceResponse = newPaymentService.getPaymentDetails(traineeCourseEnrollment.getId(),
                    category);
            PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

            if (paymentDetailsDto == null) {
                log.warn("⚠️ PaymentDetailsDto is null for enrollmentId: {}", traineeCourseEnrollment.getId());
                continue; // ⛔ Skip this record
            }

            TraineePaymentDetailsDto traineePaymentDetailsDto = new TraineePaymentDetailsDto();
            traineePaymentDetailsDto.setEnrollmentId(traineeCourseEnrollment.getId());

            CourseDto courseDto = courseDtoMap.get(traineeCourseEnrollment.getCourse().getId());
            if (courseDto != null) {
                traineePaymentDetailsDto.setCourseId(courseDto.getId());
                traineePaymentDetailsDto.setCourseName(courseDto.getTitle());
                traineePaymentDetailsDto.setCourseScheduleType(
                        courseDto.getSchedule() != null ? courseDto.getSchedule().getType() : null);
            }

            // ✅ Null-safe field handling
            traineePaymentDetailsDto.setCurrency(
                    paymentDetailsDto.getCurrency() != null ? paymentDetailsDto.getCurrency() : Currency.INR);
            traineePaymentDetailsDto.setIsDuePaymentPending(
                    paymentDetailsDto.getIsDue() != null ? paymentDetailsDto.getIsDue() : false);
            traineePaymentDetailsDto.setDueAmount(
                    paymentDetailsDto.getPendingAmount() != null ? paymentDetailsDto.getPendingAmount() : 0L);
            traineePaymentDetailsDto.setIsInProgress(
                    paymentDetailsDto.getIsInProgress() != null ? paymentDetailsDto.getIsInProgress() : false);
            traineePaymentDetailsDto.setPaymentHistory(safeMap(paymentDetailsDto.getPaymentHistory()));

            traineePaymentDetailsDtos.add(traineePaymentDetailsDto);
        }

        return sortTraineePaymentDetailsDto(traineePaymentDetailsDtos);
    }

    public static <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return map != null ? map : new HashMap<>();
    }

    @Override
    public TraineePerformanceReportDto submitTraineePerformanceReport(String coachUserId, String traineeUserId,
            SubmitTraineePerformanceReportRequestDto traineePerformanceDto, List<FileObjectDto> fileObjectDtos)
            throws ResourceException {
        if (!StringUtils.isEmpty(traineePerformanceDto.getAcademyId())) {
            academyService.getAcademyById(traineePerformanceDto.getAcademyId());
        }
        String perfReportId = UUID.randomUUID().toString();
        TraineePerformanceReport traineePerformance = modelMapper.map(traineePerformanceDto,
                TraineePerformanceReport.class);
        traineePerformance.setId(perfReportId);
        traineePerformance.setTraineeUserProfile(UserProfile.builder().id(traineeUserId).build());
        traineePerformance.setTitle(traineePerformanceDto.getTitle());
        traineePerformance.setCoachUserProfile(UserProfile.builder().id(coachUserId).build());
        traineePerformance.setAcademy(StringUtils.isEmpty(traineePerformanceDto.getAcademyId()) ? null
                : Academy.builder().id(traineePerformanceDto.getAcademyId()).build());
        traineePerformance.setCourse(StringUtils.isEmpty(traineePerformanceDto.getCourseId()) ? null
                : Course.builder().id(traineePerformanceDto.getCourseId()).build());
        traineePerformance.setCreatedOn(Timestamp.from(Instant.now()));
        traineePerformance.setReportJson(calculateCategoryRating(traineePerformanceDto.getReport()));
        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<String> mediaUrlPaths = new ArrayList<>();
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                String prefix = "trainee-performance/" + traineeUserId + "/" + coachUserId + "/" + perfReportId + "_"
                        + fileObjectDto.getOriginalFilename();
                storageService.upload(performanceMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());
                mediaUrlPaths.add(prefix);
            }
            traineePerformance.setMediaMappings(buildTraineePerfReportMappings(perfReportId, mediaUrlPaths));
        }

        traineePerformanceRepo.save(traineePerformance);

        CompletableFuture.runAsync(() -> {
            if (traineePerformance.getStatus() == PerformanceReportStatus.SUBMITTED) {
                try {
                    Map<String, String> extraArg = new HashMap<>();
                    extraArg.put("perfReportId", traineePerformance.getId());
                    extraArg.put("courseId", traineePerformanceDto.getCourseId());
                    UserProfileDto userProfileDto = userProfileService.getUserProfileById(traineeUserId);
                    log.info("Sending performance report notification to user: {}", userProfileDto.getDisplayName());
                    notificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
                            NotificationType.LIVE_NOTIFICATION, PERF_REPORT_SUBMITTED_TITLE, PERF_REPORT_SUBMITTED_BODY,
                            "LIST_TRAINEE_PERF_REPORT", CtaType.SCREEN, extraArg);
                    notificationService.addNotification(List.of(userProfileDto.getId()),
                            "Your coach has submitted a performance report. Click to review it.", CtaType.SCREEN,
                            "LIST_TRAINEE_PERF_REPORT", extraArg);
                } catch (ResourceException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Optional<CoachAcademyMapping> coachAcademyMappingOptional = coachAcademyMappingRepo
                .findByAcademy_IdAndCoachUserProfile_Id(traineePerformance.getAcademy().getId(), coachUserId);

        // Only reward if the traineePerformanceReport status is SUBMITTED
        boolean shouldRewardCoach = (traineePerformance.getStatus() == PerformanceReportStatus.SUBMITTED)
                && coachAcademyMappingOptional.isPresent()
                && coachAcademyMappingOptional.get().getRoleId() != null
                && CoachRoleValidator.ACCEPTABLE_ROLE_IDS.contains(coachAcademyMappingOptional.get().getRoleId());

        if (shouldRewardCoach) {
            log.info(
                    "[TraineeService] The user is indeed a coach in the provided academy!!! Rewarding the coach with some points :)");
            CompletableFuture.runAsync(() -> {
                try {
                    rewardCoach(coachUserId, traineePerformance);
                } catch (ResourceException e) {
                    log.error("Unable to reward coach: coachId: {}", coachUserId);
                    log.error("[TraineeService] Check the (coach_point_transaction_error) table");
                }
            });
        } else {
            log.warn(
                    "No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
        }

        return getTraineePerformanceById(perfReportId);
    }

    private void rewardCoach(String coachUserId, TraineePerformanceReport traineePerformance) throws ResourceException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("performanceReportId", traineePerformance.getId());
        metadata.put("title", traineePerformance.getTitle());
        metadata.put("traineeUserId", traineePerformance.getTraineeUserProfile().getId());
        metadata.put("academyId",
                traineePerformance.getAcademy() != null ? traineePerformance.getAcademy().getId() : null);
        metadata.put("courseId",
                traineePerformance.getCourse() != null ? traineePerformance.getCourse().getId() : null);

        coachIncentiveService.awardPoints(
                coachUserId,
                IncentiveSourceType.PERFORMANCE_REPORT,
                traineePerformance.getId(),
                traineePerformance.getAcademy().getId(),
                metadata);
    }

    @Override
    public void editTraineePerformanceReport(String id, String coachUserId, String traineeUserId,
            EditTraineePerformanceReportRequestDto traineePerformanceDto, List<FileObjectDto> fileObjectDtos)
            throws ResourceException {
        TraineePerformanceReport traineePerformance = traineePerformanceRepo.findById(id)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Trainee performance report not found. ID: " + id));
        if (traineePerformance.getStatus() == PerformanceReportStatus.SUBMITTED) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Cannot edit an submitted performance report.");
        }
        traineePerformance.setUpdatedOn(Timestamp.from(Instant.now()));
        traineePerformance.setReportJson(calculateCategoryRating(traineePerformanceDto.getReport()));
        traineePerformance.setStatus(traineePerformanceDto.getStatus());
        traineePerformance.setTitle(traineePerformanceDto.getTitle());
        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<String> mediaUrlPaths = new ArrayList<>();
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                String prefix = "trainee-performance/" + traineeUserId + "/" + coachUserId + "/" + id + "_"
                        + fileObjectDto.getOriginalFilename();
                storageService.upload(performanceMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());
                mediaUrlPaths.add(prefix);
            }
            traineePerformance.setMediaMappings(buildTraineePerfReportMappings(id, mediaUrlPaths));
        }

        TraineePerformanceReport savedTraineePerformanceReport = traineePerformanceRepo.save(traineePerformance);

        Optional<CoachAcademyMapping> coachAcademyMappingOptional = coachAcademyMappingRepo
                .findByAcademy_IdAndCoachUserProfile_Id(savedTraineePerformanceReport.getAcademy().getId(),
                        coachUserId);
        // Only reward if the traineePerformanceReport status is SUBMITTED
        boolean shouldRewardCoach = (savedTraineePerformanceReport.getStatus() == PerformanceReportStatus.SUBMITTED)
                && coachAcademyMappingOptional.isPresent()
                && coachAcademyMappingOptional.get().getRoleId() != null
                && CoachRoleValidator.ACCEPTABLE_ROLE_IDS.contains(coachAcademyMappingOptional.get().getRoleId());

        if (shouldRewardCoach) {
            log.info(
                    "[TraineeService] The user is indeed a coach in the provided academy!!! Rewarding the coach with some points :)");
            CompletableFuture.runAsync(() -> {
                try {
                    rewardCoach(coachUserId, savedTraineePerformanceReport);
                } catch (ResourceException e) {
                    log.error("Unable to reward coach: coachId: {}", coachUserId);
                    log.error("[TraineeService] Check the (coach_point_transaction_error) table");
                }
            });
        } else {
            log.warn(
                    "No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
        }
    }

    @Override
    public List<TraineePerformanceReportDto> getTraineePerformances(String traineeUserId, String courseId)
            throws ResourceException {
        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();
        UserProfileDto traineeUserProfile = userProfileService.getUserProfileById(traineeUserId);
        List<TraineePerformanceReport> traineePerformances = StringUtils.isEmpty(courseId)
                ? traineePerformanceRepo.findByTraineeUserProfile_IdAndStatus(traineeUserId,
                        PerformanceReportStatus.SUBMITTED)
                : traineePerformanceRepo.findByTraineeUserProfile_IdAndStatusAndCourse_Id(traineeUserId,
                        PerformanceReportStatus.SUBMITTED, courseId);
        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        // List<String> coachUserIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getCoachUserId).toList();
        // List<String> academyIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getAcademyId).filter(academyId
        // -> !StringUtils.isEmpty(academyId)).toList();
        // Map<String, UserProfileDto> coachUserProfileMap =
        // userProfileService.getUserProfileByIds(coachUserIds).stream().collect(Collectors.toMap(UserProfileDto::getId,
        // userProfileDto -> userProfileDto));
        // Map<String, AcademyDto> academyDtoMap =
        // academyService.getAcademyByIds(academyIds).stream().collect(Collectors.toMap(AcademyDto::getId,
        // academyDto -> academyDto));

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);
            traineePerformanceDto.setReport(traineePerformance.getReportJson());
            traineePerformanceDto
                    .setCoach(modelMapper.map(traineePerformance.getCoachUserProfile(), UserProfileMinDto.class));
            traineePerformanceDto.setTrainee(modelMapper.map(traineeUserProfile, UserProfileMinDto.class));
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
                // traineePerformanceDto.setMediaUrls(traineePerformance.getMediaUrlPaths()
                // .stream().map(mediaUrl ->
                // performanceMediaBaseUrl.concat(mediaUrl)).collect(Collectors.toList()));
            }
            traineePerformanceDtos.add(traineePerformanceDto);
        }

        return traineePerformanceDtos;
    }

    @Override
    public List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserId(String coachUserId,
            PerformanceReportStatus performanceReportStatus) throws ResourceException {
        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();
        UserProfileDto coachUserProfile = userProfileService.getUserProfileById(coachUserId);
        List<TraineePerformanceReport> traineePerformances = performanceReportStatus == null
                ? traineePerformanceRepo.findByCoachUserProfile_Id(coachUserId)
                : traineePerformanceRepo.findByCoachUserProfile_IdAndStatus(coachUserId, performanceReportStatus);
        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        // List<String> traineeUserIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getTraineeUserId).toList();
        // List<String> academyIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getAcademyId).filter(academyId
        // -> !StringUtils.isEmpty(academyId)).toList();
        // Map<String, UserProfileDto> traineeUserProfileMap =
        // userProfileService.getUserProfileByIds(traineeUserIds).stream().collect(Collectors.toMap(UserProfileDto::getId,
        // userProfileDto -> userProfileDto));
        // Map<String, AcademyDto> academyDtoMap =
        // academyService.getAcademyByIds(academyIds).stream().collect(Collectors.toMap(AcademyDto::getId,
        // academyDto -> academyDto));

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);
            traineePerformanceDto.setReport(traineePerformance.getReportJson());
            traineePerformanceDto.setCoach(modelMapper.map(coachUserProfile, UserProfileMinDto.class));
            traineePerformanceDto
                    .setTrainee(modelMapper.map(traineePerformance.getTraineeUserProfile(), UserProfileMinDto.class));
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
            }
            traineePerformanceDtos.add(traineePerformanceDto);

        }

        return traineePerformanceDtos;
    }

    @Override
    public List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndAcademyId(String academyId,
            String userId, PerformanceReportStatus performanceReportStatus, String courseId) throws ResourceException {
        academyService.getAcademyById(academyId);

        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();

        // Get performance reports under the provided academyId and/or status
        List<TraineePerformanceReport> traineePerformances = performanceReportStatus == null
                ? traineePerformanceRepo.findByAcademy_Id(academyId)
                : traineePerformanceRepo.findByAcademy_IdAndStatus(academyId, performanceReportStatus);

        // Filter by courseId if provided
        if (!StringUtils.isEmpty(courseId)) {
            traineePerformances = traineePerformances.stream()
                    .filter(traineePerformance -> traineePerformance.getCourse() != null
                            && courseId.equalsIgnoreCase(traineePerformance.getCourse().getId()))
                    .toList();
        }

        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);

            traineePerformanceDto.setReport(filterReportCategories(traineePerformance.getReportJson()));
            // traineePerformanceDto.setCoach(modelMapper.map(coachUserProfile,
            // UserProfileMinDto.class));

            if (traineePerformance.getCoachUserProfile() != null) {
                traineePerformanceDto
                        .setCoach(modelMapper.map(traineePerformance.getCoachUserProfile(), UserProfileMinDto.class));
            }

            traineePerformanceDto
                    .setTrainee(modelMapper.map(traineePerformance.getTraineeUserProfile(), UserProfileMinDto.class));
            if (traineePerformance.getCourse() != null) {
                traineePerformanceDto.setCourse(modelMapper.map(traineePerformance.getCourse(), CourseMinDto.class));
            }
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
            }
            traineePerformanceDtos.add(traineePerformanceDto);
        }

        return traineePerformanceDtos;
    }

    // @Override
    public List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndAcademyIdOld(String academyId,
            String coachUserId, PerformanceReportStatus performanceReportStatus, String courseId)
            throws ResourceException {
        academyService.getAcademyById(academyId);
        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();
        UserProfileDto coachUserProfile = userProfileService.getUserProfileById(coachUserId);
        List<TraineePerformanceReport> traineePerformances = performanceReportStatus == null
                ? traineePerformanceRepo.findByCoachUserProfile_IdAndAcademy_Id(coachUserId, academyId)
                : traineePerformanceRepo.findByCoachUserProfile_IdAndAcademy_IdAndStatus(coachUserId, academyId,
                        performanceReportStatus);
        if (!StringUtils.isEmpty(courseId)) {
            traineePerformances = traineePerformances.stream()
                    .filter(traineePerformance -> traineePerformance.getCourse() != null
                            && courseId.equalsIgnoreCase(traineePerformance.getCourse().getId()))
                    .toList();
        }
        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        // List<String> traineeUserIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getTraineeUserId).toList();
        // List<String> academyIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getAcademyId).filter(academyIdTmp
        // -> !StringUtils.isEmpty(academyIdTmp)).toList();
        // Map<String, UserProfileDto> traineeUserProfileMap =
        // userProfileService.getUserProfileByIds(traineeUserIds).stream().collect(Collectors.toMap(UserProfileDto::getId,
        // userProfileDto -> userProfileDto));
        // Map<String, AcademyDto> academyDtoMap =
        // academyService.getAcademyByIds(academyIds).stream().collect(Collectors.toMap(AcademyDto::getId,
        // academyDto -> academyDto));

        // List<String> courseIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getCourseId).filter(id
        // -> !StringUtils.isEmpty(id)).toList();
        // Map<String, CourseDto> courseDtoMap = courseService.getCourses(academyId,
        // courseIds, null, null).stream().collect(Collectors.toMap(CourseDto::getId,
        // courseDto -> courseDto));

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);
            traineePerformanceDto.setReport(traineePerformance.getReportJson());
            traineePerformanceDto.setCoach(modelMapper.map(coachUserProfile, UserProfileMinDto.class));
            traineePerformanceDto
                    .setTrainee(modelMapper.map(traineePerformance.getTraineeUserProfile(), UserProfileMinDto.class));
            if (traineePerformance.getCourse() != null) {
                traineePerformanceDto.setCourse(modelMapper.map(traineePerformance.getCourse(), CourseMinDto.class));
            }
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
            }
            traineePerformanceDtos.add(traineePerformanceDto);

        }

        return traineePerformanceDtos;
    }

    @Override
    public List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndTraineeIdAndAcademyId(
            String academyId, String coachUserId, String traineeId, PerformanceReportStatus performanceReportStatus,
            String courseId) throws ResourceException {
        academyService.getAcademyById(academyId);

        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();
        List<TraineePerformanceReport> traineePerformances = performanceReportStatus == null
                ? traineePerformanceRepo.findByAcademy_Id(academyId)
                : traineePerformanceRepo.findByAcademy_IdAndStatus(academyId, performanceReportStatus);

        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        // Filter by courseId if provided
        if (!StringUtils.isEmpty(courseId)) {
            traineePerformances = traineePerformances.stream()
                    .filter(traineePerformance -> courseId.equalsIgnoreCase(traineePerformance.getCourse().getId()))
                    .toList();
        }

        // Filter by traineeIs if provided
        if (!StringUtils.isEmpty(traineeId)) {
            traineePerformances = traineePerformances.stream()
                    .filter(t -> traineeId.equalsIgnoreCase(t.getTraineeUserProfile().getId())).toList();
        }

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);
            traineePerformanceDto.setReport(filterReportCategories(traineePerformance.getReportJson()));
            // traineePerformanceDto.setCoach(modelMapper.map(coachUserProfile,
            // UserProfileMinDto.class));

            if (traineePerformance.getCoachUserProfile() != null) {
                traineePerformanceDto
                        .setCoach(modelMapper.map(traineePerformance.getCoachUserProfile(), UserProfileMinDto.class));
            }
            traineePerformanceDto
                    .setTrainee(modelMapper.map(traineePerformance.getTraineeUserProfile(), UserProfileMinDto.class));
            if (traineePerformance.getCourse() != null) {
                traineePerformanceDto.setCourse(modelMapper.map(traineePerformance.getCourse(), CourseMinDto.class));
            }
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
            }

            traineePerformanceDtos.add(traineePerformanceDto);

        }

        return traineePerformanceDtos;
    }

    public List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndTraineeIdAndAcademyIdOld(
            String academyId, String coachUserId, String traineeId, PerformanceReportStatus performanceReportStatus,
            String courseId) throws ResourceException {
        academyService.getAcademyById(academyId);
        userProfileService.getUserProfileById(traineeId);
        List<TraineePerformanceReportDto> traineePerformanceDtos = new ArrayList<>();
        UserProfileDto coachUserProfile = userProfileService.getUserProfileById(coachUserId);
        List<TraineePerformanceReport> traineePerformances = performanceReportStatus == null
                ? traineePerformanceRepo.findByCoachUserProfile_IdAndTraineeUserProfile_IdAndAcademy_Id(coachUserId,
                        traineeId, academyId)
                : traineePerformanceRepo.findByCoachUserProfile_IdAndTraineeUserProfile_IdAndAcademy_IdAndStatus(
                        coachUserId, traineeId, academyId, performanceReportStatus);
        if (!StringUtils.isEmpty(courseId)) {
            traineePerformances = traineePerformances.stream()
                    .filter(traineePerformance -> courseId.equalsIgnoreCase(traineePerformance.getCourse().getId()))
                    .toList();
        }
        if (CollectionUtils.isEmpty(traineePerformances)) {
            return List.of();
        }

        // List<String> traineeUserIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getTraineeUserId).toList();
        // List<String> academyIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getAcademyId).filter(academyIdTmp
        // -> !StringUtils.isEmpty(academyIdTmp)).toList();
        // Map<String, UserProfileDto> traineeUserProfileMap =
        // userProfileService.getUserProfileByIds(traineeUserIds).stream().collect(Collectors.toMap(UserProfileDto::getId,
        // userProfileDto -> userProfileDto));
        // Map<String, AcademyDto> academyDtoMap =
        // academyService.getAcademyByIds(academyIds).stream().collect(Collectors.toMap(AcademyDto::getId,
        // academyDto -> academyDto));
        // List<String> courseIds =
        // traineePerformances.stream().map(TraineePerformanceReport::getCourseId).filter(id
        // -> !StringUtils.isEmpty(id)).toList();
        // Map<String, CourseDto> courseDtoMap = courseService.getCourses(academyId,
        // courseIds, null, null).stream().collect(Collectors.toMap(CourseDto::getId,
        // courseDto -> courseDto));

        for (TraineePerformanceReport traineePerformance : traineePerformances) {
            TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance,
                    TraineePerformanceReportDto.class);
            traineePerformanceDto.setReport(traineePerformance.getReportJson());
            traineePerformanceDto.setCoach(modelMapper.map(coachUserProfile, UserProfileMinDto.class));
            traineePerformanceDto
                    .setTrainee(modelMapper.map(traineePerformance.getTraineeUserProfile(), UserProfileMinDto.class));
            if (traineePerformance.getCourse() != null) {
                traineePerformanceDto.setCourse(modelMapper.map(traineePerformance.getCourse(), CourseMinDto.class));
            }
            if (traineePerformance.getAcademy() != null) {
                traineePerformanceDto.setAcademy(modelMapper.map(traineePerformance.getAcademy(), AcademyMinDto.class));
            }
            if (!CollectionUtils.isEmpty(traineePerformance.getMediaMappings())) {
                traineePerformanceDto.setMediaUrls(traineePerformance.getMediaMappings().stream()
                        .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                        .collect(Collectors.toList()));
            }

            traineePerformanceDtos.add(traineePerformanceDto);

        }

        return traineePerformanceDtos;
    }

    // Helper method to filter out categories with rating 0
    private String filterReportCategories(String reportJson) {
        if (StringUtils.isEmpty(reportJson)) {
            return reportJson;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(reportJson);

            Iterator<Map.Entry<String, JsonNode>> sportFields = rootNode.fields();
            while (sportFields.hasNext()) {
                Map.Entry<String, JsonNode> sportEntry = sportFields.next();
                JsonNode categoriesArray = sportEntry.getValue();

                if (categoriesArray.isArray()) {
                    ArrayNode filteredCategories = objectMapper.createArrayNode();
                    for (JsonNode category : categoriesArray) {
                        JsonNode ratingNode = category.get("categoryRating");
                        if (ratingNode != null && !"0".equals(ratingNode.asText())) {
                            filteredCategories.add(category);
                        }
                    }
                    ((ObjectNode) rootNode).set(sportEntry.getKey(), filteredCategories);
                }
            }
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("Error filtering report categories", e);
            return reportJson; // Return original on error
        }
    }

    @Override
    public List<AcademyDto> getMyAcademies(String traineeUserId) throws ResourceException {
        List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                .findByTraineeUserProfile_Id(traineeUserId).stream()
                .filter(traineeAcademyMapping -> traineeAcademyMapping.getStatus() == Status.ACTIVE).toList();
        List<String> academyIds = traineeAcademyMappings.stream()
                .map(traineeAcademyMapping -> traineeAcademyMapping.getAcademy().getId()).toList();
        return academyService.getAcademyByIds(academyIds);
    }

    @Override
    public void submitPerformanceReport(String userId, String performanceReportId) throws ResourceException {
        Optional<TraineePerformanceReport> traineePerformance = traineePerformanceRepo.findById(performanceReportId);
        if (traineePerformance.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "Trainee performance not found. ID: " + performanceReportId);
        }

        if (!traineePerformance.get().getCoachUserProfile().getId().equalsIgnoreCase(userId)) {
            throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User not authorized to submit performance report.");
        }

        traineePerformance.get().setStatus(PerformanceReportStatus.SUBMITTED);

        CompletableFuture.runAsync(() -> {
            Map<String, String> extraArg = new HashMap<>();
            extraArg.put("perfReportId", traineePerformance.get().getId());
            extraArg.put("courseId", traineePerformance.get().getCourse().getId());
            log.info("Sending performance report notification to user: {}",
                    traineePerformance.get().getTraineeUserProfile().getDisplayName());
            notificationService.sendMessageToPushToken(
                    traineePerformance.get().getTraineeUserProfile().getAndroidFcmPushToken(),
                    NotificationType.LIVE_NOTIFICATION, PERF_REPORT_SUBMITTED_TITLE, PERF_REPORT_SUBMITTED_BODY,
                    "LIST_TRAINEE_PERF_REPORT", CtaType.SCREEN, extraArg);
            notificationService.addNotification(List.of(traineePerformance.get().getTraineeUserProfile().getId()),
                    "Your coach has submitted a performance report. Click to review it.", CtaType.SCREEN,
                    "LIST_TRAINEE_PERF_REPORT", extraArg);
        });

        traineePerformanceRepo.save(traineePerformance.get());
    }

    @Override
    public void deletePerformanceReport(String userId, String performanceReportId) throws ResourceException {
        Optional<TraineePerformanceReport> traineePerformanceReport = traineePerformanceRepo
                .findById(performanceReportId);
        if (traineePerformanceReport.isEmpty()
                || !userId.equalsIgnoreCase(traineePerformanceReport.get().getCoachUserProfile().getId())) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "Trainee performance report not found. ID: " + performanceReportId);
        }
        traineePerformanceRepo.deleteById(performanceReportId);
    }

    @Override
    public TraineePerformanceReportPdfDto getPerformanceReportPdf(String userId, String performanceReportId)
            throws ResourceException {
        TraineePerformanceReportPdfDto traineePerformanceReportPdfDto = new TraineePerformanceReportPdfDto();
        traineePerformanceReportPdfDto.setS3Url(performanceReportUrl + performanceReportId);
        // if (!userId.equalsIgnoreCase(traineePerformanceReportDto.getCoach().getId()))
        // throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Trainee
        // performance report not found. ID: " + performanceReportId);
        // ReportPdfDTO reportPdfDTO;
        // try {
        // List<String> filesToDelete = new ArrayList<>();
        // reportPdfDTO =
        // AppConstants.OBJECT_MAPPER.readValue(traineePerformanceReportDto.getReport(),
        // ReportPdfDTO.class);
        // String htmlFilePath = performanceReportTemplateConfigPath +
        // "/report_template.html";
        // String outputPdfPath = performanceReportTemplateConfigPath + "/" +
        // UUID.randomUUID() + ".pdf";
        // filesToDelete.add(outputPdfPath);
        // String htmlContent = parseHtmlContent(new
        // String(Files.readAllBytes(Paths.get(htmlFilePath))), reportPdfDTO,
        // traineePerformanceReportDto, filesToDelete);
        // generatePdfFromHtml(htmlContent, outputPdfPath);
        // String prefix = "trainee-performance/" +
        // traineePerformanceReportDto.getTrainee().getId() + "/" +
        // traineePerformanceReportDto.getCoach().getId() + "/" +
        // traineePerformanceReportDto.getId() + "_" + UUID.randomUUID() + ".pdf";
        // storageService.upload(performanceMediaBucket, prefix,
        // Files.readAllBytes(Paths.get(outputPdfPath)), "application/pdf");
        // traineePerformanceReportPdfDto.setS3Url(performanceMediaBaseUrl.concat(prefix));
        // filesToDelete.stream()
        // .map(File::new)
        // .filter(File::exists)
        // .filter(File::isFile)
        // .forEach(File::delete);
        // } catch (JsonProcessingException e) {
        // throw new RuntimeException(e);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        return traineePerformanceReportPdfDto;
    }

    @Override
    public List<UserProfileMinDto> getRegisteredUsers(String academyId) throws ResourceException {
        Long currentMonthsFirstDayEpoch = DateTimeUtils.getCurrentMonthsFirstDayEpoch();
        List<TraineeAcademyMapping> enrollments = getEnrollmentsByAcademyId(academyId, currentMonthsFirstDayEpoch,
                null);
        List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(enrollments.stream()
                .map(traineeAcademyMapping -> traineeAcademyMapping.getTraineeUserProfile().getId()).toList());
        return userProfileDtos.stream().map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class))
                .collect(Collectors.toList());
    }

    // @Override
    public byte[] generatePerformanceReportPdfThymeLeaf(String performanceReportId) throws ResourceException {
        TraineePerformanceReport report = traineePerformanceRepo.findById(performanceReportId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Trainee performance report not found. ID: " + performanceReportId));
        try {
            Context context = new Context();
            String sportName = report.getSport().name();
            context.setVariable("report", report);
            context.setVariable("sportName", sportName);
            context.setVariable("academyName", report.getAcademy().getName());
            context.setVariable("playerName", report.getTraineeUserProfile().getDisplayName());
            context.setVariable("coachName", report.getCoachUserProfile().getDisplayName());
            context.setVariable("courseName", report.getCourse() != null ? report.getCourse().getTitle() : null);

            // Parse JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(report.getReportJson());
            JsonNode categories = rootNode.get(sportName);

            // Get academy logo
            String academyLogo = report.getAcademy().getOrg().getConfigs().stream()
                    .filter(each -> "academyLogo".equals(each.getKey())).map(each -> each.getValue()).findFirst()
                    .orElse(null);
            context.setVariable("academyLogo", academyLogo);

            // Convert JSON to List for easier template processing
            List<Map<String, Object>> categoryList = new ArrayList<>();
            if (categories != null && categories.isArray()) {
                for (JsonNode categoryNode : categories) {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("category", categoryNode.get("category").asText());
                    categoryMap.put("categoryRating", categoryNode.get("categoryRating").asText());

                    List<Map<String, Object>> attributesList = new ArrayList<>();
                    JsonNode attributes = categoryNode.get("attributes");
                    if (attributes != null && attributes.isArray()) {
                        for (JsonNode attribute : attributes) {
                            Map<String, Object> attrMap = new HashMap<>();
                            attrMap.put("name", attribute.get("name").asText());
                            attrMap.put("type", attribute.get("type").asText());
                            String value = attribute.get("value").asText();

                            // Handle empty values and zero ratings
                            boolean isEmptyOrZero = value == null || value.trim().isEmpty() || "0".equals(value);
                            attrMap.put("value", value);
                            attrMap.put("displayValue", isEmptyOrZero ? "N/A" : value);
                            attrMap.put("isEmpty", isEmptyOrZero);

                            // Add type-specific labels
                            String typeLabel = switch (attribute.get("type").asText()) {
                                case "RATING_1_5" -> "Rating (1-5)";
                                case "RATING_10" -> "Rating (1-10)";
                                case "TEXT" -> {
                                    if (sportName.equals(Sports.CALISTHENICS.name())) {
                                        yield "Number of reps/time";
                                    } else {
                                        yield "Notes/Time";
                                    }
                                }
                                // case "TEXT" -> "Notes/Time";
                                case "DROPDOWN" -> "Selection";
                                case "TIME" -> "Time";
                                default -> "Value";
                            };
                            attrMap.put("typeLabel", typeLabel);

                            attributesList.add(attrMap);
                        }
                    }
                    categoryMap.put("attributes", attributesList);
                    categoryList.add(categoryMap);
                }
            }

            context.setVariable("categories", categoryList);

            // Generate HTML from template
            String htmlContent = templateEngine.process("performance-report", context);

            // Convert HTML to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to generate Performance Report.");
        }
    }

    @Override
    public byte[] generatePerformanceReportPdf(String performanceReportId) throws ResourceException {
        TraineePerformanceReport traineePerformanceReport = traineePerformanceRepo.findById(performanceReportId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Trainee performance report not found. ID: " + performanceReportId));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(traineePerformanceReport.getReportJson());

            String sportName = traineePerformanceReport.getSport().name();
            String academyName = traineePerformanceReport.getAcademy().getName();
            String playerName = traineePerformanceReport.getTraineeUserProfile().getDisplayName();
            String coachName = traineePerformanceReport.getCoachUserProfile().getDisplayName();
            String courseName = traineePerformanceReport.getCourse() != null
                    ? traineePerformanceReport.getCourse().getTitle()
                    : null;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(byteArrayOutputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Academy logo
            String academyLogo = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "academyLogo".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(academyLogo)) {
                ImageData topLogoData = ImageDataFactory.create(new URL(academyLogo));
                Image topLogo = new Image(topLogoData).scaleToFit(450, 150).setMarginTop(-55)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(topLogo);
            }

            document.add(new Paragraph(sportName + " Player Performance Report").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            Table infoTable = new Table(2).useAllAvailableWidth();
            infoTable.setMarginTop(-30);
            infoTable.setFontSize(10);
            infoTable.addCell(new Cell().add(new Paragraph("Academy: " + academyName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Player: " + playerName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Coach: " + coachName)).setBorder(Border.NO_BORDER));
            if (StringUtils.isNotBlank(courseName)) {
                infoTable.addCell(
                        new Cell(1, 2).add(new Paragraph("Course: " + courseName)).setBorder(Border.NO_BORDER));
            }
            document.add(infoTable);

            // Attribute categories
            JsonNode categories = rootNode.get(sportName);
            if (categories != null && categories.isArray()) {
                for (JsonNode categoryNode : categories) {
                    String category = categoryNode.get("category").asText();
                    document.add(new Paragraph(category).setBold().setFontSize(12)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                    Table table = null;
                    JsonNode attributes = categoryNode.get("attributes");
                    if (attributes != null && attributes.isArray()) {
                        for (JsonNode attribute : attributes) {
                            String name = attribute.get("name").asText();
                            String type = attribute.get("type").asText();
                            String value = attribute.get("value").asText();

                            if (table == null) {
                                table = new Table(2).useAllAvailableWidth();
                                table.addCell(new Cell().add(new Paragraph("Attribute")).setBold().setFontSize(10));
                                String label = switch (type) {
                                    case "RATING_1_5" -> "Rating (1-5)";
                                    case "RATING_10" -> "Rating (1-10)";
                                    case "TEXT" -> {
                                        if (sportName.equals(Sports.CALISTHENICS.name())) {
                                            yield "Number of reps/time";
                                        } else {
                                            yield "Notes/Time";
                                        }
                                    }
                                    // case "TEXT" -> "Notes/Time";
                                    case "DROPDOWN" -> "Selection";
                                    case "TIME" -> "Time";
                                    default -> "Value";
                                };
                                table.addCell(new Cell().add(new Paragraph(label)).setFontSize(10).setBold());
                            }

                            boolean isZeroOrEmpty = !org.springframework.util.StringUtils.hasText(value)
                                    || "0".equals(value);
                            table.addCell(new Cell().add(new Paragraph(name).setFontSize(10)));

                            table.addCell(new Cell().add(new Paragraph(isZeroOrEmpty ? "N/A" : value)).setFontSize(10)
                                    .setTextAlignment(TextAlignment.CENTER));
                        }
                    }

                    if (table != null) {
                        document.add(table);
                        document.setFontSize(10);
                        document.add(new Paragraph("\n"));
                    }
                }
            }

            // Create signature table
            Table signatureTable = new Table(2).useAllAvailableWidth();
            signatureTable.setBorder(Border.NO_BORDER);

            Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(5)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);

            // Director's signature - left side
            String directorSignature = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "signature".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(directorSignature)) {
                try {
                    Image directorSignatureImg = new Image(ImageDataFactory.create(new URL(directorSignature)))
                            .scaleToFit(150, 100); // Original size
                    directorSignatureImg.setPaddingLeft(-20);
                    // leftCell.setMarginLeft(-50);
                    leftCell.add(directorSignatureImg);
                } catch (Exception e) {
                    // Handle gracefully
                }
            }

            leftCell.add(new Paragraph("Director").setBold().setFontSize(10));

            // Powered by cell
            Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPadding(5)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM) // Align to bottom
                    .setTextAlignment(TextAlignment.RIGHT);

            // Add PlayMo logo below the text
            try {
                Image playmoImg = new Image(ImageDataFactory.create(new URL(PLAYMO_LOGO_S3_URL))).scaleToFit(150, 100);
                playmoImg.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                // playmoImg.setMarginLeft(-30);
                playmoImg.setPaddingLeft(-50);
                rightCell.add(playmoImg);
            } catch (Exception e) {
                rightCell.add(new Paragraph("PlayMo").setBold().setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
            }

            // Powered by text
            Paragraph poweredByText = new Paragraph("Powered by").setBold().setFontSize(10).setMarginRight(5)
                    .setPadding(2);

            rightCell.add(poweredByText);

            signatureTable.addCell(leftCell);
            signatureTable.addCell(rightCell);
            document.add(signatureTable);

            document.close();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to generate Performance Report.");
        }
    }

    public byte[] generatePerformanceReportPdfPartiallyWorking(String performanceReportId) throws ResourceException {
        TraineePerformanceReport traineePerformanceReport = traineePerformanceRepo.findById(performanceReportId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Trainee performance report not found. ID: " + performanceReportId));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(traineePerformanceReport.getReportJson());

            String sportName = traineePerformanceReport.getSport().name();
            String academyName = traineePerformanceReport.getAcademy().getName();
            String playerName = traineePerformanceReport.getTraineeUserProfile().getDisplayName();
            String coachName = traineePerformanceReport.getCoachUserProfile().getDisplayName();
            String courseName = traineePerformanceReport.getCourse() != null
                    ? traineePerformanceReport.getCourse().getTitle()
                    : null;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(byteArrayOutputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Academy logo
            String academyLogo = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "academyLogo".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(academyLogo)) {
                ImageData topLogoData = ImageDataFactory.create(new URL(academyLogo));
                Image topLogo = new Image(topLogoData).scaleToFit(450, 150).setMarginTop(-50)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(topLogo);
            }

            document.add(new Paragraph(sportName + " Player Performance Report").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            Table infoTable = new Table(2).useAllAvailableWidth();
            infoTable.setFontSize(10);
            infoTable.addCell(new Cell().add(new Paragraph("Academy: " + academyName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Player: " + playerName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Coach: " + coachName)).setBorder(Border.NO_BORDER));
            if (StringUtils.isNotBlank(courseName)) {
                infoTable.addCell(
                        new Cell(1, 2).add(new Paragraph("Course: " + courseName)).setBorder(Border.NO_BORDER));
            }
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Attribute categories
            JsonNode categories = rootNode.get(sportName);
            if (categories != null && categories.isArray()) {
                for (JsonNode categoryNode : categories) {
                    String category = categoryNode.get("category").asText();
                    document.add(new Paragraph(category).setBold().setFontSize(12)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                    Table table = null;
                    JsonNode attributes = categoryNode.get("attributes");
                    if (attributes != null && attributes.isArray()) {
                        for (JsonNode attribute : attributes) {
                            String name = attribute.get("name").asText();
                            String type = attribute.get("type").asText();
                            String value = attribute.get("value").asText();

                            if (table == null) {
                                table = new Table(2).useAllAvailableWidth();
                                table.addCell(new Cell().add(new Paragraph("Attribute")).setBold().setFontSize(10));
                                String label = switch (type) {
                                    case "RATING_1_5" -> "Rating (1-5)";
                                    case "RATING_10" -> "Rating (1-10)";
                                    case "TEXT" -> {
                                        if (sportName.equals(Sports.CALISTHENICS.name())) {
                                            yield "Number of reps/time";
                                        } else {
                                            yield "Notes/Time";
                                        }
                                    }
                                    // case "TEXT" -> "Notes/Time";
                                    case "DROPDOWN" -> "Selection";
                                    case "TIME" -> "Time";
                                    default -> "Value";
                                };
                                table.addCell(new Cell().add(new Paragraph(label)).setFontSize(10).setBold());
                            }

                            boolean isZeroOrEmpty = !org.springframework.util.StringUtils.hasText(value)
                                    || "0".equals(value);
                            table.addCell(new Cell().add(new Paragraph(name).setFontSize(10)));

                            table.addCell(new Cell().add(new Paragraph(isZeroOrEmpty ? "N/A" : value)).setFontSize(10)
                                    .setTextAlignment(TextAlignment.CENTER));
                        }
                    }

                    if (table != null) {
                        document.add(table);
                        document.setFontSize(10);
                        document.add(new Paragraph("\n"));
                    }
                }
            }

            // Add minimal spacing
            document.add(new Paragraph(" ").setFontSize(8));

            // Create signature table
            Table signatureTable = new Table(2).useAllAvailableWidth();
            signatureTable.setBorder(Border.NO_BORDER);

            // Director's signature - left side
            String directorSignature = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "signature".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
            if (StringUtils.isNotBlank(directorSignature)) {
                try {
                    Image directorSignatureImg = new Image(ImageDataFactory.create(new URL(directorSignature)))
                            .scaleToFit(150, 80); // Original size
                    leftCell.add(directorSignatureImg);
                } catch (Exception e) {
                    // Handle gracefully
                }
            }
            leftCell.add(new Paragraph("Director").setBold().setFontSize(10));

            // Right side - "Powered by" and PlayMo logo on same line
            Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM).setPadding(5);

            // Create a table within the cell to align "Powered by" and logo horizontally
            Table poweredByTable = new Table(2);
            poweredByTable.setBorder(Border.NO_BORDER);
            poweredByTable.setWidth(UnitValue.createPercentValue(100));

            // "Powered by" text cell
            Cell poweredByCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(0);
            poweredByCell.add(new Paragraph("Powered by ").setBold().setFontSize(10).setMargin(0));

            // PlayMo logo cell
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(0);

            try {
                Image playmoImg = new Image(ImageDataFactory.create(new URL(PLAYMO_LOGO_S3_URL))).scaleToFit(150, 100); // Original
                // size
                logoCell.add(playmoImg);
            } catch (Exception e) {
                logoCell.add(new Paragraph("PlayMo").setBold().setFontSize(12));
            }

            poweredByTable.addCell(poweredByCell);
            poweredByTable.addCell(logoCell);
            rightCell.add(poweredByTable);

            signatureTable.addCell(leftCell);
            signatureTable.addCell(rightCell);
            document.add(signatureTable);

            document.close(); // Close the document

            document.close(); // Close the document
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to generate Performance Report.");
        }
    }

    // @Override
    public byte[] generatePerformanceReportPdfOverlapping(String performanceReportId) throws ResourceException {
        TraineePerformanceReport traineePerformanceReport = traineePerformanceRepo.findById(performanceReportId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Trainee performance report not found. ID: " + performanceReportId));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(traineePerformanceReport.getReportJson());

            String sportName = traineePerformanceReport.getSport().name();
            String academyName = traineePerformanceReport.getAcademy().getName();
            String playerName = traineePerformanceReport.getTraineeUserProfile().getDisplayName();
            String coachName = traineePerformanceReport.getCoachUserProfile().getDisplayName();
            String courseName = traineePerformanceReport.getCourse() != null
                    ? traineePerformanceReport.getCourse().getTitle()
                    : null;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(byteArrayOutputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Academy logo
            String academyLogo = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "academyLogo".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(academyLogo)) {
                ImageData topLogoData = ImageDataFactory.create(new URL(academyLogo));
                Image topLogo = new Image(topLogoData).scaleToFit(450, 150).setMarginTop(-50)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(topLogo);
            }

            document.add(new Paragraph(sportName + " Player Performance Report").setFontSize(14).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            Table infoTable = new Table(2).useAllAvailableWidth();
            infoTable.setFontSize(10);
            infoTable.addCell(new Cell().add(new Paragraph("Academy: " + academyName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Player: " + playerName)).setBorder(Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("Coach: " + coachName)).setBorder(Border.NO_BORDER));
            if (StringUtils.isNotBlank(courseName)) {
                infoTable.addCell(
                        new Cell(1, 2).add(new Paragraph("Course: " + courseName)).setBorder(Border.NO_BORDER));
            }
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Attribute categories
            JsonNode categories = rootNode.get(sportName);
            if (categories != null && categories.isArray()) {
                for (JsonNode categoryNode : categories) {
                    String category = categoryNode.get("category").asText();
                    document.add(new Paragraph(category).setBold().setFontSize(14)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                    Table table = null;
                    JsonNode attributes = categoryNode.get("attributes");
                    if (attributes != null && attributes.isArray()) {
                        for (JsonNode attribute : attributes) {
                            String name = attribute.get("name").asText();
                            String type = attribute.get("type").asText();
                            String value = attribute.get("value").asText();

                            if (table == null) {
                                table = new Table(2).useAllAvailableWidth();
                                table.addCell(new Cell().add(new Paragraph("Attribute")).setBold().setFontSize(10));
                                String label = switch (type) {
                                    case "RATING_1_5" -> "Rating (1-5)";
                                    case "DROPDOWN" -> "Selection";
                                    case "TEXT" -> {
                                        if (sportName.equals(Sports.CALISTHENICS.name())) {
                                            yield "Number of reps/time";
                                        } else {
                                            yield "Notes/Time";
                                        }
                                    }
                                    // case "TEXT" -> "Notes/Time";
                                    default -> "Value";
                                };
                                table.addCell(new Cell().add(new Paragraph(label)).setFontSize(10).setBold());
                            }

                            boolean isZeroOrEmpty = !org.springframework.util.StringUtils.hasText(value)
                                    || "0".equals(value);
                            table.addCell(new Cell().add(new Paragraph(name).setFontSize(10)));

                            table.addCell(new Cell().add(new Paragraph(isZeroOrEmpty ? "N/A" : value)).setFontSize(10)
                                    .setTextAlignment(TextAlignment.CENTER));
                        }
                    }

                    if (table != null) {
                        document.add(table);
                        document.setFontSize(10);
                        document.add(new Paragraph("\n"));
                    }
                }
            }

            int lastPageNum = pdf.getNumberOfPages();
            PdfPage lastPage = pdf.getPage(lastPageNum);
            PdfCanvas canvas = new PdfCanvas(lastPage);
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            float directorTextX = 50;
            float directorTextY = 150;

            // Directors's signature
            String directorSignature = Optional.ofNullable(traineePerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "signature".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(directorSignature)) {
                Image directorSignatureImg = new Image(ImageDataFactory.create(new URL(directorSignature)))
                        .scaleToFit(150, 80).setFixedPosition(directorTextX - 15, directorTextY + 5); // 5 units above
                // text
                document.add(directorSignatureImg);

                // Add "Director" bottom-left
                canvas.beginText().setFontAndSize(font, 10).moveText(directorTextX, directorTextY).showText("Director")
                        .endText();

                canvas.release();
            }

            // Add PlayMo logo
            Image playmoImg = new Image(ImageDataFactory.create(new URL(PLAYMO_LOGO_S3_URL))).scaleToFit(150, 100)
                    .setFixedPosition(PageSize.A4.getWidth() - 170, 100);
            document.add(playmoImg);

            // Add "Powered by" text above logo on bottom-right (moved up from 35 to 65)
            Paragraph poweredByText = new Paragraph("Powered by").setFont(font).setFontSize(10)
                    .setFixedPosition(PageSize.A4.getWidth() - 240, 140, 80); // moved up from 35 to 65
            document.add(poweredByText);

            document.close(); // Close the document
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to generate Performance Report.");
        }
    }

    @Override
    public byte[] generateCoachPerformanceReportPdf(String performanceReportId) throws ResourceException {
        CoachPerformanceReport coachPerformanceReport = coachPerformanceRepo.findById(performanceReportId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Coach performance report not found. ID: " + performanceReportId));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(coachPerformanceReport.getReportJson());

            // Extract Metadata
            String sportName = coachPerformanceReport.getSport() != null ? coachPerformanceReport.getSport().name()
                    : "";
            String academyName = coachPerformanceReport.getAcademy().getName();
            String playerName = coachPerformanceReport.getTraineeUserProfile().getDisplayName();
            String coachName = coachPerformanceReport.getCoachUserProfile().getDisplayName();
            String courseName = coachPerformanceReport.getCourse() != null
                    ? coachPerformanceReport.getCourse().getTitle()
                    : null;

            // Generate PDF with proper page handling
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(byteArrayOutputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Academy logo
            String academyLogo = Optional.ofNullable(coachPerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "academyLogo".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(academyLogo)) {
                ImageData topLogoData = ImageDataFactory.create(new URL(academyLogo));
                Image topLogo = new Image(topLogoData).scaleToFit(450, 150).setMarginTop(-50)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(topLogo);
            }

            // Set margins
            document.setMargins(20, 20, 20, 20);

            // Create document header
            createDocumentHeader(document, sportName, academyName);

            // Add participant information
            createParticipantInfoSection(document, playerName, coachName, courseName);

            // Determine JSON structure and process accordingly
            JsonNode categories = determineDataStructure(rootNode, sportName);

            if (categories != null) {
                processCategories(document, categories);
            } else {
                document.add(new Paragraph("No valid report data found."));
            }

            int lastPageNum = pdf.getNumberOfPages();
            PdfPage lastPage = pdf.getPage(lastPageNum);
            PdfCanvas canvas = new PdfCanvas(lastPage);
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            float directorTextX = 50;
            float directorTextY = 150;

            // Directors's signature
            String directorSignature = Optional.ofNullable(coachPerformanceReport).map(tp -> tp.getAcademy())
                    .map(Academy::getOrg).map(org -> org.getConfigs())
                    .map(configs -> configs.stream().filter(each -> "signature".equals(each.getKey()))
                            .map(each -> each.getValue()).findFirst().orElse(null))
                    .orElse(null);

            if (StringUtils.isNotBlank(directorSignature)) {
                Image directorSignatureImg = new Image(ImageDataFactory.create(new URL(directorSignature)))
                        .scaleToFit(150, 80).setFixedPosition(directorTextX - 15, directorTextY + 5); // 5 units above
                // text
                document.add(directorSignatureImg);

                // Add "Director" bottom-left
                canvas.beginText().setFontAndSize(font, 10).moveText(directorTextX, directorTextY).showText("Director")
                        .endText();

                canvas.release();
            }

            // Add PlayMo logo
            Image playmoImg = new Image(ImageDataFactory.create(new URL(PLAYMO_LOGO_S3_URL))).scaleToFit(150, 100)
                    .setFixedPosition(PageSize.A4.getWidth() - 170, 100);
            document.add(playmoImg);

            // Add "Powered by" text above logo on bottom-right (moved up from 35 to 65)
            Paragraph poweredByText = new Paragraph("Powered by").setFont(font).setFontSize(10)
                    .setFixedPosition(PageSize.A4.getWidth() - 240, 140, 80); // moved up from 35 to 65
            document.add(poweredByText);

            // Add footer
            // addDocumentFooter(document);

            document.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to generate Performance Report.");
        }
    }

    /**
     * Creates document header with formatted sport name
     */
    private void createDocumentHeader(Document document, String sportName, String academyName) {
        // Main title with formatted sport name
        String mainTitle = "Program Feedback Report";

        Paragraph titleParagraph = new Paragraph(mainTitle).setFontSize(18).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5);
        document.add(titleParagraph);

        // Academy name
        Paragraph academyParagraph = new Paragraph(academyName).setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(academyParagraph);
    }

    /**
     * Creates participant information section with adaptive layout
     */
    private void createParticipantInfoSection(Document document, String playerName, String coachName,
            String courseName) {
        // Determine if names are long and need single column layout
        boolean useSingleColumn = shouldUseSingleColumnLayout(playerName, coachName, courseName);

        if (useSingleColumn) {
            createSingleColumnInfoLayout(document, playerName, coachName, courseName);
        } else {
            createTwoColumnInfoLayout(document, playerName, coachName, courseName);
        }

        document.add(new Paragraph("\n"));
    }

    /**
     * Determines if single column layout should be used based on content length
     */
    private boolean shouldUseSingleColumnLayout(String playerName, String coachName, String courseName) {
        // Use single column if any name is longer than 25 characters
        return playerName.length() > 25 || coachName.length() > 25 || (courseName != null && courseName.length() > 25);
    }

    /**
     * Creates single column layout for longer names
     */
    private void createSingleColumnInfoLayout(Document document, String playerName, String coachName,
            String courseName) {
        Table infoTable = new Table(1).useAllAvailableWidth();
        infoTable.setBorder(Border.NO_BORDER);

        infoTable.addCell(createInfoCell("Player: " + playerName));
        infoTable.addCell(createInfoCell("Coach: " + coachName));

        if (courseName != null && !courseName.isEmpty()) {
            infoTable.addCell(createInfoCell("Course: " + courseName));
        }

        document.add(infoTable);
    }

    /**
     * Creates two column layout for shorter names
     */
    private void createTwoColumnInfoLayout(Document document, String playerName, String coachName, String courseName) {
        Table infoTable = new Table(2).useAllAvailableWidth();
        infoTable.setBorder(Border.NO_BORDER);

        infoTable.addCell(createInfoCell("Player: " + playerName));
        infoTable.addCell(createInfoCell("Coach: " + coachName));

        if (courseName != null && !courseName.isEmpty()) {
            infoTable.addCell(new Cell(1, 2).add(new Paragraph("Course: " + courseName)).setBorder(Border.NO_BORDER)
                    .setPadding(3));
        }

        document.add(infoTable);
    }

    /**
     * Creates info cell without truncation
     */
    private Cell createInfoCell(String content) {
        return new Cell().add(new Paragraph(content)).setBorder(Border.NO_BORDER).setPadding(3);
    }

    /**
     * Determines the data structure type and returns the categories node
     */
    private JsonNode determineDataStructure(JsonNode rootNode, String sportName) {
        // Check if it's the nested structure (Format 2: with sport name as key)
        if (org.springframework.util.StringUtils.hasText(sportName)) {
            // Try exact match first
            JsonNode sportCategories = rootNode.get(sportName);
            if (sportCategories != null && sportCategories.isArray()) {
                return sportCategories;
            }

            // Try case-insensitive match
            Iterator<String> fieldNames = rootNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.equalsIgnoreCase(sportName)) {
                    JsonNode fieldValue = rootNode.get(fieldName);
                    if (fieldValue != null && fieldValue.isArray()) {
                        return fieldValue;
                    }
                }
            }
        }

        // Check if it's the flat array structure (Format 1: direct array)
        if (rootNode.isArray()) {
            return rootNode;
        }

        // Try to find any sport key in the root node if sportName didn't work
        if (rootNode.isObject()) {
            // Get the first field that contains an array (likely the sport data)
            Iterator<String> fieldNames = rootNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = rootNode.get(fieldName);
                if (fieldValue != null && fieldValue.isArray() && fieldValue.size() > 0) {
                    // Check if the array contains objects with "category" field
                    JsonNode firstElement = fieldValue.get(0);
                    if (firstElement.isObject() && firstElement.has("category")) {
                        return fieldValue;
                    }
                }
            }

            // If rootNode is an object but not with sport key, check if it has category
            // field
            if (rootNode.has("category")) {
                return rootNode;
            }
        }

        return null;
    }

    /**
     * Processes categories with proper page break handling
     */
    private void processCategories(Document document, JsonNode categories) {
        if (categories.isArray()) {
            for (JsonNode categoryNode : categories) {
                processSingleCategory(document, categoryNode);
            }
        } else if (categories.isObject() && categories.has("category")) {
            processSingleCategory(document, categories);
        }
    }

    /**
     * Processes a single category with page break awareness
     */
    private void processSingleCategory(Document document, JsonNode categoryNode) {
        try {
            String category = categoryNode.get("category") != null ? categoryNode.get("category").asText()
                    : "Unknown Category";

            // Format category name
            String formattedCategory = formatCategoryName(category);

            // Add category rating if available
            String categoryRating = extractCategoryRating(categoryNode);

            // Create category header
            Paragraph categoryHeader = new Paragraph(formattedCategory + categoryRating).setBold().setFontSize(14)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(5).setMarginTop(15).setMarginBottom(5);

            // Keep category header with its table
            categoryHeader.setKeepWithNext(true);
            document.add(categoryHeader);

            // Process attributes
            JsonNode attributes = categoryNode.get("attributes");
            if (attributes != null && attributes.isArray()) {
                processAttributesWithPageBreakHandling(document, attributes);
            } else {
                document.add(new Paragraph("No attributes found for this category."));
                document.add(new Paragraph("\n"));
            }

        } catch (Exception e) {
            document.add(new Paragraph("Error processing category: " + e.getMessage()));
            document.add(new Paragraph("\n"));
        }
    }

    /**
     * Formats category names to be more readable
     */
    private String formatCategoryName(String category) {
        if (category == null || category.isEmpty()) {
            return "Performance Category";
        }

        // Handle common patterns - convert snake_case, kebab-case, and camelCase to
        // readable format
        String formatted = category.replace("_", " ").replace("-", " ").replaceAll("([a-z])([A-Z])", "$1 $2"); // Handle
        // camelCase

        // Capitalize each word
        String[] words = formatted.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Extracts category rating information
     */
    private String extractCategoryRating(JsonNode categoryNode) {
        if (categoryNode.has("categoryRating") && categoryNode.get("categoryRating") != null) {
            String rating = categoryNode.get("categoryRating").asText();
            if (org.springframework.util.StringUtils.hasText(rating) && !"0".equals(rating)) {
                return " (Overall Rating: " + rating + "/5)";
            }
        }
        return "";
    }

    /**
     * Processes attributes with proper page break handling
     */
    private void processAttributesWithPageBreakHandling(Document document, JsonNode attributes) {
        // Determine table structure based on attribute types
        String tableType = determineTableType(attributes);

        // Create table with proper column configuration
        Table table = createTableWithHeaders(tableType);

        // Set table to not break across pages inappropriately
        table.setKeepTogether(false); // Allow table to break

        // Add all attribute rows
        for (JsonNode attribute : attributes) {
            addAttributeRowToTable(table, attribute);
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    /**
     * Determines the most appropriate table type based on attributes
     */
    private String determineTableType(JsonNode attributes) {
        Set<String> types = new HashSet<>();

        for (JsonNode attribute : attributes) {
            String type = attribute.get("type") != null ? attribute.get("type").asText() : "UNKNOWN";
            types.add(type.toUpperCase());
        }

        // Return the most specific type found
        if (types.contains("RATING_1_5"))
            return "RATING_1_5";
        if (types.contains("BOOLEAN"))
            return "BOOLEAN";
        if (types.contains("DROPDOWN"))
            return "DROPDOWN";
        if (types.contains("TEXT"))
            return "TEXT";
        return "GENERIC";
    }

    /**
     * Creates table with appropriate headers based on type
     */
    private Table createTableWithHeaders(String tableType) {
        Table table = new Table(2).useAllAvailableWidth();

        // Add headers that won't be separated from content
        Cell headerCell1 = new Cell().add(new Paragraph("Attribute").setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(5);

        Cell headerCell2;
        switch (tableType) {
            case "RATING_1_5":
                headerCell2 = new Cell().add(new Paragraph("Rating (1-5)").setBold());
                break;
            case "BOOLEAN":
                headerCell2 = new Cell().add(new Paragraph("Yes/No").setBold());
                break;
            case "DROPDOWN":
                headerCell2 = new Cell().add(new Paragraph("Selection").setBold());
                break;
            case "TEXT":
                headerCell2 = new Cell().add(new Paragraph("Notes").setBold());
                break;
            default:
                headerCell2 = new Cell().add(new Paragraph("Value").setBold());
                break;
        }

        headerCell2.setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(5);

        // Ensure headers stay with at least one data row
        headerCell1.setKeepWithNext(true);
        headerCell2.setKeepWithNext(true);

        table.addHeaderCell(headerCell1);
        table.addHeaderCell(headerCell2);

        return table;
    }

    /**
     * Adds attribute row to table with full content display
     */
    private void addAttributeRowToTable(Table table, JsonNode attribute) {
        String name = attribute.get("name") != null ? attribute.get("name").asText() : "N/A";
        String type = attribute.get("type") != null ? attribute.get("type").asText() : "UNKNOWN";
        String value = attribute.get("value") != null ? attribute.get("value").asText() : "";

        // Format attribute name for better readability
        String formattedName = formatAttributeName(name);

        // Format value without truncation
        String formattedValue = formatAttributeValueFull(value, type);

        // Create cells
        Cell nameCell = new Cell().add(new Paragraph(formattedName)).setPadding(5);
        Cell valueCell = new Cell().add(new Paragraph(formattedValue)).setPadding(5);

        // Set text alignment based on type
        if ("RATING_1_5".equals(type.toUpperCase()) || "BOOLEAN".equals(type.toUpperCase())) {
            valueCell.setTextAlignment(TextAlignment.CENTER);
        }

        table.addCell(nameCell);
        table.addCell(valueCell);
    }

    /**
     * Formats attribute names for better readability without truncation
     */
    private String formatAttributeName(String name) {
        if (name == null || name.isEmpty())
            return "N/A";

        // Handle common naming patterns
        String formatted = name.replace("_", " ").replace("-", " ").replaceAll("([a-z])([A-Z])", "$1 $2"); // Handle
        // camelCase

        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Formats attribute value without truncation, showing full content
     */
    private String formatAttributeValueFull(String value, String type) {
        // Check if value is empty or zero
        boolean isZeroOrEmpty = !org.springframework.util.StringUtils.hasText(value) || "0".equals(value);

        if (isZeroOrEmpty) {
            return "N/A";
        }

        // Format based on type without truncation
        switch (type.toUpperCase()) {
            case "RATING_1_5":
                try {
                    int rating = Integer.parseInt(value);
                    if (rating >= 1 && rating <= 5) {
                        return value;
                    }
                    return value;
                } catch (NumberFormatException e) {
                    return value;
                }
            case "BOOLEAN":
                return "true".equalsIgnoreCase(value) || "1".equals(value) ? "Yes" : "No";
            case "TEXT":
                // Return full text without truncation
                return value;
            default:
                return value;
        }
    }

    /**
     * Adds document footer with generation timestamp
     */
    private void addDocumentFooter(Document document) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm");
        String timestamp = dateFormat.format(new Date());

        Paragraph footer = new Paragraph("Generated on " + timestamp).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(15);

        document.add(footer);
    }

    // private String parseHtmlContent(String htmlContent, ReportPdfDTO
    // reportPdfDTO, TraineePerformanceReportDto traineePerformanceReportDto,
    // List<String> filesToDelete) {
    // int totalScore = 0;
    // int scoreAchieved = 0;
    // String level = null;
    // String coachNotes = null;
    // String recommendations = null;
    // String psychologicalTable = "";
    // String table = "";
    // int i =2;
    // List<ReportPdfDTO.Category> categories = null;
    // if (reportPdfDTO.getBadminton() != null) {
    // categories = reportPdfDTO.getBadminton();
    // } else if (reportPdfDTO.getCricket() != null) {
    // categories = reportPdfDTO.getCricket();
    // } else {
    // return null;
    // }
    // for(ReportPdfDTO.Category category : categories) {
    // for (ReportPdfDTO.Attribute attribute: category.getAttributes()) {
    // if (attribute.getType().equalsIgnoreCase("RATING_1_5")) {
    // totalScore += 5;
    // scoreAchieved += Integer.parseInt(attribute.getValue());
    // }
    // }
    //
    // if (category.getCategory().equalsIgnoreCase("General Notes")) {
    // for (ReportPdfDTO.Attribute attribute: category.getAttributes()) {
    // if(attribute.getName().equalsIgnoreCase("Level")) {
    // level = attribute.getValue();
    // }
    // if(attribute.getName().equalsIgnoreCase("Coach Notes")) {
    // coachNotes = attribute.getValue();
    // }
    // if(attribute.getName().equalsIgnoreCase("Recommendations")) {
    // recommendations = attribute.getValue();
    // }
    // }
    // }
    // else if (category.getCategory().equalsIgnoreCase("Psychological Factor")) {
    // psychologicalTable = HtmlConstants.TABLE_CONTENT;
    // psychologicalTable = psychologicalTable.replace("{{tableHeading}}" , "1
    // Psychological Factor")
    // .replace("{{tableHead}}", HtmlConstants.PSYCHOLOGICAL_TABLE_HEAD);
    // String tableRow = "";
    // int categoryTotalScore = 0;
    // int categoryScoreAchieved = 0;
    // for (ReportPdfDTO.Attribute attribute: category.getAttributes()) {
    // categoryTotalScore += 5;
    // categoryScoreAchieved += Integer.parseInt(attribute.getValue());
    // String tableData = "<td>" + attribute.getName() + "</td> <td>" + "NA" + "
    // </td>";
    // tableRow += HtmlConstants.TABLE_ROW.replace("{{tableData}}",
    // tableData).replace("{{rating}}", getRatingStars(attribute.getValue()));
    // }
    // String donutChartFilePath = "sports-api-core/src/main/resources/template/" +
    // UUID.randomUUID() + ".png";
    // filesToDelete.add(donutChartFilePath);
    // ChartUtils.createAndSaveDonutChart(donutChartFilePath, categoryScoreAchieved,
    // categoryTotalScore);
    // psychologicalTable = psychologicalTable.replace("{{tableBody}}",
    // tableRow).replace("{{donutChart}}", donutChartFilePath);;
    // } else {
    // int categoryTotalScore = 0;
    // int categoryScoreAchieved = 0;
    // String localTable = HtmlConstants.TABLE_CONTENT;
    // localTable = localTable.replace("{{tableHeading}}" , i + " " +
    // category.getCategory())
    // .replace("{{tableHead}}", HtmlConstants.TABLE_HEAD);
    // String tableRow = "";
    // for (ReportPdfDTO.Attribute attribute: category.getAttributes()) {
    // categoryTotalScore += 5;
    // categoryScoreAchieved += Integer.parseInt(attribute.getValue());
    // String tableData = "<td>" + attribute.getName() + "</td> <td>" + "NA" +
    // "</td>" + "<td>" + "NA" + "</td>";
    // tableRow += HtmlConstants.TABLE_ROW.replace("{{tableData}}",
    // tableData).replace("{{rating}}", getRatingStars(attribute.getValue()));
    // }
    // String donutChartFilePath = "sports-api-core/src/main/resources/template/" +
    // UUID.randomUUID() + ".png";
    // filesToDelete.add(donutChartFilePath);
    // ChartUtils.createAndSaveDonutChart(donutChartFilePath, categoryScoreAchieved,
    // categoryTotalScore);
    // localTable = localTable.replace("{{tableBody}}",
    // tableRow).replace("{{donutChart}}", donutChartFilePath);
    // table += localTable;
    // i++;
    // }
    //
    // }
    // return htmlContent.replace("{{traineeName}}",
    // traineePerformanceReportDto.getTrainee().getDisplayName())
    // .replace("{{coachName}}",
    // traineePerformanceReportDto.getCoach().getDisplayName())
    // .replace("{{academyName}}",
    // traineePerformanceReportDto.getAcademy().getName())
    // .replace("{{courseName}}",
    // traineePerformanceReportDto.getCourse().getTitle())
    // .replace("{{sport}}", traineePerformanceReportDto.getSport().name())
    // .replace("{{totalScore}}", String.valueOf(totalScore))
    // .replace("{{scoreAchieved}}", String.valueOf(scoreAchieved))
    // .replace("{{level}}",
    // org.apache.commons.lang3.StringUtils.defaultIfBlank(level, "NA"))
    // .replace("{{coachNotes}}",
    // org.apache.commons.lang3.StringUtils.defaultIfBlank(coachNotes,"NA"))
    // .replace("{{recommendations}}",
    // org.apache.commons.lang3.StringUtils.defaultIfBlank(recommendations,"NA"))
    // .replace("{{date}}", DateTimeUtils.getReportCurrentDate())
    // .replace("{{table}}", psychologicalTable + table);
    // }

    private TraineePerformanceReportDto getTraineePerformanceById(String id) throws ResourceException {
        Optional<TraineePerformanceReport> traineePerformance = traineePerformanceRepo.findById(id);
        if (traineePerformance.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Trainee performance not found. ID: " + id);
        }

        TraineePerformanceReportDto traineePerformanceDto = modelMapper.map(traineePerformance.get(),
                TraineePerformanceReportDto.class);
        if (traineePerformance.get().getCourse() != null) {
            // CourseDto courseDto =
            // courseService.getCourse(traineePerformance.get().getAcademyId(),
            // traineePerformance.get().getCourseId());
            traineePerformanceDto.setCourse(modelMapper.map(traineePerformance.get().getCourse(), CourseMinDto.class));
        }
        // UserProfileDto traineeUserProfile =
        // userProfileService.getUserProfileById(traineePerformance.get().getTraineeUserId());
        // UserProfileDto coachUserProfile =
        // userProfileService.getUserProfileById(traineePerformance.get().getCoachUserId());
        traineePerformanceDto.setReport(traineePerformance.get().getReportJson());
        traineePerformanceDto
                .setTrainee(modelMapper.map(traineePerformance.get().getTraineeUserProfile(), UserProfileMinDto.class));
        traineePerformanceDto
                .setCoach(modelMapper.map(traineePerformance.get().getCoachUserProfile(), UserProfileMinDto.class));
        if (traineePerformance.get().getAcademy() != null) {
            traineePerformanceDto
                    .setAcademy(modelMapper.map(traineePerformance.get().getAcademy(), AcademyMinDto.class));
        }
        if (!CollectionUtils.isEmpty(traineePerformance.get().getMediaMappings())) {
            traineePerformanceDto.setMediaUrls(traineePerformance.get().getMediaMappings().stream()
                    .map(mediaMapping -> performanceMediaBaseUrl.concat(mediaMapping.getMediaUrl()))
                    .collect(Collectors.toList()));
        }

        return traineePerformanceDto;
    }

    private List<TraineeDetailsDto> sort(List<TraineeDetailsDto> traineeDetailsDtos) {
        return traineeDetailsDtos.stream()
                .sorted(Comparator.comparing(traineeDetailsDto -> traineeDetailsDto.getUserProfile().getDisplayName()))
                .toList();
    }

    private List<TraineePaymentDetailsDto> sortTraineePaymentDetailsDto(
            List<TraineePaymentDetailsDto> traineePaymentDetailsDtos) {
        return traineePaymentDetailsDtos.stream().sorted(Comparator.comparing(TraineePaymentDetailsDto::getCourseName))
                .toList();
    }

    private String calculateCategoryRating(String reportString) {
        Map<Sports, List<Category>> report = AppConstants.GSON.fromJson(reportString,
                TypeToken.getParameterized(Map.class, Sports.class,
                        TypeToken.getParameterized(List.class, Category.class).getType()).getType());
        for (Map.Entry<Sports, List<Category>> entry : report.entrySet()) {
            for (Category category : entry.getValue()) {
                List<Attribute> ratingAttributes = category.getAttributes().stream()
                        .filter(attribute -> attribute.getType() == AttributeType.RATING_1_5
                                || attribute.getType() == AttributeType.RATING_10)
                        .toList();
                double averageRating = ratingAttributes.stream().mapToDouble(a -> {
                    try {
                        double value = Double.parseDouble(a.getValue());
                        // Normalize RATING_10 to 1-5 scale
                        if (a.getType() == AttributeType.RATING_10) {
                            return (value - 1) * 4 / 9 + 1; // Maps 1-10 to 1-5
                        }
                        return value;
                    } catch (NumberFormatException e) {
                        log.error("The string does not contain a parsable double");
                        return 0.0;
                    } catch (NullPointerException npe) {
                        log.error("The string is null");
                        return 0.0;
                    }
                }) // Extract the ratings
                        .average() // Calculate the average
                        .orElse(0.0);

                category.setCategoryRating(String.valueOf((int) Math.ceil(averageRating)));
            }
        }
        return AppConstants.GSON.toJson(report);
    }

    public void generatePdfFromHtml(String htmlContent, String outputPdfPath)
            throws IOException, com.lowagie.text.DocumentException {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent.replace("&", "&amp;"));
        renderer.layout();
        try (FileOutputStream fos = new FileOutputStream(outputPdfPath)) {
            renderer.createPDF(fos);
        }
    }

    private String getRatingStars(String rating) {
        return switch (rating) {
            case "1" -> "*";
            case "2" -> "* *";
            case "3" -> "* * *";
            case "4" -> "* * * *";
            case "5" -> "* * * * *";
            default -> "NA";
        };
    }

    private List<TraineePerformanceReportMediaMapping> buildTraineePerfReportMappings(String reportId,
            List<String> mediaUrlPaths) {
        return mediaUrlPaths.stream()
                .map(mediaUrlPath -> TraineePerformanceReportMediaMapping.builder()
                        .report(TraineePerformanceReport.builder().id(reportId).build()).mediaUrl(mediaUrlPath).build())
                .collect(Collectors.toList());
    }

    @Override
    public ServiceResponse getAllTrainess(GenericFilter filter, String domainUrl) {
        List<TraineeView> traineesList = new ArrayList<>();
        Page<TraineeView> pageableContent = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
            UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();

            String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
            filter.setUserRole(userRole);
            filter.setUserId(currentUser.getId());

            filter.setDomainUrl(domainUrl);

            TraineeSpecification traineeViewSpec = new TraineeSpecification(filter, currentUser,
                    coachAcademyMappingRepo, traineeAcademyMappingRepo);

            if (!filter.isExport() && filter.isPageable()) {
                PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
                pageableContent = traineeViewRepo.findAll(traineeViewSpec, pageRequest);
                traineesList = pageableContent.getContent();
            } else {
                traineesList = traineeViewRepo.findAll(traineeViewSpec);
            }

            if (traineesList.isEmpty()) {
                log.info(ApiResponse.NO_RECORD_FOUND.getMessage());
                return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND, HttpStatus.OK);
            }

            if (filter.isExport()) {
                try {
                    List<TraineeViewDao> traineeViewDaos = traineesList.stream().map(PlayerMapper::mapTraineeViewToDao)
                            .collect(Collectors.toList());

                    List<String> columnOrder = Arrays.asList("playerName", "dob", "emailId", "phoneNumber", "gender",
                            "createdOn", "addressLine1", "addressLine2", "pincode", "city", "state", "country",
                            "programNames");

                    byte[] excelBytes = ExcelGenerator.generateExcel(traineeViewDaos, columnOrder);

                    Map<String, Object> responseMap = new HashMap<>();
                    responseMap.put("status", HttpStatus.OK);
                    responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
                    responseMap.put("fileName", "trainee_data.xlsx");
                    responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

                    return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
                } catch (IOException e) {
                    log.error("Error exporting trainee data to Excel", e);
                    return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
                }
            } else {
                if (filter.isPageable()) {
                    return ResponseBuilder.success(traineesList, ApiResponse.FETCHED_LIST, HttpStatus.OK,
                            pageableContent.getTotalPages(), pageableContent.getTotalElements());
                }
                return ResponseBuilder.success(traineesList, ApiResponse.FETCHED_LIST, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Something unexpected occured: {}", e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }

    }
}