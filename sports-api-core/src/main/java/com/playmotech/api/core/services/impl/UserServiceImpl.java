package com.playmotech.api.core.services.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.UserAuthDetails;
import com.playmotech.api.core.dao_postgres.UserDocuments;
import com.playmotech.api.core.dao_postgres.UserExpertiseMapping;
import com.playmotech.api.core.dao_postgres.UserPreferredSportsMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UsersActionsMapping;
import com.playmotech.api.core.dto.ChangePasswordDto;
import com.playmotech.api.core.dto.CoachAcademyDetails;
import com.playmotech.api.core.dto.FileObjectDetails;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.LoginResponseDto;
import com.playmotech.api.core.dto.LoginUserDto;
import com.playmotech.api.core.dto.ModulesActionsDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserDocumentsDto;
import com.playmotech.api.core.dto.UserExistsDto;
import com.playmotech.api.core.dto.UserListBasedOnRoleDto;
import com.playmotech.api.core.dto.UserProfileAddEditDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.UserMapper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.UserAuthDetailsRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.repo.UsersActionsMappingRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.UserProfileDao;
import com.playmotech.api.core.security.JwtService;
import com.playmotech.api.core.services.IAuthenticationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.UserService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UsersActionsMappingRepo usersActionsMappingRepository;
    private final CoachAcademyMappingRepo coachAcademyMappingRepository;
    private final IStorageService storageService;
    private final AcademyRepo academyRepository;
    private final UserProfileRepo userProfileRepository;
    private final UserAuthDetailsRepo userAuthDetailsRepo;

    private final IUserProfileService userProfileService;
    private final IAuthenticationService authenticationService;
    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    ;

    private final RolesService roleService;
    private final AcademyDomainUtil academyDomainUtil;
    private final UserProfileHelper validationHelper;

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
    public ServiceResponse getUserProfileById(String id) {
        try {
            Optional<UserProfile> optionalUser = userProfileRepository.findById(id);

            if (optionalUser.isEmpty()) {
                log.info(ApiResponse.NOT_FOUND_PROFILE.message);
                return ResponseBuilder.success(ApiResponse.NOT_FOUND_PROFILE);
            }

            UserProfileDao userDao = UserMapper.mapUserToUserDao(optionalUser.get());

            return ResponseBuilder.success(userDao, ApiResponse.USER_FETCHED);
        } catch (Exception e) {
            log.error("Exception occurred while retrieving user profile: {}", e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    @Transactional
    public ServiceResponse deleteUser(String id, String userId) {
        try {
            userProfileRepository.toggleInActiveStatus(id);
            return ResponseBuilder.success(ApiResponse.USER_INACTIVATED_SUCCESSFULLY);
        } catch (Exception e) {
            log.error("Exception occurred while inactivating user: {}", e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    public ServiceResponse getUsersList(GenericFilter filter, String userId, String domainUrl) {
        try {
            String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

            System.out.println("userId: " + userId);
            System.out.println("domainUrl: " + domainUrl);

            List<Object[]> results = userProfileRepository.findUsersListRoleWise(userId, userRole, domainUrl);

            if (results.isEmpty()) {
                log.info("Empty users list");
                return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND);
            }

            List<UserListBasedOnRoleDto> usersList = results.stream().map(obj -> {
                UserListBasedOnRoleDto dto = new UserListBasedOnRoleDto();
                dto.setId((String) obj[0]);
                dto.setName((String) obj[1]);
                dto.setPhoneNumber((String) obj[2]);
                dto.setEmailId((String) obj[3]);
                dto.setRoleName((String) obj[4]);
                dto.setSport((String) obj[5]);
                dto.setBranch((String) obj[6]);
                dto.setTitles((String) obj[7]);
                dto.setInactive((boolean) obj[8]);

                // Handle text[] arrays
                List<String> programs = new ArrayList<>();
                if (obj[9] != null) {
                    String[] progArray = (String[]) obj[9];
                    programs = Arrays.asList(progArray);
                }

                List<String> sports = new ArrayList<>();
                if (obj[10] != null) {
                    String[] sportArray = (String[]) obj[10];
                    sports = Arrays.asList(sportArray);
                }

                dto.setAssociatedPrograms(programs); // associated_programs
                dto.setSports(sports); // sports

                // String fields
                dto.setAcademyIds((String) obj[11]);
                dto.setAcademyJson((String) obj[12]);

                // Handle created_on timestamp (index 13)
                if (obj[13] != null) {
                    dto.setCreatedOn((Timestamp) obj[13]);
                }

                return dto;
            }).toList();

            return ResponseBuilder.success(usersList, ApiResponse.LIST_FETCHED_SUCCESSFULLY);

        } catch (Exception e) {
            log.error("Exception occurred while fetching users: {}", e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
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

    @Override
    @Transactional
    public ServiceResponse addUser(UserProfileAddEditDto userProfileDto, String userId) {
        try {
            Optional<UserProfile> userAdding = userProfileRepository.findById(userId);

            if (userAdding.isEmpty()) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_CREDENTIALS);
            }

            // Validate user profile using helper
            try {
                validationHelper.validateUserProfile(userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName(),
                        userProfileDto.getEmailId(), null // No user to exclude for new user creation
                );
            } catch (ResourceException e) {
                return ResponseBuilder.conflict(e.getMessage());
            }

            UserProfileHelper.ValidationResult validationResult = validationHelper.validateWithDetailedErrors(
                    userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName(), userProfileDto.getEmailId(),
                    null);

            if (validationResult.hasErrors()) {
                return ResponseBuilder.conflict(validationResult.getErrorMessage());
            }

            // String addingUserRole = userAdding.get().getRole().getRoleName();
            // String newUserRole = userProfileDto.getRole().getRoleName();
            //
            // if ("ADMIN".equalsIgnoreCase(addingUserRole) &&
            // "ADMIN".equalsIgnoreCase(newUserRole)) {
            // return ResponseBuilder.forbidden("Admin cannot add another admin");
            // }

            // Optional<UserProfile> duplicatePhoneNumber = userProfileRepository
            // .findByPhoneNumberAndInactiveIsFalse(userProfileDto.getUsername());
            List<UserProfile> existingProfiles = userProfileRepository
                    .findByUsernameAndInactive(userProfileDto.getPhoneNumber(), false);

            UserAuthDetails authDetails = null;

            if (!CollectionUtils.isEmpty(existingProfiles)) {
                Optional<UserAuthDetails> optionalAuthDetails = existingProfiles.stream()
                        .map(UserProfile::getAuthDetails).filter(Objects::nonNull).findFirst();

                if (optionalAuthDetails.isPresent()) {
                    authDetails = optionalAuthDetails.get();
                    userProfileDto.setPrimaryAccount(false);
                }
            }

            // Optional<UserProfile> duplicateEmail = userProfileRepository
            // .findByEmailIdAndInactiveIsFalse(userProfileDto.getEmailId());

            // TODO add check when ui supports email
            // Optional<UserProfile> duplicateEmail =
            // userProfileRepository.findByEmailId(userProfileDto.getEmailId());
            //
            // if (duplicateEmail.isPresent()) {
            // return ResponseBuilder.conflict(ApiResponse.DUPLICATE_EMAIL);
            // }

            UserProfile profileToSave = UserMapper.mapUserDtoToUser(userProfileDto);

            // This check will be after model mapper (in request body primaryAccount won't
            // be present, which then will be set to false)
            if (authDetails == null) {
                log.info("No existing authDetails found for username '{}'. Creating new authDetails.",
                        userProfileDto.getUsername());
                authDetails = new UserAuthDetails();
                profileToSave.setPrimaryAccount(true);
            }

            profileToSave.setId(UUID.randomUUID().toString());

            profileToSave.setRole(Role.USER);

            if (userProfileDto.getGender() != null && userProfileDto.getGender().equals(Gender.MALE)) {
                profileToSave.setProfilePictureUrl(defaultProfilePictureUrlMale);
            } else if (userProfileDto.getGender().equals(Gender.FEMALE)) {
                profileToSave.setProfilePictureUrl(defaultProfilePictureUrlFemale);
            }

            if (userProfileDto.getPreferredSports() != null && !userProfileDto.getPreferredSports().isEmpty()) {
                profileToSave.setPreferredSports(getUserPreferredSportsMapping(profileToSave.getId(),
                        userProfileDto.getPreferredSports(), List.of()));
            }

            if (ObjectUtils.isNotEmpty(userProfileDto.getUploadedDocuments())) {
                profileToSave.setUserDocuments(
                        mapDocumentsDtoToDocumnet(userProfileDto.getUploadedDocuments(), profileToSave));
            }

            // For roles ADMIN & COACHES, set UserType as COACH.
            // For rest, set UserType as PLAYER.
            Set<String> coachRoles = Set.of("SUPER_ADMIN", "ADMIN", "COACHES", "COACH");
            UserType userType = coachRoles.contains(userProfileDto.getRole().getRoleName()) ? UserType.COACH
                    : UserType.PLAYER;
            profileToSave.setUserType(userType);

            profileToSave.setCreatedOn(Timestamp.from(Instant.now()));

            if (!StringUtils.hasText(userProfileDto.getPassword())) {
                userProfileDto.setPassword(
                        generateDefaultPassword(userProfileDto.getPhoneNumber(), userProfileDto.getDisplayName()));
                // profileToSave.getAuthDetails().setDefaultPassword(true);
                authDetails.setDefaultPassword(true);
            }

            authDetails.setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(userProfileDto.getPassword())));
            authDetails.setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

            UserAuthDetails savedUserAuth = userAuthDetailsRepo.save(authDetails);

            profileToSave.setAuthDetails(savedUserAuth);

            UserProfile savedUser = userProfileRepository.save(profileToSave);

            if (userProfileDto.getUserActions() != null) {
                addEditUserActions(savedUser, userProfileDto.getUserActions());
            }
            //
            if (!CollectionUtils.isEmpty(userProfileDto.getAcademyId())) {
                mapCoachToAcademy(savedUser, userProfileDto);
            }

            if (userProfileDto.getAcademyCoaches() != null && !userProfileDto.getAcademyCoaches().isEmpty()
                    && userProfileDto.getAcademyCoaches().stream()
                    .allMatch(dto -> dto.getAcademyId() != null && !dto.getAcademyId().isBlank())) {

                mapCoachToAcademyWithDesignation(savedUser, userProfileDto);
            }

            return ResponseBuilder.success(ApiResponse.DATA_ADDED_SUCCESSFULLY);
        } catch (Exception e) {
            log.error("Exception occurred while adding user: {}", e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    public ServiceResponse fileUpload(FileObjectDetails filesToUpload) {
        String prefix = "users-media/" + UUID.randomUUID() + "_" + filesToUpload.getFile().getOriginalFilename();
//        List<MultipartFile> files = new ArrayList<>();
//        files.add(filesToUpload.getFile());

//        ServiceResponse response = storageService.uploadFileToS3Bucket(usersMediaBucket, prefix, files, filesToUpload.getFile().getContentType());
//        response.setBody(usersMediaBaseUrl.concat(prefix));
//        return response;

        try {
            FileObjectDto dto = new FileObjectDto();
            MultipartFile file = filesToUpload.getFile();

            dto.setContentType(file.getContentType());
            dto.setContent(file.getBytes());
            dto.setOriginalFilename(file.getOriginalFilename());
            storageService.upload(usersMediaBucket, prefix, dto.getContent(), dto.getContentType());

            return ResponseBuilder.success(usersMediaBaseUrl.concat(prefix), ApiResponse.FILE_UPLOADED_SUCCESSFULLY);
        } catch (Exception e) {
            log.error("Exception occurred while uploading file: {}", e.getMessage());
            return ResponseBuilder.internalServerError("Something went wrong while uploading file");
        }
    }

    @Override
    @Transactional
    public ServiceResponse editUser(UserProfileAddEditDto userProfileToUpdateDto, String userId) {
        try {
            if (!StringUtils.hasText(userProfileToUpdateDto.getId())) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }

            Optional<UserProfile> userUpdating = userProfileRepository.findById(userId);

            if (userUpdating.get().getUserType() != UserType.COACH
                    && userId.equalsIgnoreCase(userProfileToUpdateDto.getId())) {
                return ResponseBuilder.forbidden("User does not match with the logged in user");
            }

            Optional<UserProfile> existingUser = userProfileRepository.findById(userProfileToUpdateDto.getId());

            if (existingUser.isEmpty()) {
                return ResponseBuilder.badRequest(ApiResponse.NOT_FOUND_PROFILE);
            }

            // Get current user's phone number for validation
            String currentPhoneNumber = existingUser.get().getPhoneNumber();
            String excludeUserId = userProfileToUpdateDto.getId();

            // Only validate fields that are actually being updated
            String displayNameToValidate = null;
            String emailToValidate = null;

            if (StringUtils.hasText(userProfileToUpdateDto.getDisplayName())
                    && !userProfileToUpdateDto.getDisplayName().equals(existingUser.get().getDisplayName())) {
                displayNameToValidate = userProfileToUpdateDto.getDisplayName();
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getEmailId())
                    && !userProfileToUpdateDto.getEmailId().equals(existingUser.get().getEmailId())) {
                emailToValidate = userProfileToUpdateDto.getEmailId();
            }

            // Validate only if there are changes
            if (displayNameToValidate != null || emailToValidate != null) {
                try {
                    validationHelper.validateUserProfile(currentPhoneNumber, displayNameToValidate, emailToValidate,
                            excludeUserId);
                } catch (ResourceException e) {
                    return ResponseBuilder.conflict(e.getMessage());
                }
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getDisplayName())) {
                existingUser.get().setDisplayName(userProfileToUpdateDto.getDisplayName());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getEmailId())) {
                existingUser.get().setEmailId(userProfileToUpdateDto.getEmailId());
            }

            if (userProfileToUpdateDto.getUserType() != null) {
                existingUser.get().setUserType(userProfileToUpdateDto.getUserType());
            }

            if (userProfileToUpdateDto.getGender() != null) {
                existingUser.get().setGender(userProfileToUpdateDto.getGender());
            }

            if (!CollectionUtils.isEmpty(userProfileToUpdateDto.getPreferredSports())) {
                existingUser.get().setPreferredSports(getUserPreferredSportsMapping(userProfileToUpdateDto.getId(),
                        userProfileToUpdateDto.getPreferredSports(), existingUser.get().getPreferredSports()));
            }

            if (!CollectionUtils.isEmpty(userProfileToUpdateDto.getExpertiseLevel())) {
                existingUser.get().setExpertiseLevel(getUserExpertiseMapping(userProfileToUpdateDto.getId(),
                        userProfileToUpdateDto.getExpertiseLevel(), existingUser.get().getExpertiseLevel()));
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getDob())) {
                existingUser.get().setDob(userProfileToUpdateDto.getDob());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getAddressLine1())) {
                existingUser.get().setAddressLine1(userProfileToUpdateDto.getAddressLine1());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getAddressLine2())) {
                existingUser.get().setAddressLine2(userProfileToUpdateDto.getAddressLine2());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getCity())) {
                existingUser.get().setCity(userProfileToUpdateDto.getCity());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getState())) {
                existingUser.get().setState(userProfileToUpdateDto.getState());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getCountry())) {
                existingUser.get().setCountry(userProfileToUpdateDto.getCountry());
            }

            if (StringUtils.hasText(userProfileToUpdateDto.getPincode())) {
                existingUser.get().setPincode(userProfileToUpdateDto.getPincode());
            }

            if (userProfileToUpdateDto.getExperienceInMonths() != null) {
                existingUser.get().setExperienceInMonths(userProfileToUpdateDto.getExperienceInMonths());
            }

            UserDocumentsDto uploadedDocs = userProfileToUpdateDto.getUploadedDocuments();
            UserDocuments existingDocs = existingUser.get().getUserDocuments();

            if (uploadedDocs != null && existingDocs != null) {
                if (!Objects.equals(existingDocs.getAadharUrl(), uploadedDocs.getAadharUrl())) {
                    existingDocs.setAadharUrl(uploadedDocs.getAadharUrl());
                }
                if (!Objects.equals(existingDocs.getPanUrl(), uploadedDocs.getPanUrl())) {
                    existingDocs.setPanUrl(uploadedDocs.getPanUrl());
                }
            }

            existingUser.get().setUserDocuments(existingDocs);

            if (existingUser.get().getRole() == null || existingUser.get().getRole().name().isBlank()
                    || existingUser.get().getRole().name().isEmpty()) {
                existingUser.get().setRole(Role.USER);
            }

            UserProfile savedUser = userProfileRepository.save(existingUser.get());

            if (userProfileToUpdateDto.getUserActions() != null) {
                addEditUserActions(savedUser, userProfileToUpdateDto.getUserActions());
            }

            if (userProfileToUpdateDto.getAcademyCoaches() != null
                    && !userProfileToUpdateDto.getAcademyCoaches().isEmpty()
                    && userProfileToUpdateDto.getAcademyCoaches().stream()
                    .allMatch(dto -> dto.getAcademyId() != null && !dto.getAcademyId().isBlank())) {

                mapCoachToAcademyWithDesignation(savedUser, userProfileToUpdateDto);
            }

            return ResponseBuilder.success(ApiResponse.DATA_ADDED_SUCCESSFULLY);
        } catch (Exception e) {
            log.error("Exception occurred while editing user: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }

    @Override
    public ServiceResponse getUserBasedRoleActions(String userId, String domainUrl) {
        Optional<UserProfile> userProfileOptional;
        List<ModulesActionsDto> modulesActionsDtoList;
        try {
            userProfileOptional = userProfileRepository.findByIdAndInactiveIsFalse(userId);

            if (!userProfileOptional.isPresent()) {
                return ResponseBuilder.notFound("User not found");
            }

            UserProfile userProfile = userProfileOptional.get();

            modulesActionsDtoList = roleService.getModulesActionsDtoListByUser(userProfile, domainUrl);

            return ResponseBuilder.success(modulesActionsDtoList,
                    modulesActionsDtoList.isEmpty() ? ApiResponse.NO_RECORD_FOUND
                            : ApiResponse.LIST_FETCHED_SUCCESSFULLY,
                    HttpStatus.OK);
        } catch (Exception e) {
            log.error("Exception in UserServiceImpl.getUserBasedRoleActions " + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
        }
    }

    private List<UserExpertiseMapping> getUserExpertiseMapping(String userId, Map<Sports, SkillLevel> skillLevels,
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
        // .toList() returns immutable list. DO NOT REMOVE THIS
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

    private void mapCoachToAcademy(UserProfile savedUser, UserProfileAddEditDto userProfileDto) {
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

    @Transactional
    public List<CoachAcademyMapping> mapCoachToAcademyWithDesignation(UserProfile savedUser,
                                                                      UserProfileAddEditDto userProfileDto) {

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

    public Map<String, String> mapAcademyDesignations(List<String> academyIds, UserProfileAddEditDto userProfileDto) {
        if (academyIds == null || academyIds.isEmpty() || userProfileDto == null) {
            return Map.of();
        }

        List<Academy> academies = academyRepository.findAllById(academyIds);
        return academies.stream().collect(Collectors.toMap(Academy::getId, academy -> userProfileDto.getDesignation()));
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

    private UserDocuments mapDocumentsDtoToDocumnet(UserDocumentsDto dtos, UserProfile profileToSave) {
        UserDocuments docu = new UserDocuments();
        BeanUtils.copyProperties(dtos, docu);
        docu.setUser(profileToSave);

        return docu;
    }

    @Override
    public ServiceResponse changePassword(ChangePasswordDto passwordDto, String userId) {
        try {
            Optional<UserProfile> userProfileOptional = userProfileRepository
                    .findByEmailIdAndPrimaryAccountIsTrue(passwordDto.getEmail());

            if (userProfileOptional.isEmpty()) {
                return ResponseBuilder.notFound(ApiResponse.RESOURCE_NOT_FOUND);
            }

            UserProfile userProfile = userProfileOptional.get();

            // // Additional check: verify the email belongs to the correct userId
            // if (!userProfile.getId().equals(userId)) {
            // return ResponseBuilder.badRequest(ApiResponse.USER_ID_MISMATCH);
            // }

            // Compare already hashed old password
            if (!passwordEncoder.matches(passwordDto.getOldPassword(),
                    userProfile.getAuthDetails().getPasswordHashed())) {
                return ResponseBuilder.notAcceptable(ApiResponse.INVALID_CURRENT_PASSWORD);
            }

            // Set new password (already hashed)
            userProfile.getAuthDetails().setPasswordHashed(passwordEncoder.encode(passwordDto.getNewPassword()));
            userProfile.getAuthDetails().setDefaultPassword(false);

            userProfileRepository.save(userProfile);

            return ResponseBuilder.success(ApiResponse.PASSWORD_UPDATED_SUCCESSFULLY);

        } catch (Exception e) {
            log.error("Exception in changePassword: " + e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.PASSWORD_UPDATE_FAILED);
        }
    }

    @Override
    public ServiceResponse forgotPasswordForApp(String identifier, String userId) {
        try {
            UserProfile userProfile = userProfileService.getByIdentifier(identifier);

            // if (!userProfile.getId().equals(userId)) {
            // return ResponseBuilder.badRequest(ApiResponse.USER_ID_MISMATCH);
            // }

            boolean isEmailLogin = identifier.equals(userProfile.getEmailId());
            boolean isPhoneLogin = identifier.equals(userProfile.getPhoneNumber());
            boolean isUsernameLogin = identifier.equals(userProfile.getUsername());

            if (!determineVerificationStatus(userProfile, identifier, isEmailLogin, isPhoneLogin, isUsernameLogin)) {
                return ResponseBuilder.notAcceptable(ApiResponse.UNVERIFIED_IDENTIFIER);
            }

            String tempPassword = generateDefaultPassword(userProfile.getPhoneNumber(), userProfile.getDisplayName());
            userProfile.getAuthDetails().setDefaultPassword(true);
            userProfile.getAuthDetails()
                    .setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(tempPassword)));
            userProfile.getAuthDetails().setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

            userProfileRepository.save(userProfile);

            return ResponseBuilder.success(ApiResponse.TEMP_PASSWORD_SENT);

        } catch (Exception e) {
            log.error("Exception in changePassword: " + e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.PASSWORD_UPDATE_FAILED);
        }
    }

    @Override
    public ServiceResponse forgotPasswordForWeb(String identifier, String userId, String domainUrl) {
        try {

            UserProfile userProfile = userProfileService.getByIdentifier(identifier);

            // if (!userProfile.getId().equals(userId)) {
            // return ResponseBuilder.badRequest(ApiResponse.USER_ID_MISMATCH);
            // }

            boolean isEmailLogin = identifier.equals(userProfile.getEmailId());
            boolean isPhoneLogin = identifier.equals(userProfile.getPhoneNumber());
            boolean isUsernameLogin = identifier.equals(userProfile.getUsername());

            if (!determineVerificationStatus(userProfile, identifier, isEmailLogin, isPhoneLogin, isUsernameLogin)) {
                return ResponseBuilder.notAcceptable(ApiResponse.UNVERIFIED_IDENTIFIER);
            }

            // Domain role check for web
            if (domainUrl != null) {
                Roles role = academyDomainUtil.getUserRole(userProfile, domainUrl);
                if (role == null) {
                    return ResponseBuilder.badRequest(ApiResponse.USER_NOT_FOUND_IN_ACADEMY);
                }

            }

            String tempPassword = generateDefaultPassword(userProfile.getPhoneNumber(), userProfile.getDisplayName());
            userProfile.getAuthDetails().setDefaultPassword(true);
            userProfile.getAuthDetails()
                    .setPasswordHashed(passwordEncoder.encode(hashPasswordWithSHA512(tempPassword)));
            userProfile.getAuthDetails().setOtpHashed(passwordEncoder.encode(hashPasswordWithSHA512("1234")));

            userProfileRepository.save(userProfile);

            return ResponseBuilder.success(ApiResponse.TEMP_PASSWORD_SENT);

        } catch (Exception e) {
            log.error("Exception in changePassword: " + e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.PASSWORD_UPDATE_FAILED);
        }
    }

    @Override
    public ServiceResponse login(LoginUserDto loginUserDto) {
        try {
            // UserDetail authenticatedUser = authenticationService.login(loginUserDto);
            UserDetail authenticatedUser = authenticationService.newLogin(loginUserDto);

            String jwtToken = jwtService.generateToken(authenticatedUser);
            String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
            long expiresIn = jwtService.getExpirationTime();

            UserProfileDto userProfileDto = userProfileService
                    .getUserProfileByUsername(authenticatedUser.getUsername());

            LoginResponseDto loginResponse = new LoginResponseDto();
            loginResponse.setJwtToken(jwtToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setExpiresIn(expiresIn);
            loginResponse.setUserProfile(userProfileDto);

            return ResponseBuilder.success(loginResponse, ApiResponse.LOGIN_SUCCESSFUL);
        } catch (BadCredentialsException e) {
            log.error("ResourceException during login: {}", e.getMessage(), e);
            return ResponseBuilder.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (ResourceException e) {
            log.error("ResourceException during login: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.LOGIN_FAILED);
        } catch (Exception e) {
            log.error("Exception during login: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError(ApiResponse.LOGIN_FAILED);
        }
    }

    @Override
    public ServiceResponse checkUserExistsAndSendOtp(String identifier, Boolean sendOtp, Boolean usePassword,
                                                     String domainUrl) throws ResourceException {
        try {
            UserProfile userProfile = userProfileService.getByIdentifier(identifier);

            boolean isEmailLogin = identifier.equals(userProfile.getEmailId());
            boolean isPhoneLogin = identifier.equals(userProfile.getPhoneNumber());
            boolean isUsernameLogin = identifier.equals(userProfile.getUsername());

            boolean verified = determineVerificationStatus(userProfile, identifier, isEmailLogin, isPhoneLogin,
                    isUsernameLogin);

            boolean passwordLoginEligible = false;
            boolean isFirstLogin = false;

            String otp = "";
            if (Boolean.TRUE.equals(usePassword)) {
                passwordLoginEligible = userProfile.getAuthDetails().getPasswordHashed() != null;
                isFirstLogin = userProfile.getAuthDetails().isDefaultPassword();
            } else {
                boolean otpByPassCheck = checkOtpBypass(identifier, isEmailLogin, isPhoneLogin);
                otp = sendOtp(sendOtp, usePassword, otpByPassCheck, identifier);
            }

            // defaultSetPasswordAndOtp(userProfile);

            UserExistsDto dto = buildUserExistsDto(true, otp, passwordLoginEligible, isFirstLogin, verified);

            if (domainUrl != null) {
                Roles role = academyDomainUtil.getUserRole(userProfile, domainUrl);
                if (role == null) {
                    return ResponseBuilder.badRequest(ApiResponse.USER_NOT_FOUND_IN_ACADEMY);
                }
                dto.setUserExists(true);
            }

            return ResponseBuilder.success(dto, ApiResponse.USER_FOUND);

        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception in checkUserExistsAndSendOtp: {}", e.getMessage(), e);

            UserExistsDto userExistsDto = new UserExistsDto();
            userExistsDto.setUserExists(false);

            ServiceResponse res = new ServiceResponse();

            res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            // Response<UserExistsDto> dto = Response.<UserExistsDto>builder()
            // .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            // .message(e.getMessage())
            // .body(userExistsDto).build();

            res.setBody(userExistsDto);

            return res;
        }
    }

    private void defaultSetPasswordAndOtp(UserProfile userProfile) {
        String tempPassword = generateDefaultPassword(userProfile.getPhoneNumber(), userProfile.getDisplayName());
        String hashedTempPassword = passwordEncoder.encode(hashPasswordWithSHA512(tempPassword));
        String hashedOtp = passwordEncoder.encode("1234");

        System.out.println(tempPassword);

        boolean needsUpdate = false;

        if (userProfile.getAuthDetails().getPasswordHashed() == null || !passwordEncoder
                .matches(hashPasswordWithSHA512(tempPassword), userProfile.getAuthDetails().getPasswordHashed())) {
            log.info("Setting temporary password for user: {}", userProfile.getId());
            userProfile.getAuthDetails().setPasswordHashed(hashedTempPassword);
            userProfile.getAuthDetails().setDefaultPassword(false);
            needsUpdate = true;
        } else {
            log.info("Temporary password already set and matches for user: {}", userProfile.getId());
        }

        if (userProfile.getAuthDetails().getOtpHashed() == null
                || !passwordEncoder.matches("1234", userProfile.getAuthDetails().getOtpHashed())) {
            log.info("Setting OTP for user: {}", userProfile.getId());
            userProfile.getAuthDetails().setOtpHashed(hashedOtp);
            needsUpdate = true;
        } else {
            log.info("OTP already set and matches for user: {}", userProfile.getId());
        }

        if (needsUpdate) {
            userProfileRepository.save(userProfile);
            log.info("User profile updated for user: {}", userProfile.getId());
        } else {
            log.info("No updates needed for user profile: {}", userProfile.getId());
        }
    }

    private boolean determineVerificationStatus(UserProfile userProfile, String identifier, boolean isEmailLogin,
                                                boolean isPhoneLogin, boolean isUsernameLogin) {
        if (isEmailLogin) {
            return userProfile.getAuthDetails().isEmailIdVerified();
        } else if (isPhoneLogin) {
            return userProfile.getAuthDetails().isPhoneNumberVerified();
        } else if (isUsernameLogin) {
            return checkUsernameVerification(userProfile, identifier);
        }
        return false;
    }

    private boolean checkUsernameVerification(UserProfile userProfile, String identifier) {
        if (isValidEmail(identifier)) {
            return userProfile.getAuthDetails().isEmailIdVerified();
        } else if (isValidPhoneNumber(identifier)) {
            return userProfile.getAuthDetails().isPhoneNumberVerified();
        }
        return false; // or based on business logic
    }

    private boolean checkOtpBypass(String identifier, boolean isEmailLogin, boolean isPhoneLogin) {
        return otpByPass && ((isEmailLogin && otpByPassEmails.contains(identifier))
                || (isPhoneLogin && otpByPassUsernames.contains(identifier)));
    }

    private String sendOtp(boolean sendOtp, boolean usePassword, boolean otpByPassCheck, String identifier)
            throws ResourceException {
//		if (sendOtp && !otpByPassCheck && !usePassword) {
        return userProfileService.sendOtp(identifier);
//		}
//		return "1234";
    }

    private UserExistsDto buildUserExistsDto(boolean exists, String otp, boolean usePassword, boolean firstLogin,
                                             boolean verified) {
        UserExistsDto dto = new UserExistsDto();
        dto.setUserExists(exists);
        dto.setOtp(otp);
        dto.setUsePassword(usePassword);
        dto.setFirstLogin(firstLogin);
        dto.setVerified(verified);
        return dto;
    }

    private boolean isValidEmail(String identifier) {
        return identifier.contains("@");
    }

    private boolean isValidPhoneNumber(String identifier) {
        // Implement comprehensive phone validation
        return identifier.matches("^\\+?[0-9]{7,15}$");
    }

}
