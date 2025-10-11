package com.playmotech.api.core.services.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.PushNotifConstants;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BadmintonLiveScore;
import com.playmotech.api.core.dao_postgres.BadmintonMatch;
import com.playmotech.api.core.dao_postgres.BadmintonMatchTeamPlayerMapping;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Group;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.Team;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserAuthDetails;
import com.playmotech.api.core.dao_postgres.UserExpertiseMapping;
import com.playmotech.api.core.dao_postgres.UserPreferredSportsMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UserRolesMapping;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.BadmintonUserStatsDto;
import com.playmotech.api.core.dto.CoachAcademyDto;
import com.playmotech.api.core.dto.EnrollTraineeInCourseDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.LoginResponseDto;
import com.playmotech.api.core.dto.PlayerEnrollInCourseDto;
import com.playmotech.api.core.dto.PlayerMatchStatsDto;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.UpdateUserProfileDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dto.UserStatsDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.BadmintonLiveScoreRepository;
import com.playmotech.api.core.repo.BadmintonMatchPlayDetailRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.GroupRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.repo.UserAuthDetailsRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.dao.UserProfileDetails;
import com.playmotech.api.core.security.JwtService;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.AcademyDomainUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@Service
public class UserProfileService implements IUserProfileService {

    private final static String OTP = "{OTP}";
    private final static String PHONE_NUMBER = "{PHONE_NUMBER}";

    private final ModelMapper modelMapper = new ModelMapper();
    private final UserProfileRepo userProfileRepo;
    private final PasswordEncoder passwordEncoder;
    private final IPushNotificationService notificationService;
    private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;

    private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
    private final GroupRepo groupRepo;
    private final IStorageService storageService;
    private final IMailService mailService;

    private final RolesRepo rbacRoleRepo;
    private final AcademyRepo academyRepo;
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;
    private final AcademyDomainUtil academyDomainUtil;
    private final JwtService jwtService;
    private final UserAuthDetailsRepo userAuthDetailsRepo;
    private final BadmintonLiveScoreRepository badmintonLiveScoreRepository;
    private final BadmintonMatchPlayDetailRepo badmintonMatchPlayDetailRepo;
    private final UserProfileHelper validationHelper;

    @Value("${sendOtp-on-registration}")
    private boolean sendOtpOnRegistration;

    @Value("${default.icons.male}")
    private String defaultProfilePictureUrlMale;

    @Value("${default.icons.female}")
    private String defaultProfilePictureUrlFemale;

    @Value("${users-media-base-url}")
    private String usersMediaBaseUrl;

    @Value("${storage.users-media-bucket}")
    private String usersMediaBucket;

    @Value("${2factor.otp.url}")
    private String otpUrl;

    @Value("${otp-bypass}")
    private boolean otpByPass;

    @Value("${otp-bypass-username}")
    private List<String> otpByPassUsernames;

    @Value("${otp-bypass-email}")
    private List<String> otpByPassEmails;

    private final RestTemplate template;

    @Value("${otp-expiry-time}")
    private int otpExpiryTime;

    @Autowired
    public UserProfileService(final UserProfileRepo userProfileRepo, IPushNotificationService notificationService,
            TraineeAcademyMappingRepo traineeAcademyMappingRepo, GroupRepo groupRepo, IStorageService storageService,
            final RestTemplate template, IMailService mailService, RolesRepo rbacRoleRepo, AcademyRepo academyRepo,
            CoachAcademyMappingRepo coachAcademyMappingRepo, AcademyDomainUtil academyDomainUtil, JwtService jwtService,
            UserAuthDetailsRepo userAuthDetailsRepo, UserProfileHelper validationHelper,
            TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo,
            BadmintonLiveScoreRepository badmintonLiveScoreRepository,
            BadmintonMatchPlayDetailRepo badmintonMatchPlayDetailRepo) {
        this.userProfileRepo = userProfileRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.notificationService = notificationService;
        this.traineeCourseEnrollmentRepo = traineeCourseEnrollmentRepo;
        this.traineeAcademyMappingRepo = traineeAcademyMappingRepo;
        this.groupRepo = groupRepo;
        this.storageService = storageService;
        this.validationHelper = validationHelper;
        this.template = template;
        this.mailService = mailService;
        this.rbacRoleRepo = rbacRoleRepo;
        this.academyRepo = academyRepo;
        this.coachAcademyMappingRepo = coachAcademyMappingRepo;
        this.academyDomainUtil = academyDomainUtil;
        this.jwtService = jwtService;
        this.userAuthDetailsRepo = userAuthDetailsRepo;
        this.badmintonLiveScoreRepository = badmintonLiveScoreRepository;
        this.badmintonMatchPlayDetailRepo = badmintonMatchPlayDetailRepo;
    }

