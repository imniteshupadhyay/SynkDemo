package com.playmotech.api.core.services.impl;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.VideoAnalysis;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dto.DirectUploadDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.VideoAnalysisDto;
import com.playmotech.api.core.dto.VideoAnalyzerDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.VideoAnalysisMapper;
import com.playmotech.api.core.mapper.VideoAnalyzerMapper;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.repo.VideoAnalysisRepo;
import com.playmotech.api.core.repo.VideoAnalyzerRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.VideoAnalyzerDao;
import com.playmotech.api.core.services.ICoachIncentiveService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.VideoAnalyzerService;
import com.playmotech.api.core.specification.VideoAnalyzerSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.utils.ThumbnailGenerator;
import com.playmotech.api.core.validation.CoachRoleValidator;
import com.playmotech.api.core.validation.VideoAnalyzerValidation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class VideoAnalyzerServiceImpl implements VideoAnalyzerService {
    @Value("${storage.users-media-bucket}")
    private String usersMediaBucket;

    @Value("${users-media-base-url}")
    private String usersMediaBaseUrl;

    @Value("${direct.upload.expiration.time.seconds:3600}")
    private int expirationTimeInSeconds;

    @Value("${default.thumbnail.url}")
    private String defaultThumbnailUrl;

    private final IStorageService iStorageService;
    private final IPushNotificationService pushNotificationService;

    private final UserProfileRepo userProfileRepo;
    private final VideoAnalysisRepo videoAnalysisRepo;
    private final VideoAnalyzerRepo videoAnalyzerRepository;

    private final VideoAnalyzerValidation validationUtil;
    private final UserProfileHelper userProfileHelper;
    private final AcademyDomainUtil academyDomainUtil;
    private final ICoachIncentiveService coachIncentiveService;
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;

    @Override
    public ServiceResponse getVideoAnalyzersList(GenericFilter filter, String domainUrl) {
        try {
            log.info(ApiResponse.START_GET_VIDEO_ANALYZERS_LIST.getMessage());

            // String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

            // Fetch user profile to determine role
            UserProfile user = userProfileRepo.findById(filter.getUserId())
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "User not found"));

            String role;
            if (user.getRole() == Role.SUPER_ADMIN) {
                role = Role.SUPER_ADMIN.name();
            } else if (user.getRole() == Role.USER) {
                // Determine based on userType
                role = (user.getUserType() == UserType.PLAYER) ? UserType.PLAYER.name() : UserType.COACH.name();
            } else {
                role = user.getRole().name();
            }

            filter.setUserRole(role);

            VideoAnalyzerSpecification specification = new VideoAnalyzerSpecification(filter);
            List<VideoAnalyzer> analyzerList;
            Page<VideoAnalyzer> pageableContent = null;

            if (filter.isPageable()) {
                PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
                pageableContent = videoAnalyzerRepository.findAll(specification, page);
                analyzerList = pageableContent.getContent();
            } else {
                analyzerList = videoAnalyzerRepository.findAll(specification);
            }

            if (analyzerList.isEmpty()) {
                log.info(ApiResponse.VIDEO_ANALYZER_NOT_FOUND.getMessage());
                return ResponseBuilder.success(ApiResponse.VIDEO_ANALYZER_NOT_FOUND, HttpStatus.OK);
            }

            List<VideoAnalyzerDao> analyzerListDao = analyzerList.stream().map(VideoAnalyzerMapper::mapEntityToDao)
                    .collect(Collectors.toList());

            if (filter.isPageable()) {
                return ResponseBuilder.success(analyzerListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK,
                        pageableContent.getTotalPages(), pageableContent.getTotalElements());
            }
            return ResponseBuilder.success(analyzerListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK);
        } catch (Exception e) {
            log.error(ApiResponse.ERROR_FETCHING_LIST.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
        }
    }

    @Override
    public ServiceResponse getVideoAnalyzer(String id) {
        try {
            log.info(ApiResponse.START_GET_VIDEO_ANALYZER.getMessage());

            if (!validationUtil.isValidId(id)) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }
            Optional<VideoAnalyzer> analyzerOptional = videoAnalyzerRepository.findByIdAndDeletedIsFalse(id);

            if (analyzerOptional.isEmpty()) {
                log.info(ApiResponse.VIDEO_ANALYZER_NOT_FOUND.getMessage());
                return ResponseBuilder.notFound(ApiResponse.VIDEO_ANALYZER_NOT_FOUND);
            }

            VideoAnalyzerDao dao = VideoAnalyzerMapper.mapEntityToDao(analyzerOptional.get());
            return ResponseBuilder.success(dao, ApiResponse.VIDEO_ANALYZER_FETCHED);

        } catch (Exception e) {
            log.error(ApiResponse.ERROR_VIDEO_ANALYZER_FETCHED.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    @Transactional
    public ServiceResponse addVideoAnalyzer(VideoAnalyzerDto analyzerDto) {
        try {
            log.info(ApiResponse.START_ADD_VIDEO_ANALYZER.getMessage());

            // Validate DTO
            if (!validationUtil.validate(analyzerDto)) {
                return ResponseBuilder.error(ApiResponse.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
            }

            // Check media URL uniqueness
            // if (videoAnalyzerRepository.existsByMediaUrl(analyzerDto.getMediaUrl())) {
            // return ResponseBuilder.error(ApiResponse.DUPLICATE_MEDIA_URL,
            // HttpStatus.CONFLICT);
            // }

            VideoAnalyzer analyzer = VideoAnalyzerMapper.mapDtoToEntity(analyzerDto);

            VideoAnalyzer savedVideoAnalyzer = videoAnalyzerRepository.save(analyzer);
            // TODO: As discussed with Sanjay & Mayur - no points when adding media
            // CompletableFuture.runAsync(() -> {
            // String userId = savedVideoAnalyzer.getCreatedBy().getId();
            // try {
            // rewardCoach(userId, savedVideoAnalyzer, IncentiveSourceType.MEDIA_ADD);
            // } catch (ResourceException e) {
            // log.error("Unable to reward coach: coachId: {}", userId, e);
            // log.error("[VideoAnalyzerServiceImpl] Check the
            // (coach_point_transaction_error) table");
            // }
            // });

            return ResponseBuilder.success(ApiResponse.VIDEO_ANALYZER_ADDED, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error(ApiResponse.EXCEPTION_IN_ADD_VIDEO_ANALYZER.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    @Transactional
    public ServiceResponse updateVideoAnalyzer(VideoAnalyzerDto analyzerDto, String userId) {
        try {
            log.info(ApiResponse.START_UPDATE_VIDEO_ANALYZER.getMessage());

            // Validate the provided analyzer ID
            if (!validationUtil.isValidId(analyzerDto.getId())) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }

            // Fetch the existing VideoAnalyzer entity by ID
            Optional<VideoAnalyzer> analyzerOptional = videoAnalyzerRepository
                    .findByIdAndDeletedIsFalse(analyzerDto.getId());
            if (analyzerOptional.isEmpty()) {
                return ResponseBuilder.error(ApiResponse.VIDEO_ANALYZER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            // Validate the DTO
            if (!validationUtil.validate(analyzerDto)) {
                return ResponseBuilder.error(ApiResponse.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
            }

            // Fetch the existing VideoAnalyzer entity
            VideoAnalyzer existing = analyzerOptional.get();

            // Map the DTO to the existing VideoAnalyzer entity
            VideoAnalyzer updated = VideoAnalyzerMapper.mapDtoToExistingEntity(analyzerDto, existing);

            // Handle updating VideoAnalysis (check for new or existing analysis)
            if (analyzerDto.getAnalysis() != null) {
                Set<String> updatedAnalysisIds = new HashSet<>();
                List<VideoAnalysis> updatedAnalysisList = new ArrayList<>();

                // Loop through the provided analysis items
                for (VideoAnalysisDto analysisDto : analyzerDto.getAnalysis()) {
                    VideoAnalysis existingAnalysis = null;

                    // Check if the analysis already exists by ID
                    if (analysisDto.getId() != null) {
                        existingAnalysis = videoAnalysisRepo.findById(analysisDto.getId()).orElse(null);
                    }

                    if (existingAnalysis != null) {
                        // Update the existing analysis
                        existingAnalysis = VideoAnalysisMapper.mapDtoToExistingEntity(analysisDto, existingAnalysis,
                                updated.getCreatedBy());
                        existingAnalysis.setVideoAnalyzer(updated);
                        updatedAnalysisList.add(existingAnalysis);
                        updatedAnalysisIds.add(existingAnalysis.getId());
                    } else {
                        // Create a new analysis
                        VideoAnalysis newAnalysis = VideoAnalysisMapper.mapDtoToEntity(analysisDto, updated);
                        updatedAnalysisList.add(newAnalysis);
                    }
                }

                // Handle deletion of removed analysis (not included in the provided list)
                List<VideoAnalysis> analysisToDelete = new ArrayList<>();
                for (VideoAnalysis existingAnalysis : existing.getAnalysis()) {
                    if (!updatedAnalysisIds.contains(existingAnalysis.getId())) {
                        // Mark analysis as deleted or inactive (set status as false)
                        // existingAnalysis.setStatus(false); // Assuming you're marking it inactive
                        // instead of deleting
                        analysisToDelete.add(existingAnalysis);
                    }
                }

                // After the iteration, add the modified or deleted analyses
                updatedAnalysisList.addAll(analysisToDelete);

                // Set the updated analysis list to the VideoAnalyzer entity
                updated.setAnalysis(updatedAnalysisList);
                updated.setAnalysed(true);

                Optional<CoachAcademyMapping> coachAcademyMappingOptional = coachAcademyMappingRepo
                        .findByAcademy_IdAndCoachUserProfile_Id(updated.getAcademy().getId(), userId);

                if (coachAcademyMappingOptional.isPresent() &&
                        coachAcademyMappingOptional.get().getRoleId() != null &&
                        CoachRoleValidator.ACCEPTABLE_ROLE_IDS
                                .contains(coachAcademyMappingOptional.get().getRoleId())) {
                    // VideoAnalyzer savedVideoAnalyzer = updated;
                    log.info(
                            "[VideoAnalyzerServiceImpl] The user is indeed a coach in the provided academy!!! Rewarding the coach with some points :)");
                    CompletableFuture.runAsync(() -> {
                        try {
                            rewardCoach(userId, updated, IncentiveSourceType.MEDIA_ANALYSE);
                        } catch (ResourceException e) {
                            log.error("Unable to reward coach: coachId: {}", userId, e);
                            log.error("[VideoAnalyzerServiceImpl] Check the (coach_point_transaction_error) table");
                        }
                    });
                } else {
                    log.warn(
                            "No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
                }
            }

            // Set the user who updated
            updated.setUpdatedBy(VideoAnalyzerMapper.mapIdToUser(userId));

            // Save the updated VideoAnalyzer entity
            videoAnalyzerRepository.save(updated);
            return ResponseBuilder.success(ApiResponse.VIDEO_ANALYZER_UPDATED, HttpStatus.OK);

        } catch (Exception e) {
            log.error(ApiResponse.EXCEPTION_IN_UPDATE_VIDEO_ANALYZER.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    public ServiceResponse fileUpload(String userId, FileObjectDto fileObjectDto,
            FileObjectDto thumbnailFileObjectDto) {
        UUID uuid = UUID.randomUUID();
        String originalFileName = fileObjectDto.getOriginalFilename();
        String bucketFolder = "users-media/";
        String sanitizedFileName = originalFileName.replaceAll("\\s+", "_");
        String mediaPrefix = String.format("%s/%s/%s",
                userId,
                uuid,
                uuid + "_" + sanitizedFileName);
        Map<String, String> responseBody = new HashMap<>();

        // ServiceResponse response =
        // storageService.uploadFileToS3Bucket(usersMediaBucket, "users-media/" +
        // prefix, files,
        // filesToUpload.getFile().getContentType());
        // uploading media
        iStorageService.upload(usersMediaBucket, bucketFolder + mediaPrefix, fileObjectDto.getContent(),
                fileObjectDto.getContentType());
        responseBody.put("mediaUrl", usersMediaBaseUrl.concat(mediaPrefix));

        if (thumbnailFileObjectDto != null) {
            String thumbnailFileName = thumbnailFileObjectDto.getOriginalFilename();
            String thumbnailPrefix = String.format("%s/%s/%s/%s",
                    userId,
                    uuid,
                    "thumbnail",
                    uuid + "_" + thumbnailFileName);
            // uploading thumbnail
            iStorageService.upload(usersMediaBucket, bucketFolder + thumbnailPrefix,
                    thumbnailFileObjectDto.getContent(), thumbnailFileObjectDto.getContentType());

            responseBody.put("thumbnailUrl", usersMediaBaseUrl.concat(thumbnailPrefix));
        } else {
            responseBody.put("thumbnailUrl", null);
        }

        return ResponseBuilder.success(responseBody, ApiResponse.FILE_UPLOADED_SUCCESSFULLY);
    }

    @Override
    @Transactional
    public ServiceResponse deleteVideoAnalyzer(String id, String userId) {
        try {
            log.info(ApiResponse.START_DELETE_VIDEO_ANALYZER.getMessage());

            if (!validationUtil.isValidId(id)) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }

            Optional<VideoAnalyzer> analyzerOptional = videoAnalyzerRepository.findById(id);
            if (analyzerOptional.isEmpty()) {
                return ResponseBuilder.error(ApiResponse.VIDEO_ANALYZER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            VideoAnalyzer analyzer = analyzerOptional.get();
            analyzer.setDeleted(true);
            analyzer.setUpdatedBy(VideoAnalyzerMapper.mapIdToUser(userId));
            videoAnalyzerRepository.save(analyzer);

            return ResponseBuilder.success(ApiResponse.VIDEO_ANALYZER_DELETED, HttpStatus.OK);

        } catch (Exception e) {
            log.error(ApiResponse.EXCEPTION_IN_DELETE_VIDEO_ANALYZER.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    @Transactional
    public ServiceResponse shareVideoAnalyzer(String id, String courseId, String playerId, String userId) {
        try {
            log.info(ApiResponse.START_VIDEO_ANALYSIS.getMessage());

            if (!validationUtil.isValidId(id) || !validationUtil.validateUserId(playerId)
                    || !validationUtil.validateCourse(courseId) || !validationUtil.validateMandatoryUserId(userId)) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }

            Optional<VideoAnalyzer> analyzerOptional = videoAnalyzerRepository.findById(id);
            if (analyzerOptional.isEmpty()) {
                return ResponseBuilder.error(ApiResponse.VIDEO_ANALYZER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            VideoAnalyzer analyzer = analyzerOptional.get();
            VideoAnalyzer savedAnalyzer = null;

            if (analyzer.getPlayer() == null && analyzer.getCourse() == null) {
                analyzer.setCourse(VideoAnalyzerMapper.mapIdToCourse(courseId));
                analyzer.setPlayer(VideoAnalyzerMapper.mapIdToUser(playerId));
                analyzer.setUpdatedBy(VideoAnalyzerMapper.mapIdToUser(userId));
                savedAnalyzer = videoAnalyzerRepository.save(analyzer);
            }

            ServiceResponse profileServiceResponse = userProfileHelper.fetchUserProfileById(playerId);

            if (!profileServiceResponse.getHttpStatus().is2xxSuccessful()) {
                return profileServiceResponse;
            }

            UserProfile userProfile = (UserProfile) profileServiceResponse.getBody();
            // TODO: Need to decide which extra params to send
            Map<String, String> extraParams = new HashMap<>();

            String fcmToken = userProfile.getAndroidFcmPushToken();

            if (!StringUtils.hasText(fcmToken)) {
                log.warn("fcmToken is either empty or null: {}", fcmToken);
            } else {
                try {
                    pushNotificationService.sendMessageToPushToken(fcmToken, NotificationType.LIVE_NOTIFICATION,
                            "Coach Review for You", "A coach has added a review for you.", "COACH_REVIEWS",
                            CtaType.SCREEN, extraParams);
                } catch (Exception e) {
                    log.error("Unable to send push notification to the userId: {}", userProfile.getId());
                    e.printStackTrace();
                }

                try {
                    /**
                     * TODO: Need to update the cta (COACH_REVIEWS) here as currently we don't know
                     * the screen name
                     */
                    pushNotificationService.addNotification(List.of(userProfile.getId()), "You recieved coach review",
                            CtaType.SCREEN, "COACH_REVIEWS", extraParams);
                } catch (Exception e) {
                    log.error("Unable to add notification to the userId: {}", userProfile.getId());
                    e.printStackTrace();
                }
            }

            // Coach incentive on sharing media
            Optional<CoachAcademyMapping> coachAcademyMappingOptional = coachAcademyMappingRepo
                    .findByAcademy_IdAndCoachUserProfile_Id(analyzer.getAcademy().getId(), userId);

            if (coachAcademyMappingOptional.isPresent() &&
                    coachAcademyMappingOptional.get().getRoleId() != null &&
                    CoachRoleValidator.ACCEPTABLE_ROLE_IDS
                            .contains(coachAcademyMappingOptional.get().getRoleId())) {
                // VideoAnalyzer savedVideoAnalyzer = updated;
                log.info(
                        "[VideoAnalyzerServiceImpl] The user is indeed a coach in the provided academy!!!\nRewarding the coach with some points for sharing the analysed video :)");
                CompletableFuture.runAsync(() -> {
                    try {
                        rewardCoach(userId, analyzer, IncentiveSourceType.MEDIA_SHARE);
                    } catch (ResourceException e) {
                        log.error("Unable to reward coach: coachId: {}", userId, e);
                    }
                });
            } else {
                log.warn(
                        "No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
            }

            return ResponseBuilder.success(ApiResponse.VIDEO_ANALYSIS_COMPLETED, HttpStatus.OK);

        } catch (Exception e) {
            log.error(ApiResponse.EXCEPTION_IN_VIDEO_ANALYSIS.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    private void rewardCoach(String coachUserId, VideoAnalyzer videoAnalyzer, IncentiveSourceType sourceType)
            throws ResourceException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("videoAnalyzerId", videoAnalyzer.getId());
        metadata.put("coachUserId", coachUserId);
        metadata.put("academyId", videoAnalyzer.getAcademy() != null ? videoAnalyzer.getAcademy().getId() : null);
        metadata.put("courseId", videoAnalyzer.getCourse() != null ? videoAnalyzer.getCourse().getId() : null);
        metadata.put("playerId", videoAnalyzer.getPlayer() != null ? videoAnalyzer.getPlayer().getId() : null);

        coachIncentiveService.awardPoints(
                coachUserId,
                sourceType,
                videoAnalyzer.getId(),
                videoAnalyzer.getAcademy().getId(),
                metadata);

    }

    @Override
    public ServiceResponse generateDirectUploadUrl(String userId, String fileName) {
        try {
            log.info("Generating direct upload URL for file: {}", fileName);

            // Validate inputs
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(fileName)) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }

            // Verify user
            Optional<UserProfile> user = userProfileRepo.findById(userId);
            if (user.isEmpty()) {
                return ResponseBuilder.error(ApiResponse.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            String uuid = UUID.randomUUID().toString();
            String sanitizedFileName = fileName.replaceAll("\\s+", "_");
            String bucketFolder = "users-media/";
            String fileKey = String.format("%s%s/%s/%s",
                    bucketFolder,
                    userId,
                    uuid,
                    uuid + "_" + sanitizedFileName);

            URL preSignedUploadUrl = iStorageService.generatePreSignedUploadUrl(usersMediaBucket, fileKey, null,
                    expirationTimeInSeconds);

            DirectUploadDto uploadDto = new DirectUploadDto();

            uploadDto.setFileName(fileName);
            uploadDto.setModuleType("VIDEO_ANALYZER");
            uploadDto.setFileKey(fileKey);
            uploadDto.setPresignedUrl(preSignedUploadUrl.toString());
            uploadDto.setModuleId(uuid);

            uploadDto.setExpiresAt(Date.from(Instant.now().plusSeconds(expirationTimeInSeconds)));

            return ResponseBuilder.success(uploadDto, "success", HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating pre-signed URL: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    public ServiceResponse confirmDirectUpload(String fileKey, String id) throws ResourceException {
        if (!StringUtils.hasText(fileKey)) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Missing fileKey");
        }

        if (!StringUtils.hasText(id)) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Missing fileKey");
        }

        if (!iStorageService.doesObjectExist(usersMediaBucket, fileKey)) {
            // return ResponseBuilder.error("File does not exist", HttpStatus.NOT_FOUND);
            throw new ResourceException(ErrorCodes.NOT_FOUND, "File doest not exist in the bucket");
        }
        String bucketFolder = "users-media/";
        String fileName = fileKey.substring(bucketFolder.length());

        // mediaUrl using CDN url
        String mediaUrl = usersMediaBaseUrl.concat(fileName);

        Optional<VideoAnalyzer> videoAnalyzer = videoAnalyzerRepository.findById(id);
        if (!videoAnalyzer.isPresent()) {
            // return ResponseBuilder.error("Object to save not found",
            // HttpStatus.NOT_FOUND);
            log.error("Entity with the provided id {} does not exist in the database", id);
            throw new ResourceException(ErrorCodes.NOT_FOUND, "Entity with the provided id does not exist.");

        }
        videoAnalyzer.get().setMediaUrl(mediaUrl);
        videoAnalyzer.get().setThumbnailUrl(defaultThumbnailUrl);

        videoAnalyzerRepository.save(videoAnalyzer.get());

        String[] parts = fileKey.split("/");
        String fileName2 = parts[3];

        CompletableFuture.runAsync(() -> {
            try {
                generateThumbnail(fileKey, bucketFolder, fileName2, id);
            } catch (Exception e) {
                log.error("Async thumbnail generation failed for fileKey={} id={}, error={}",
                        fileKey, id, e.getMessage(), e);
            }
        });

        ServiceResponse response = new ServiceResponse();
        response.setMessage("success");
        response.setHttpStatus(HttpStatus.OK);
        response.setStatus(HttpStatus.OK.value());
        return response;
    }

    public void generateThumbnail(String fileKey, String bucketFolder, String fileName, String id)
            throws ResourceException {

        String[] parts = fileKey.split("/");
        if (parts.length < 3) {
            log.error("fileKey splitting is less than 3 fileKey: {}", fileKey);
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid file format");
        }

        String userId = parts[1];
        Optional<VideoAnalyzer> videoAnalyzer = videoAnalyzerRepository.findById(id);
        if (!videoAnalyzer.isPresent()) {
            log.error("Entity with the provided id {} does not exist in the database", id);
            throw new ResourceException(ErrorCodes.NOT_FOUND, "Entity with the provided id does not exist.");
        }

        // Download video temporarily for thumbnail generation
        byte[] videoBytes = iStorageService.download(usersMediaBucket, fileKey);
        String thumbnailKey = null;
        String thumbnailUrl = null;

        if (videoBytes == null || videoBytes.length == 0) {
            log.error("Received object from the bucket is null or empty: videoBytes: {}", videoBytes.length);
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid file");
        }

        FileObjectDto thumbnailFileDto = ThumbnailGenerator.generateThumbnailFromBytes(videoBytes, fileName);

        if (thumbnailFileDto != null) {
            String key = String.format("%s%s/%s/thumbnail/%s_thumbnail.jpg",
                    bucketFolder,
                    userId,
                    videoAnalyzer.get().getId(),
                    fileName.replace(".", "_"));
            CompletableFuture.runAsync(() -> {
                iStorageService.upload(
                        usersMediaBucket,
                        key,
                        thumbnailFileDto.getContent(),
                        thumbnailFileDto.getContentType());
            });
            thumbnailKey = key;
        }

        if (thumbnailKey != null) {
            thumbnailUrl = usersMediaBaseUrl + thumbnailKey.substring(bucketFolder.length());
        }

        videoAnalyzer.get().setThumbnailUrl(thumbnailUrl);

        videoAnalyzerRepository.save(videoAnalyzer.get());

    }
}