    @Override
    public UserProfileDto create(UserProfileDto userProfileDto, Boolean sendOtp) throws ResourceException {
        // Validate user profile constraints using centralized validation
        // This ensures display name uniqueness per phone number and email uniqueness
        // across phone numbers
        try {
            validationHelper.validateUserProfile(userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName(),
                    userProfileDto.getEmailId(), null // No user to exclude since this is a new user
            );
        } catch (ResourceException e) {
            log.warn("User profile validation failed during create: {}", e.getMessage());
            throw e; // Re-throw the validation exception
        }

        List<UserProfile> existingProfiles = userProfileRepo.findByPhoneNumberList(userProfileDto.getPhoneNumber());

        log.info("Existing profiles list size: {}", existingProfiles.size());

        UserAuthDetails authDetails = null;
        // If existing profile exists, then use one of theirs authDetails object and set
        // primaryAccount of new user to false.
        if (!CollectionUtils.isEmpty(existingProfiles)) {
            Optional<UserAuthDetails> optionalAuthDetails = existingProfiles.stream().map(UserProfile::getAuthDetails)
                    .filter(Objects::nonNull).findFirst();

            if (optionalAuthDetails.isPresent()) {
                authDetails = optionalAuthDetails.get();
                userProfileDto.setPrimaryAccount(false);
            }
        }

        // SimpleDateFormat sdfSlash = new SimpleDateFormat("dd/MM/yyyy");
        // if (!StringUtils.isEmpty(userProfileDto.getDob())) {
        // String dob = userProfileDto.getDob();
        // Date date = new Date();
        // try {
        // date = sdfSlash.parse(dob);
        // } catch (ParseException pe1) {
        // log.error("Unable to parse dob using dd/MM/yyyy format while adding: {}",
        // dob);

        // try {
        // // If slash format fails, try with dash format
        // SimpleDateFormat sdfDash = new SimpleDateFormat("yyyy-MM-dd");
        // date = sdfDash.parse(dob);
        // } catch (ParseException pe2) {
        // log.error("Unable to parse dob using yyyy-MM-dd format while adding: {}",
        // dob);
        // }
        // }
        // log.info("dob while adding a user: {}", dob);
        // userProfileDto.setDob(sdfSlash.format(date));
        // }

        UserProfile userProfile = new UserProfile();

        BeanUtils.copyProperties(userProfileDto, userProfile);

        // This check will be after model mapper (in request body primaryAccount won't
        // be present, which then will be set to false)
        if (authDetails == null) {
            log.info("No existing authDetails found for username '{}'. Creating new authDetails.",
                    userProfileDto.getUsername());
            authDetails = new UserAuthDetails();
            userProfile.setPrimaryAccount(true);
        }

        userProfile.setId(UUID.randomUUID().toString());
        if (userProfileDto.getUserType() == null) {
            userProfile.setUserType(UserType.PLAYER);
        }
        userProfile.setRoles(getUserRolesMappings(userProfile.getId(), List.of(Role.USER), List.of()));
        userProfile.setRole(userProfileDto.getRole() == null ? Role.USER : userProfileDto.getRole());
        userProfile.setCreatedOn(Timestamp.from(Instant.now()));
        if (!CollectionUtils.isEmpty(userProfileDto.getPreferredSports())) {
            userProfile.setPreferredSports(getUserPreferredSportsMappings(userProfile.getId(),
                    userProfileDto.getPreferredSports(), List.of()));
        }
        if (!CollectionUtils.isEmpty(userProfileDto.getExpertiseLevel())) {
            userProfile.setExpertiseLevel(
                    getUserExpertiseMappings(userProfile.getId(), userProfileDto.getExpertiseLevel(), List.of()));
        }
        if (StringUtils.isEmpty(userProfileDto.getUsername())) {
            userProfile.setUsername(userProfile.getPhoneNumber());
        }
        if (StringUtils.isEmpty(userProfileDto.getPassword())) {
            userProfileDto.setPassword(
                    generateDefaultPassword(userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName()));
        }

        Roles role = getRbacRole(userProfileDto.getRoleId());

        // TODO: Do I have to set role when a user object is created?
        // if (userProfileDto.getRoleId() != null) {
        // userProfile.setRbacRoles(role);
        // }

        // userProfile.setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(userProfileDto.getPassword())));
        // userProfile.setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

        // authDetails.setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(userProfileDto.getPassword())));
        // authDetails.setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

        if (StringUtils.isEmpty(userProfileDto.getPassword())) {
            userProfileDto.setPassword(
                    generateDefaultPassword(userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName()));
            authDetails.setDefaultPassword(true);
        }

        authDetails.setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(userProfileDto.getPassword())));
        authDetails.setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

        if (userProfileDto.getGender() != null && userProfileDto.getGender() == Gender.MALE) {
            userProfile.setProfilePictureUrl(defaultProfilePictureUrlMale);
        } else if (userProfileDto.getGender() != null && userProfileDto.getGender() == Gender.FEMALE) {
            userProfile.setProfilePictureUrl(defaultProfilePictureUrlFemale);
        }

        UserProfile updatedUserProfile;
        try {
            UserAuthDetails savedUserAuth = userAuthDetailsRepo.save(authDetails);
            userProfile.setAuthDetails(savedUserAuth);
            updatedUserProfile = userProfileRepo.save(userProfile);
            if (sendOtpOnRegistration || sendOtp) {
                sendOtp(userProfileDto.getPhoneNumber());
            }

            // if academies ids are present then add, else skip
            // if role is player, then skip adding academies list
            if (!CollectionUtils.isEmpty(userProfileDto.getAcademyId())
                    && !role.getRoleName().equalsIgnoreCase("PLAYER")) {
                mapUserToAcademy(updatedUserProfile, userProfileDto);
            }
            return toDto(updatedUserProfile);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, duplicateKeyException.getMessage());
        }
    }

    @Override
    public UserProfileDto update(String userId, UpdateUserProfileDto updateUserProfileDto) throws ResourceException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        UserProfileDto userProfileDto = getUserProfileById(userId);

        Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
        if (userProfile.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }

        UserProfile existingProfile = userProfile.get();

        // Validate user profile constraints using centralized validation
        // Only validate fields that are being updated
        try {
            String phoneToValidate = existingProfile.getPhoneNumber(); // Phone number doesn't change in update
            String displayNameToValidate = !StringUtils.isEmpty(updateUserProfileDto.getDisplayName())
                    ? updateUserProfileDto.getDisplayName()
                    : existingProfile.getDisplayName();
            String emailToValidate = !StringUtils.isEmpty(updateUserProfileDto.getEmailId())
                    ? updateUserProfileDto.getEmailId()
                    : existingProfile.getEmailId();

            validationHelper.validateUserProfile(phoneToValidate, displayNameToValidate, emailToValidate, userId // Exclude
            // current
            // user
            // from
            // validation
            );
        } catch (ResourceException e) {
            log.warn("User profile validation failed during update: {}", e.getMessage());
            throw e; // Re-throw the validation exception
        }

        if (updateUserProfileDto.getRole() != null) {
            existingProfile.setRole(updateUserProfileDto.getRole());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getDisplayName())) {
            existingProfile.setDisplayName(updateUserProfileDto.getDisplayName());
        }
        if (updateUserProfileDto.getUserType() != null) {
            existingProfile.setUserType(updateUserProfileDto.getUserType());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getGoogleAdvertisingId())) {
            existingProfile.setGoogleAdvertisingId(updateUserProfileDto.getGoogleAdvertisingId());
        }
        if (!StringUtils.isEmpty(updateUserProfileDto.getEmailId())) {
            existingProfile.setEmailId(updateUserProfileDto.getEmailId());
        }
        if (updateUserProfileDto.getGender() != null) {
            existingProfile.setGender(updateUserProfileDto.getGender());
        }

        if (!CollectionUtils.isEmpty(updateUserProfileDto.getPreferredSports())) {
            existingProfile.setPreferredSports(getUserPreferredSportsMappings(userId,
                    updateUserProfileDto.getPreferredSports(), existingProfile.getPreferredSports()));
        }

        if (!CollectionUtils.isEmpty(updateUserProfileDto.getExpertiseLevel())) {
            existingProfile.setExpertiseLevel(getUserExpertiseMappings(userId, updateUserProfileDto.getExpertiseLevel(),
                    existingProfile.getExpertiseLevel()));
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getAddressLine1())) {
            existingProfile.setAddressLine1(updateUserProfileDto.getAddressLine1());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getAddressLine2())) {
            existingProfile.setAddressLine2(updateUserProfileDto.getAddressLine2());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getCity())) {
            existingProfile.setCity(updateUserProfileDto.getCity());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getState())) {
            existingProfile.setState(updateUserProfileDto.getState());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getCountry())) {
            existingProfile.setCountry(updateUserProfileDto.getCountry());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getPincode())) {
            existingProfile.setPincode(updateUserProfileDto.getPincode());
        }

        if (!StringUtils.isEmpty(updateUserProfileDto.getDob())) {
            // SimpleDateFormat sdfSlash = new SimpleDateFormat("dd/MM/yyyy");
            // String dob = updateUserProfileDto.getDob();
            // Date date = new Date();
            // try {
            // date = sdfSlash.parse(dob);
            // } catch (ParseException pe1) {
            // log.error("Unable to parse dob using dd/MM/yyyy format while editing: {}",
            // dob);
            // try {
            // // If slash format fails, try with dash format
            // SimpleDateFormat sdfDash = new SimpleDateFormat("yyyy-MM-dd");
            // date = sdfDash.parse(dob);
            // } catch (ParseException pe2) {
            // log.error("Unable to parse dob using yyyy-MM-dd format while editing: {}",
            // dob);
            // }
            // }
            // log.info("dob while editing a user: {}", dob);
            // existingProfile.setDob(sdfSlash.format(date));

            existingProfile.setDob(updateUserProfileDto.getDob());
        }

        // no need to update role id now.
        // if (updateUserProfileDto.getRoleId() != null) {
        // // If existing roleId is not matching with received role id, update user with
        // the received role id
        // if
        // (!userProfile.get().getRbacRoles().getId().equals(updateUserProfileDto.getRoleId()))
        // {
        // userProfile.get().setRbacRoles(role);
        // }
        // }

        // !TODO: commenting this out since we're not getting role
        // if (!CollectionUtils.isEmpty(updateUserProfileDto.getAcademyId())
        // && !role.getRoleName().equalsIgnoreCase("PLAYER")) {
        // mapUserToAcademy(userProfile.get(), updateUserProfileDto);
        // }

        if (!StringUtils.isEmpty(updateUserProfileDto.getAndroidFcmPushToken())) {
            log.info("Received Updated Android FCM Push Token: {} for userId {}",
                    updateUserProfileDto.getAndroidFcmPushToken(), userId);
            existingProfile.setAndroidFcmPushToken(updateUserProfileDto.getAndroidFcmPushToken());

            CompletableFuture.runAsync(() -> {
                try {
                    if (StringUtils.isEmpty(updateUserProfileDto.getAndroidFcmPushToken())) {
                        log.warn("Skipping re-subscription to academy for user: {} as FCM token is not available.",
                                userId);
                        return;
                    }
                    List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                            .findByTraineeUserProfile_Id(userId).stream()
                            .filter(traineeAcademyMapping -> traineeAcademyMapping.getStatus() == Status.ACTIVE)
                            .toList();
                    for (TraineeAcademyMapping traineeAcademyMapping : traineeAcademyMappings) {
                        log.info("Subscribing user: {} to academy: {}", userId,
                                traineeAcademyMapping.getAcademy().getId());
                        notificationService.subscribeToTopic(updateUserProfileDto.getAndroidFcmPushToken(),
                                String.format(PushNotifConstants.ACADEMY_SUBSCRIPTION_NAME,
                                        traineeAcademyMapping.getAcademy().getId()));
                    }
                    log.info("Re-subscribed user: {} to all academies.", userId);
                } catch (Exception e) {
                    log.error("Error while subscribing to academy for user: {}", userId, e);
                }
            });

            CompletableFuture.runAsync(() -> {
                try {
                    if (org.apache.commons.lang3.StringUtils.isEmpty(updateUserProfileDto.getAndroidFcmPushToken())) {
                        return;
                    }
                    List<Group> groups = groupRepo.findByInactive(false).stream()
                            .filter(group -> !CollectionUtils.isEmpty(group.getGroupMemberMappings())
                                    && group.getGroupMemberMappings().stream()
                                            .anyMatch(groupMemberMapping -> groupMemberMapping
                                                    .getGroupMemberUserProfile().getId().equalsIgnoreCase(userId)))
                            .toList();
                    for (Group group : groups) {
                        String topicName = String.format(PushNotifConstants.GROUP_SUBSCRIPTION_NAME, group.getId());
                        log.info("Subscribing user: {} to group: {}", userId, group.getId());
                        notificationService.subscribeToTopic(updateUserProfileDto.getAndroidFcmPushToken(), topicName);
                    }
                } catch (Exception e) {
                    log.error("Error while subscribing to group for user: {}", userId, e);
                }
            });
        }

        if (updateUserProfileDto.getExperienceInMonths() != null) {
            existingProfile.setExperienceInMonths(updateUserProfileDto.getExperienceInMonths());
        }

        return toDto(userProfileRepo.save(existingProfile));
    }

    @Override
    public String updateProfilePicture(String userId, FileObjectDto fileObjectDto) throws ResourceException {
        Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
        if (userProfile.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        if (fileObjectDto != null) {
            // Upload path includes folder for organization
            String folder = "users-media/";
            String filePath = userId + "/" + UUID.randomUUID() + "_" + fileObjectDto.getOriginalFilename();
            String fullStoragePath = folder + filePath;

            // Upload to storage with full path
            storageService.upload(usersMediaBucket, fullStoragePath, fileObjectDto.getContent(),
                    fileObjectDto.getContentType());

            // Set URL in profile without exposing the internal folder structure
            userProfile.get().setProfilePictureUrl(usersMediaBaseUrl + filePath);
            userProfileRepo.save(userProfile.get());

            return usersMediaBaseUrl + filePath;

        }
        throw new ResourceException(ErrorCodes.INVALID_REQUEST, "FileObjectDto is null");
    }

    @Override
    public Optional<UserDetail> userDetail(String username) {
        // The repo call will always give one entry (primary account true)
        Optional<UserProfile> userProfile = userProfileRepo.findByUsernameOrEmailIdAndPrimaryAccountIsTrue(username,
                username);
        if (userProfile.isEmpty() || userProfile.get().isInactive()) {
            return Optional.empty();
        }
        log.info("otp by pass: {}", otpByPass);
        log.info("otp by pass usernames: {}, {}", otpByPassUsernames,
                (CollectionUtils.isEmpty(otpByPassUsernames) || otpByPassUsernames.contains(username)));
        return userProfile.map(profile -> new UserDetail(profile.getId(), profile.getUsername(),
                otpByPass && (CollectionUtils.isEmpty(otpByPassUsernames)
                        || otpByPassUsernames.contains(profile.getUsername())) ? passwordEncoder.encode("1234")
                                : profile.getAuthDetails().getOtpHashed()));
    }

    @Override
    public Optional<UserDetail> userById(String id) {
        Optional<UserProfile> userProfile = userProfileRepo.findById(id);
        if (userProfile.isEmpty() || userProfile.get().isInactive()) {
            return Optional.empty();
        }

        return userProfile.map(profile -> new UserDetail(profile.getId(), profile.getUsername(), null));
    }

    @Override
    public UserProfileDto getUserProfileByUsername(String username) throws ResourceException {
        Optional<UserProfile> userProfile = userProfileRepo.findByUsernameAndPrimaryAccountIsTrue(username);
        if (userProfile.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }

        return toDto(userProfile.get());
    }

    @Override
    public List<UserProfileDto> getUserProfileDtoListByUsername(String username) throws ResourceException {
        List<UserProfile> userProfiles = userProfileRepo.findByUsernameContainingIgnoreCase(username);
        if (userProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        return userProfiles.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<UserProfileDetails> getUsersByUsername(String username, String domain) {
        List<UserProfile> userProfiles = userProfileRepo.findByUsernameContainingIgnoreCase(username);

        if (userProfiles.isEmpty()) {
            return Collections.emptyList();
        }

        return userProfiles.stream().map(userProfile -> {
            UserProfileDetails details = toDetails(userProfile);

            // Check user type and apply appropriate mapping
            if (userProfile.getUserType() == UserType.COACH) {
                // Handle coach mappings
                handleCoachMappings(userProfile, details, domain);
            } else if (userProfile.getUserType() == UserType.PLAYER) {
                // Handle player/trainee mappings
                handlePlayerMappings(userProfile, details, domain);
            }

            return details;
        }).collect(Collectors.toList());
    }

    @Override
    public UserStatsDto getUserStats(String userId) throws ResourceException {
        Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
        if (userProfile.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }

        UserStatsDto userStatsDto = new UserStatsDto();
        userStatsDto.setStats(new HashMap<>());

        BadmintonUserStatsDto badmintonUserStatsDto = new BadmintonUserStatsDto();

        PlayerMatchStatsDto playerMatchStatsDto = badmintonMatchPlayDetailRepo.getPlayerMatchStatistics(userId);
        badmintonUserStatsDto.setTotalMatches(
                playerMatchStatsDto.getTotalMatchesPlayed() != null ? playerMatchStatsDto.getTotalMatchesPlayed() : 0l);
        badmintonUserStatsDto
                .setWins(playerMatchStatsDto.getMatchesWon() != null ? playerMatchStatsDto.getMatchesWon() : 0l);
        badmintonUserStatsDto
                .setLosses(playerMatchStatsDto.getMatchesLost() != null ? playerMatchStatsDto.getMatchesLost() : 0l);
        badmintonUserStatsDto
                .setDraws(playerMatchStatsDto.getMatchesTied() != null ? playerMatchStatsDto.getMatchesTied() : 0l);

        List<BadmintonLiveScore> badmintonLiveScore = badmintonLiveScoreRepository.findByPlayerId(userId);
        if (!CollectionUtils.isEmpty(badmintonLiveScore)) {
            Map<String, Long> pointsByShotType = badmintonLiveScore.stream()
                    .filter(score -> StringUtils.isNotEmpty(score.getShotType())) // Filter out null shot types
                    .collect(Collectors.groupingBy(BadmintonLiveScore::getShotType, Collectors.counting()));

            List<BadmintonUserStatsDto.PointsByShotType> pointsByShotTypes = new ArrayList<>();

            for (Map.Entry<String, Long> entry : pointsByShotType.entrySet()) {
                pointsByShotTypes.add(new BadmintonUserStatsDto.PointsByShotType("", entry.getKey(), entry.getValue()));
            }

            badmintonUserStatsDto.setPointsByShotType(pointsByShotTypes);
        }
        userStatsDto.getStats().put(Sports.BADMINTON, badmintonUserStatsDto);

        return userStatsDto;
    }

    @Override
    // This method returns Map<username, stats>
    public Map<String, UserStatsDto> getUsersStats(List<String> userIds) throws ResourceException {
        Map<String, UserStatsDto> result = new HashMap<>();

        for (String userId : userIds) {
            Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
            if (userProfile.isEmpty()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found for ID: " + userId);
            }

            UserStatsDto userStatsDto = new UserStatsDto();
            userStatsDto.setStats(new HashMap<>());

            BadmintonUserStatsDto badmintonUserStatsDto = new BadmintonUserStatsDto();

            PlayerMatchStatsDto playerMatchStatsDto = badmintonMatchPlayDetailRepo.getPlayerMatchStatistics(userId);
            badmintonUserStatsDto.setTotalMatches(
                    playerMatchStatsDto.getTotalMatchesPlayed() != null ? playerMatchStatsDto.getTotalMatchesPlayed()
                            : 0L);
            badmintonUserStatsDto.setWins(
                    playerMatchStatsDto.getMatchesWon() != null ? playerMatchStatsDto.getMatchesWon() : 0L);
            badmintonUserStatsDto.setLosses(
                    playerMatchStatsDto.getMatchesLost() != null ? playerMatchStatsDto.getMatchesLost() : 0L);
            badmintonUserStatsDto.setDraws(
                    playerMatchStatsDto.getMatchesTied() != null ? playerMatchStatsDto.getMatchesTied() : 0L);

            List<BadmintonLiveScore> badmintonLiveScore = badmintonLiveScoreRepository.findByPlayerId(userId);
            if (!CollectionUtils.isEmpty(badmintonLiveScore)) {
                Map<String, Long> pointsByShotType = badmintonLiveScore.stream()
                        .filter(score -> StringUtils.isNotEmpty(score.getShotType()))
                        .collect(Collectors.groupingBy(BadmintonLiveScore::getShotType, Collectors.counting()));

                List<BadmintonUserStatsDto.PointsByShotType> pointsByShotTypes = new ArrayList<>();
                for (Map.Entry<String, Long> entry : pointsByShotType.entrySet()) {
                    pointsByShotTypes
                            .add(new BadmintonUserStatsDto.PointsByShotType("", entry.getKey(), entry.getValue()));
                }

                badmintonUserStatsDto.setPointsByShotType(pointsByShotTypes);
            }

            userStatsDto.getStats().put(Sports.BADMINTON, badmintonUserStatsDto);
            result.put(userProfile.get().getDisplayName(), userStatsDto);
        }

        return result;
    }

    private void handleCoachMappings(UserProfile userProfile, UserProfileDetails details, String domain) {
        // Coach academy mappings
        List<CoachAcademyMapping> academyMappings = coachAcademyMappingRepo
                .findByCoachUserProfile_Id(userProfile.getId());

        // Extract academy IDs
        List<String> academyIds = academyMappings.stream().map(CoachAcademyMapping::getAcademy).filter(Objects::nonNull)
                .map(Academy::getId).collect(Collectors.toList());

        // Map academy -> designation
        Map<String, String> academyDesignationMap = academyMappings.stream()
                .filter(mapping -> mapping.getAcademy() != null && mapping.getDesignation() != null)
                .collect(Collectors.toMap(mapping -> mapping.getAcademy().getId(), CoachAcademyMapping::getDesignation,
                        (v1, v2) -> v1));

        // Expertise map
        Map<Sports, SkillLevel> expertiseMap = (userProfile.getExpertiseLevel() != null) ? userProfile
                .getExpertiseLevel().stream()
                .filter(mapping -> mapping.getSport() != null && mapping.getExpertise() != null).collect(Collectors
                        .toMap(UserExpertiseMapping::getSport, UserExpertiseMapping::getExpertise, (v1, v2) -> v1))
                : Map.of();

        // Set coach-specific data
        details.setAcademyDesignationMap(academyDesignationMap);
        details.setExpertiseLevel(expertiseMap);
        details.setAcademyId(academyIds);

        // Map academyCoaches
        List<CoachAcademyDto> academyCoachDtos = academyMappings.stream()
                .filter(mapping -> mapping.getAcademy() != null).map(mapping -> {
                    CoachAcademyDto dto = new CoachAcademyDto();
                    dto.setAcademyId(mapping.getAcademy().getId());
                    dto.setDesignation(mapping.getDesignation());
                    dto.setStatus(mapping.getStatus() != null ? mapping.getStatus().name() : null);
                    return dto;
                }).collect(Collectors.toList());

        details.setAcademyCoaches(academyCoachDtos);
    }

    private void handlePlayerMappings(UserProfile userProfile, UserProfileDetails details, String domain) {
        // Player/Trainee academy mappings
        List<TraineeAcademyMapping> mappings = traineeAcademyMappingRepo
                .findByTraineeUserProfile_Id(userProfile.getId());

        List<String> academyIds = new ArrayList<>();
        List<PlayerEnrollInCourseDto> enrollments = new ArrayList<>();

        for (TraineeAcademyMapping mapping : mappings) {
            Academy academy = mapping.getAcademy();
            if (academy != null) {
                academyIds.add(academy.getId());

                PlayerEnrollInCourseDto academyDto = new PlayerEnrollInCourseDto();
                academyDto.setId(mapping.getId());
                academyDto.setAcademyId(academy.getId());
                academyDto.setAcademyStatus(mapping.getStatus());

                // Fetch program enrollments for this academy
                List<TraineeCourseEnrollment> programEnrollments = traineeCourseEnrollmentRepo
                        .findByTraineeUserProfile_IdAndAcademy_Id(userProfile.getId(), academy.getId());

                List<EnrollTraineeInCourseDto> programDtos = programEnrollments.stream().map(program -> {
                    EnrollTraineeInCourseDto dto = new EnrollTraineeInCourseDto();
                    dto.setId(program.getId());
                    dto.setProgramId(program.getCourse().getId());
                    dto.setProgramStatus(program.getStatus());
                    dto.setTraineeUserId(userProfile.getId());
                    dto.setAmount(program.getAmount());
                    dto.setJoiningDate(program.getJoiningDate());
                    dto.setDueDate(program.getDueDate());
                    dto.setPaymentSchedule(program.getPaymentSchedule());
                    return dto;
                }).collect(Collectors.toList());

                academyDto.setEnrollInPrograms(programDtos);
                enrollments.add(academyDto);
            }
        }

        // Set player-specific data
        details.setAcademyId(academyIds);
        details.setEnrollInAcademies(enrollments);
    }

    private static UserProfileDetails toDetails(UserProfile userProfile) {
        UserProfileDetails details = new UserProfileDetails();

        details.setId(userProfile.getId());
        details.setUsername(userProfile.getUsername());
        details.setDisplayName(userProfile.getDisplayName());
        details.setDob(userProfile.getDob());
        details.setEmailId(userProfile.getEmailId());
        details.setPhoneNumber(userProfile.getPhoneNumber());
        details.setProfilePictureUrl(userProfile.getProfilePictureUrl());
        details.setGender(userProfile.getGender());
        details.setUserType(userProfile.getUserType());
        details.setExperienceInMonths(userProfile.getExperienceInMonths());
        details.setAddressLine1(userProfile.getAddressLine1());
        details.setAddressLine2(userProfile.getAddressLine2());
        details.setPincode(userProfile.getPincode());
        details.setCity(userProfile.getCity());
        details.setState(userProfile.getState());
        details.setCountry(userProfile.getCountry());
        details.setRole(userProfile.getRole());

        details.setPhoneNumberVerified(userProfile.getAuthDetails().isPhoneNumberVerified());
        details.setEmailIdVerified(userProfile.getAuthDetails().isEmailIdVerified());

        if (userProfile.getUserDocuments() != null) {
            details.setAadharUrl(userProfile.getUserDocuments().getAadharUrl());
            details.setPanUrl(userProfile.getUserDocuments().getPanUrl());
        }

        return details;
    }

    @Override
    public List<UserProfileMinDto> searchUser(String searchTxt) throws ResourceException {
        if (StringUtils.isEmpty(searchTxt) || StringUtils.length(searchTxt) < 3) {
            return List.of();
        }
        List<UserProfile> userProfiles = userProfileRepo.searchUserByNameOrUsername(searchTxt);
        if (CollectionUtils.isEmpty(userProfiles)) {
            return List.of();
        }

        return userProfiles.stream().map(userProfile -> modelMapper.map(userProfile, UserProfileMinDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfileDto> getUserProfileByIds(List<String> userIds) throws ResourceException {
        List<UserProfile> userProfiles = userProfileRepo.findByIdIn(userIds);
        // Sending domainUrl as null
        return toDto(userProfiles, true, null);
    }

    @Override
    public UserProfileDto getUserProfileById(String userId) throws ResourceException {
        UserProfile userProfile = userProfileRepo.findById(userId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found"));
        return toDto(userProfile);
    }

    @Override
    public List<UserProfileDto> getByUserType(UserType userType, String searchTxt) throws ResourceException {
        if (StringUtils.isEmpty(searchTxt) || StringUtils.length(searchTxt) < 3) {
            // Sending domainUrl as null
            return toDto(userProfileRepo.findByUserType(userType), true, null);
        }
        List<UserProfile> userProfiles = userProfileRepo.searchUserByNameOrUsernameAndUserType(searchTxt, userType);
        if (CollectionUtils.isEmpty(userProfiles)) {
            return new ArrayList<>();
        }
        // Sending domainUrl as null
        return toDto(userProfiles, true, null);
    }

    @Override
    public void exists(String username) throws ResourceException {
        // Optional<UserProfile> userProfile = userProfileRepo.findByUsername(username);
        // Optional<UserProfile> userProfile = userProfileRepo.findByUsername(username)
        // .or(() -> userProfileRepo.findByEmailId(username));
        Optional<UserProfile> userProfile = userProfileRepo.findByUsernameOrEmailId(username, username);
        if (userProfile.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
    }

    @Override
    public UserProfile getByUsernameOrEmail(String username) throws ResourceException {
        return userProfileRepo.findByUsernameOrEmailId(username, username)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found"));
    }

    // Service Layer
    @Override
    public UserProfile getByIdentifier(String identifier) throws ResourceException {
        return userProfileRepo
                .findByUsernameOrEmailIdOrPhoneNumberAndPrimaryAccountIsTrue(identifier, identifier, identifier)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found"));
    }

    @Override
    public List<UserProfileDto> getUserProfileByIdsAndNameAndPhoneNumber(List<String> userIds, String name,
            String phoneNumber, String searchTxt) throws ResourceException {
        List<UserProfile> userProfiles;
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(phoneNumber)) {
            userProfiles = userProfileRepo.findByIdIn(userIds);
        } else if (StringUtils.isEmpty(name)) {
            userProfiles = userProfileRepo.findByIdInAndPhoneNumber(userIds, phoneNumber);
        } else if (StringUtils.isEmpty(phoneNumber)) {
            userProfiles = userProfileRepo.findByIdInAndUsername(userIds, name);
        } else {
            userProfiles = userProfileRepo.findByIdInAndUsernameAndPhoneNumber(userIds, name, phoneNumber);
        }

        if (StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2) {
            userProfiles = userProfiles.stream()
                    .filter(userProfile -> userProfile.getDisplayName().toLowerCase().contains(searchTxt.toLowerCase())
                            || userProfile.getUsername().toLowerCase().contains(searchTxt.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Sending domainUrl as null
        return toDto(userProfiles, true, null);
    }

    @Override
    public String sendOtp(String phoneNumber) throws ResourceException {
        // Optional<UserProfile> userProfile =
        // userProfileRepo.findByPhoneNumber(phoneNumber);
        Optional<UserProfile> userProfile = userProfileRepo.findByPhoneNumberAndPrimaryAccountIsTrue(phoneNumber);
        if (userProfile.isEmpty()) {
            // Check for user by email instead
            // userProfile = userProfileRepo.findByEmailId(phoneNumber);
            userProfile = userProfileRepo.findByEmailIdAndPrimaryAccountIsTrue(phoneNumber);
            if (userProfile.isEmpty()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
            }
            return sendEmailOtp(userProfile.get());
        }

        boolean byPass = otpByPass || otpByPassUsernames.contains(phoneNumber) || otpByPassEmails.contains(phoneNumber);

        String otp = byPass ? "1234" : String.valueOf(new Random().nextInt(9000) + 1000);
        String otpMessage = otpUrl.replace(OTP, otp).replace(PHONE_NUMBER, phoneNumber);
        String otpHashed = passwordEncoder.encode(otp);
        log.info("sending otp: {}", otp);

        userProfile.get().getAuthDetails().setOtpHashed(otpHashed);
        userProfile.get().getAuthDetails().setOtpExpiryTime(generateOtpExpiryTime());
        userProfile.get().getAuthDetails().setOtpUsed(false);
        if (byPass) {
            log.warn("Bypassing sending OTP as otpByPass is: {}", otpByPass);
            log.info("OTP sent successfully to phone number: otp: {}", otp);
            userProfileRepo.save(userProfile.get());
        } else {
            ResponseEntity<String> response = template.getForEntity(otpMessage, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP sent successfully to phone number: {}", phoneNumber);
                userProfileRepo.save(userProfile.get());
            } else {
                throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to send OTP.");
            }
        }
        return otp;
    }

    private String sendEmailOtp(UserProfile userProfile) throws ResourceException {
        String otp = otpByPass ? "1234" : String.valueOf(new Random().nextInt(9000) + 1000);
        String encodedOtp = passwordEncoder.encode(otp);
        log.info("encodedOtp: {}", encodedOtp);
        userProfile.getAuthDetails().setOtpHashed(encodedOtp);
        userProfile.getAuthDetails().setOtpUsed(false);
        userProfile.getAuthDetails().setOtpExpiryTime(generateOtpExpiryTime());
        log.info("Sending OTP via email: {}", otp);

        try {
            // Send OTP via email
            if (mailService.sendOtpMail(userProfile, otp)) {
                userProfileRepo.save(userProfile);
                log.info("OTP sent successfully to the email: {} ", userProfile.getEmailId());
            }
        } catch (Exception e) {
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to send OTP.");
        }
        return otp;
    }

    // @Transactional
    // @Override
    // public void deleteUser(String userId) throws ResourceException {
    // Optional<UserProfile> userProfile = userProfileRepo.findById(userId);
    // if (userProfile.isEmpty()) {
    // throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
    // }
    // userProfile.get().setInactive(true);
    // userProfileRepo.save(userProfile.get());
    // }

    @Transactional
    @Override
    public void deleteUser(String userId) throws ResourceException {
        // Find the user to be deleted
        UserProfile userToDelete = userProfileRepo.findById(userId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found"));

        // Get the auth details to check for multiple accounts
        UserAuthDetails authDetails = userToDelete.getAuthDetails();

        if (authDetails != null) {
            // Count active profiles
            long activeProfileCount = authDetails.getUserProfiles().stream().filter(profile -> !profile.isInactive())
                    .count();

            // If trying to delete primary account with multiple active profiles, throw
            // error
            if (activeProfileCount > 1 && userToDelete.isPrimaryAccount()) {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                        "Cannot delete primary account. Please switch to another account as primary before deletion.");
            }
        }

        // Mark the user as inactive (soft delete)
        userToDelete.setInactive(true);
        userProfileRepo.save(userToDelete);

        log.info("User [{}] marked as inactive.", userId);
    }

    private List<UserRolesMapping> getUserRolesMappings(String userId, List<Role> roles,
            List<UserRolesMapping> existingRoles) {
        Map<Role, UserRolesMapping> existingRolesMap = CollectionUtils.isEmpty(existingRoles) ? new HashMap<>()
                : existingRoles.stream()
                        .collect(Collectors.toMap(userRolesMapping -> Role.valueOf(userRolesMapping.getRole()),
                                userRolesMapping -> userRolesMapping));

        return roles.stream().map(role -> {
            if (existingRolesMap.containsKey(role)) {
                return existingRolesMap.get(role);
            }
            UserRolesMapping userRolesMapping = new UserRolesMapping();
            userRolesMapping.setUserProfile(UserProfile.builder().id(userId).build());
            userRolesMapping.setRole(role.name());
            return userRolesMapping;
        }).collect(Collectors.toList());
    }

    private List<UserPreferredSportsMapping> getUserPreferredSportsMappings(String userId, Set<Sports> sports,
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
            userPreferredSportsMapping.setUserProfile(UserProfile.builder().id(userId).build());
            userPreferredSportsMapping.setSport(sport);
            return userPreferredSportsMapping;
        }).collect(Collectors.toList());
    }

    private List<UserExpertiseMapping> getUserExpertiseMappings(String userId, Map<Sports, SkillLevel> skillLevels,
            List<UserExpertiseMapping> expertiseMappingList) {
        Map<Sports, UserExpertiseMapping> existingSkillLevels = CollectionUtils.isEmpty(expertiseMappingList)
                ? new HashMap<>()
                : expertiseMappingList.stream().collect(
                        Collectors.toMap(UserExpertiseMapping::getSport, userExpertiseMapping -> userExpertiseMapping));
        return skillLevels.entrySet().stream().map(entry -> {
            if (existingSkillLevels.containsKey(entry.getKey())) {
                UserExpertiseMapping userExpertiseMapping = existingSkillLevels.get(entry.getKey());
                userExpertiseMapping.setExpertise(entry.getValue());
                return userExpertiseMapping;
            }
            UserExpertiseMapping userExpertiseMapping = new UserExpertiseMapping();
            userExpertiseMapping.setUserProfile(UserProfile.builder().id(userId).build());
            userExpertiseMapping.setSport(entry.getKey());
            userExpertiseMapping.setExpertise(entry.getValue());
            return userExpertiseMapping;
        }).collect(Collectors.toList());
    }

    private List<UserProfileDto> toDto(List<UserProfile> userProfiles, boolean sort, String domainUrl) {
        if (CollectionUtils.isEmpty(userProfiles)) {
            return new ArrayList<>();
        }

        Stream<UserProfileDto> stream = userProfiles.stream().map(userProfile -> {
            UserProfileDto userProfileDto = modelMapper.map(userProfile, UserProfileDto.class);
            if (userProfile.getRole() != null
                    && Arrays.asList(Role.USER, Role.PLAYER, Role.COACH).contains(userProfile.getRole())) {
                userProfileDto.setVisibilityConfig(AppConstants.VISIBILITY_CONFIGS);
            } else if (userProfile.getRole() != null
                    && Arrays.asList(Role.USER, Role.PLAYER).contains(userProfile.getRole())) {
                userProfileDto.setVisibilityConfig(AppConstants.VISIBILITY_CONFIGS_CASH_PAYMENT);
            }

            List<UserPreferredSportsMapping> userPreferredSportsMappings = userProfile.getPreferredSports();
            if (!CollectionUtils.isEmpty(userPreferredSportsMappings)) {
                userProfileDto.setPreferredSports(userPreferredSportsMappings.stream()
                        .map(UserPreferredSportsMapping::getSport).collect(Collectors.toSet()));
            }

            List<UserExpertiseMapping> userExpertiseMappings = userProfile.getExpertiseLevel();
            if (!CollectionUtils.isEmpty(userExpertiseMappings)) {
                userProfileDto.setExpertiseLevel(userExpertiseMappings.stream()
                        .collect(Collectors.toMap(UserExpertiseMapping::getSport, UserExpertiseMapping::getExpertise)));
            }

            // where & how to get the role if we've moved the role to CoachAcademyMapping
            // entity? Need academy id here
            // if (userProfile.getRbacRoles() != null) {
            // userProfileDto.setRoleId(userProfile.getRbacRoles().getId());
            // }
            if (!StringUtils.isEmpty(domainUrl)) {
                try {
                    Roles role = academyDomainUtil.getUserRole(userProfile, domainUrl);
                    userProfileDto.setRoleId(role.getId());
                } catch (ResourceException e) {
                    log.error("Error while mapping role to user in dto: {}", e.getMessage());
                    e.printStackTrace();
                }
            }

            return userProfileDto;
        });

        if (sort) {
            stream = stream.sorted(Comparator.comparing(UserProfileDto::getDisplayName));
        }

        return stream.toList();
    }

    private Instant generateOtpExpiryTime() {
        return Instant.now().plusMillis(otpExpiryTime);
    }

    // private String generateDefaultPassword(String phoneNumber, String name) {
    // return "1234";
    // }

    public static String hashPasswordWithSHA512(String tempPassword) {
        try {
            StringBuilder sb = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(tempPassword.getBytes());

            for (byte b : bytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not found", e);
        }
    }

    private static String generateDefaultPassword(String phoneNumber, String name) {
        return "PlayMo@123";
    }

    // private String generateDefaultPassword(String phoneNumber, String name) {
    // if (name == null || phoneNumber == null) {
    // return "PlayMo@123"; // Fallback password (compliant with all rules)
    // }
    //
    // // Clean and extract name
    // String cleanedName;
    // String trimmedName = name.trim();
    //
    // if (trimmedName.isEmpty()) {
    // cleanedName = "Usr"; // Default if name is empty after trimming
    // } else if (trimmedName.contains(" ")) {
    // // Name contains space - get first part
    // String firstPart = trimmedName.split("\\s+")[0];
    //
    // // Format name with first letter uppercase, rest lowercase
    // firstPart = firstPart.substring(0, 1).toUpperCase()
    // + (firstPart.length() > 1 ? firstPart.substring(1).toLowerCase() : "");
    //
    // // Apply length rules
    // if (firstPart.length() <= 10) {
    // cleanedName = firstPart;
    // } else {
    // cleanedName = firstPart.substring(0, 3);
    // }
    // } else {
    // // No space in name - use entire name
    // String singleName = trimmedName;
    //
    // // Format name with first letter uppercase, rest lowercase
    // singleName = singleName.substring(0, 1).toUpperCase()
    // + (singleName.length() > 1 ? singleName.substring(1).toLowerCase() : "");
    //
    // // Apply length rules
    // if (singleName.length() <= 10) {
    // cleanedName = singleName;
    // } else {
    // cleanedName = singleName.substring(0, 3);
    // }
    // }
    //
    // // Ensure name is at least 3 characters
    // if (cleanedName.length() < 3) {
    // cleanedName = String.format("%-3s", cleanedName).replace(' ', 'x');
    // }
    //
    // // Extract last 5 digits from phone number
    // String digits = phoneNumber.replaceAll("\\D", "");
    // String numberPart;
    //
    // if (digits.isEmpty()) {
    // numberPart = "00000";
    // } else if (digits.length() >= 5) {
    // numberPart = digits.substring(digits.length() - 5);
    // } else {
    // numberPart = String.format("%05d", Integer.parseInt(digits));
    // }
    //
    // // Build the password: [Name]@[Number]
    // String password = cleanedName + "@" + numberPart;
    //
    // return password;
    // }

    private UserProfileDto toDto(UserProfile userProfile) {
        // Sending domainUrl as null
        return toDto(List.of(userProfile), false, null).get(0);
    }

    private void mapUserToAcademy(UserProfile savedUser, UserProfileDto userProfileDto) {
        List<Academy> academies = academyRepo.findAllById(userProfileDto.getAcademyId());

        if (CollectionUtils.isEmpty(academies)) {
            return;
        }

        List<CoachAcademyMapping> listCoachAcademyMappings = academies.stream().map(academy -> {
            CoachAcademyMapping coachAcademyMapping = new CoachAcademyMapping();
            coachAcademyMapping.setId(UUID.randomUUID().toString());
            coachAcademyMapping.setAcademy(academy);
            coachAcademyMapping.setCoachUserProfile(savedUser);
            coachAcademyMapping.setCreatedOn(Timestamp.from(Instant.now()));
            coachAcademyMapping.setRoleId(userProfileDto.getRoleId());
            coachAcademyMapping.setDesignation(userProfileDto.getDesignation());
            coachAcademyMapping.setExperienceInMonths(userProfileDto.getExperienceInMonths());
            coachAcademyMapping.setStatus(Status.ACTIVE);
            return coachAcademyMapping;
        }).toList();

        coachAcademyMappingRepo.saveAll(listCoachAcademyMappings);

    }

    private void mapUserToAcademy(UserProfile userToSave, UpdateUserProfileDto updateUserProfileDto) {
        List<Academy> academies = academyRepo.findAllById(updateUserProfileDto.getAcademyId());

        if (CollectionUtils.isEmpty(academies)) {
            return;
        }

        List<CoachAcademyMapping> listCoachAcademyMappings = academies.stream().map(academy -> {
            CoachAcademyMapping coachAcademyMapping = new CoachAcademyMapping();
            coachAcademyMapping.setId(UUID.randomUUID().toString());
            coachAcademyMapping.setAcademy(academy);
            coachAcademyMapping.setCoachUserProfile(userToSave);
            coachAcademyMapping.setCreatedOn(Timestamp.from(Instant.now()));
            coachAcademyMapping.setDesignation(updateUserProfileDto.getDesignation());
            coachAcademyMapping.setExperienceInMonths(updateUserProfileDto.getExperienceInMonths());
            coachAcademyMapping.setStatus(Status.ACTIVE);
            coachAcademyMapping.setRoleId(updateUserProfileDto.getRoleId());
            return coachAcademyMapping;
        }).toList();

        coachAcademyMappingRepo.saveAll(listCoachAcademyMappings);
    }

    private Roles getRbacRole(Long roleId) {
        if (roleId == null) {
            log.error("Received roleId as: {}", roleId);
            throw new NullPointerException("Not a correct role. Please try again");
        }
        return rbacRoleRepo.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Did not found any role"));
    }

    @Override
    public boolean checkUserExistBeforeOtpForWeb(String domainUrl, String phoneNumber) throws ResourceException {
        try {
            Optional<UserProfile> profile = userProfileRepo.findByPhoneNumberOrEmailId(phoneNumber, phoneNumber);
            if (profile.isEmpty()) {
                return false;
            }

            Roles role = academyDomainUtil.getUserRole(profile.get(), domainUrl);
            // if (ObjectUtils.isEmpty(role))
            // return false;
            // else
            // return true;

            return !ObjectUtils.isEmpty(role);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while checking if user exists before sending otp");
            return false;
        }
    }

    /**
     * Switches the user profile to another account associated with the same phone
     * number
     *
     * @param currentUser    The current user details
     * @param switchToUserId The user ID to switch to
     * @return LoginResponseDto of the switched account
     * @throws ResourceException if the operation fails
     */
    @Override
    public LoginResponseDto switchProfile(UserDetail currentUser, String switchToUserId) throws ResourceException {
        // Validate inputs
        if (currentUser == null || switchToUserId == null || switchToUserId.isEmpty()) {
            throw new IllegalArgumentException("Current user and target user ID must be provided");
        }

        // First, get the current user's profile
        UserProfile currentUserProfile = userProfileRepo.findById(currentUser.getUserId()).orElseThrow(
                () -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Current user profile not found"));

        // Get the user-name from the current user's profile
        String userName = currentUserProfile.getUsername();

        if (userName == null || userName.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "Current user does not have an associated phone number");
        }

        // Find all user profiles associated with this user-name
        List<UserProfile> associatedProfiles = userProfileRepo.findByUsernameAndInactive(userName, false);

        if (associatedProfiles == null || associatedProfiles.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "No active profiles found for the current user's phone number");
        }

        // Check if the requested userId exists in the list of associated profiles
        UserProfile targetProfile = associatedProfiles.stream()
                .filter(profile -> profile.getId().equals(switchToUserId)).findFirst().orElse(null);

        if (targetProfile == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "Requested user profile does not exist or is not associated with your phone number");
        }
        UserDetail userDetail = new UserDetail(targetProfile.getId(), targetProfile.getUsername(), null);

        String jwtToken = jwtService.generateToken(userDetail);
        String refreshToken = jwtService.generateRefreshToken(userDetail);
        long expiresIn = jwtService.getExpirationTime();

        LoginResponseDto loginResponse = new LoginResponseDto();
        loginResponse.setJwtToken(jwtToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setExpiresIn(expiresIn);
        loginResponse.setUserProfile(toDto(targetProfile));

        return loginResponse;
    }

    /**
     * Switches the primary account designation to another profile associated with
     * the same phone number
     *
     * @param currentUser The current user details
     * @param userId      The user ID to set as primary (if null, current user will
     *                    be set as primary)
     * @return List<UserProfileDto> which holds the users object associated with the
     *         same phone number
     * @throws ResourceException if the operation fails
     */
    @Override
    public List<UserProfileDto> switchPrimaryProfile(UserDetail currentUser, String userId) throws ResourceException {
        // If no target userId is provided, use the current user's ID
        String targetUserId = (userId == null || userId.isEmpty()) ? currentUser.getUserId() : userId;

        // Get the current user's profile
        UserProfile currentUserProfile = userProfileRepo.findById(currentUser.getUserId()).orElseThrow(
                () -> new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST, "Current user profile not found"));

        // Get the phone number from the current user's profile
        String phoneNumber = currentUserProfile.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "Current user does not have an associated phone number");
        }

        // Find all user profiles associated with this phone number
        List<UserProfile> associatedProfiles = userProfileRepo.findByPhoneNumberAndInactive(phoneNumber, false);

        if (associatedProfiles == null || associatedProfiles.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                    "No active profiles found for the current user's phone number");
        }

        // Check if the requested userId exists in the list of associated profiles
        UserProfile targetProfile = null;
        if (targetUserId.equals(currentUser.getUserId())) {
            targetProfile = currentUserProfile;
        } else {
            targetProfile = associatedProfiles.stream().filter(profile -> profile.getId().equals(targetUserId))
                    .findFirst().orElse(null);
        }

        if (targetProfile == null) {
            throw new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST,
                    "Requested user profile does not exist or is not associated with your phone number");
        }

        // Update all profiles to non-primary first
        for (UserProfile profile : associatedProfiles) {
            profile.setPrimaryAccount(profile.getId().equals(targetUserId));
            log.info("profile id: {} and its primaryAccount: {}", profile.getId(), profile.isPrimaryAccount());
            // userProfileRepo.save(profile);
        }
        userProfileRepo.saveAll(associatedProfiles);

        return toDto(associatedProfiles, false, null);
    }

    @Override
    public List<BadmintonMatchDto> getUserMatches(String userId) throws ResourceException {
        List<BadmintonMatch> matches = badmintonMatchPlayDetailRepo.findCompletedMatchesForUser(userId);

        return matches.stream()
                .map(this::mapToDto)
                .toList();
    }

    private BadmintonMatchDto mapToDto(BadmintonMatch match) {
        BadmintonMatchDto dto = modelMapper.map(match, BadmintonMatchDto.class);

        if (match.getGameFormat() == GameFormat.DOUBLES) {
            List<TeamDto> teamDtos = new ArrayList<>();

            // Group players by team from badmintonMatchTeamPlayers mapping
            if (match.getBadmintonMatchTeamPlayers() != null && !match.getBadmintonMatchTeamPlayers().isEmpty()) {
                // Group by team
                Map<String, List<BadmintonMatchTeamPlayerMapping>> playersByTeam = match.getBadmintonMatchTeamPlayers()
                        .stream()
                        .collect(Collectors.groupingBy(m -> m.getTeam().getId()));

                // Create TeamDto for each team
                for (Map.Entry<String, List<BadmintonMatchTeamPlayerMapping>> entry : playersByTeam.entrySet()) {
                    TeamDto teamDto = new TeamDto();
                    List<BadmintonMatchTeamPlayerMapping> teamPlayers = entry.getValue();

                    // Set team basic info from the first player's team (all should have same team
                    // info)
                    if (!teamPlayers.isEmpty() && teamPlayers.get(0).getTeam() != null) {
                        Team team = teamPlayers.get(0).getTeam();
                        teamDto = modelMapper.map(team, TeamDto.class);
                    }

                    // Create player DTOs for this team
                    List<TeamPlayerDto> playerDtos = teamPlayers.stream()
                            .map(teamPlayerMapping -> {
                                TeamPlayerDto playerDto = new TeamPlayerDto();

                                if (teamPlayerMapping.getPlayerUserProfile() != null) {
                                    // Regular registered player
                                    playerDto.setPlayerUserId(teamPlayerMapping.getPlayerUserProfile().getId());
                                    playerDto.setUserProfile(modelMapper.map(teamPlayerMapping.getPlayerUserProfile(),
                                            UserProfileMinDto.class));
                                } else if (teamPlayerMapping.getGuestName() != null) {
                                    // Guest player
                                    playerDto.setGuestPlayerName(teamPlayerMapping.getGuestName());
                                }

                                return playerDto;
                            })
                            .toList();

                    teamDto.setPlayers(playerDtos);
                    teamDtos.add(teamDto);
                }
            }

            dto.setTeams(teamDtos);
        }
        return dto;
    }
}
