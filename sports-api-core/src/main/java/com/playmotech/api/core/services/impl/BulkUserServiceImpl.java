package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
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
import com.playmotech.api.core.dao_postgres.UserExpertiseMapping;
import com.playmotech.api.core.dao_postgres.UserPreferredSportsMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.GroupRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.UserAuthDetailsRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.BulkUserService;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.PasswordGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel Modified to fix duplicate entries issue
 **/

@Slf4j
@Service
public class BulkUserServiceImpl implements BulkUserService {

	private final static String OTP = "{OTP}";
	private final static String PHONE_NUMBER = "{PHONE_NUMBER}";

	private final ModelMapper modelMapper = new ModelMapper();
	private final UserProfileRepo userProfileRepo;
	private final PasswordEncoder passwordEncoder;
	private final RolesRepo rbacRoleRepo;
	private final AcademyRepo academyRepo;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final AcademyDomainUtil academyDomainUtil;

	private final UserAuthDetailsRepo userAuthDetailsRepo;

	private final UserProfileHelper validationHelper;

	@Autowired
	public BulkUserServiceImpl(final UserProfileRepo userProfileRepo, IPushNotificationService notificationService,
			TraineeAcademyMappingRepo traineeAcademyMappingRepo, GroupRepo groupRepo, IStorageService storageService,
			final RestTemplate template, IMailService mailService, RolesRepo rbacRoleRepo, AcademyRepo academyRepo,
			CoachAcademyMappingRepo coachAcademyMappingRepo, AcademyDomainUtil academyDomainUtil,
			UserAuthDetailsRepo userAuthDetailsRepo, UserProfileHelper validationHelper) {
		this.userProfileRepo = userProfileRepo;
		this.passwordEncoder = new BCryptPasswordEncoder();
		this.rbacRoleRepo = rbacRoleRepo;
		this.academyRepo = academyRepo;
		this.coachAcademyMappingRepo = coachAcademyMappingRepo;
		this.academyDomainUtil = academyDomainUtil;
		this.userAuthDetailsRepo = userAuthDetailsRepo;
		this.validationHelper = validationHelper;
	}

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

	@Value("${otp-expiry-time}")
	private int otpExpiryTime;

	@Override
	public ServiceResponse create(UserProfileDto dto) throws ResourceException {
		return handleUserProfile(dto, false, true);
	}

	@Override
	public ServiceResponse update(UserProfileDto dto) throws ResourceException {
		return handleUserProfile(dto, false, false);
	}

	@Transactional
	private ServiceResponse handleUserProfile(UserProfileDto dto, boolean bypassOtp, boolean isCreate) {
		try {
			log.info("Starting {} user with phone number: {}", isCreate ? "creation of" : "update for",
					dto.getPhoneNumber());
			UserProfile profile;

			// Check if user already exists by phone number
			List<UserProfile> existingUsers = userProfileRepo.findByPhoneNumberAndInactive(dto.getPhoneNumber(), false);

			if (isCreate) {
				// Original create logic remains the same
				try {
					validationHelper.validateUserProfile(dto.getPhoneNumber(), dto.getDisplayName(), dto.getEmailId(),
							null // No user to exclude since this is a new user
					);
				} catch (ResourceException e) {
					log.warn("User profile validation failed during create: {}", e.getMessage());
					throw e;
				}

				UserProfileHelper.ValidationResult validationResult = validationHelper
						.validateWithDetailedErrors(dto.getPhoneNumber(), dto.getDisplayName(), dto.getEmailId(), null);

				if (validationResult.hasErrors()) {
					return ResponseBuilder.conflict(validationResult.getErrorMessage());
				}

				// Check for existing profiles with the same username (phone number)
				List<UserProfile> existingProfiles = userProfileRepo.findByUsernameOrPhoneNumber(dto.getPhoneNumber());

				UserAuthDetails authDetails = null;

				// Look for existing auth details
				if (!CollectionUtils.isEmpty(existingProfiles)) {
					Optional<UserAuthDetails> optionalAuthDetails = existingProfiles.stream()
							.map(UserProfile::getAuthDetails).filter(Objects::nonNull).findFirst();

					if (optionalAuthDetails.isPresent()) {
						authDetails = optionalAuthDetails.get();
						dto.setPrimaryAccount(false);
						log.info("Found existing authDetails for username '{}'. Setting as secondary account.",
								dto.getPhoneNumber());
					}
				}

				// Create the user profile first
				profile = createNewUserProfile(dto);

				// Set up authentication details
				if (authDetails == null) {
					log.info("No existing authDetails found for username '{}'. Creating new authDetails.",
							dto.getPhoneNumber());
					dto.setPrimaryAccount(true);
					authSetup(dto, profile);
				} else {
					// Link existing auth details to the new profile
					profile.setAuthDetails(authDetails);
					log.info("Linked existing authDetails to new profile for user: {}", profile.getId());
				}

			} else {
				// For update operation - check if ID is present
				if (dto.getId() != null) {
					// ID is present - treat as edit operation on that specific user
					log.info("ID {} present - treating as edit operation on specific user", dto.getId());

					// Find the user by ID
					Optional<UserProfile> existingUserById = userProfileRepo.findById(dto.getId());
					if (!existingUserById.isPresent()) {
						log.error("User with ID {} not found", dto.getId());
						return ResponseBuilder.notFound(ApiResponse.RESOURCE_NOT_FOUND);
					}

					profile = existingUserById.get();

					// Validate that the phone number matches (security check)
					if (!profile.getPhoneNumber().equals(dto.getPhoneNumber())) {
						log.error("Phone number mismatch for user ID {}: expected {}, got {}", dto.getId(),
								profile.getPhoneNumber(), dto.getPhoneNumber());
						return ResponseBuilder.badRequest("Phone number cannot be changed");
					}

					// Check for duplicate display name with same phone number (exclude current
					// user)
					String incomingDisplayName = dto.getDisplayName() != null ? dto.getDisplayName().trim() : null;
					boolean duplicateNameExists = existingUsers.stream()
							.filter(user -> !user.getId().equals(dto.getId())) // Exclude current user
							.anyMatch(user -> {
								String existingDisplayName = user.getDisplayName() != null
										? user.getDisplayName().trim()
										: null;
								return existingDisplayName != null && incomingDisplayName != null
										&& existingDisplayName.equalsIgnoreCase(incomingDisplayName);
							});

					if (duplicateNameExists) {
						log.warn("Duplicate display name '{}' found for phone number '{}' during update of user ID {}",
								incomingDisplayName, dto.getPhoneNumber(), dto.getId());
						return ResponseBuilder.conflict("Display name already exists for this phone number");
					}

					// Validate user profile constraints for update (exclude current user)
					try {
						validationHelper.validateUserProfile(dto.getPhoneNumber(), dto.getDisplayName(),
								dto.getEmailId(), profile.getId() // Exclude current user from validation
						);
					} catch (ResourceException e) {
						log.warn("User profile validation failed during update: {}", e.getMessage());
						throw e;
					}

					// Update basic fields
					modelMapper.map(dto, profile);
					updateUserFields(profile, dto);

					log.info("Updated user profile with ID: {}", profile.getId());

				} else {
					// No ID present - use existing flow for phone number with new name
					UserProfile existingUserWithSameName = null;

					// Check if there's an existing user with same phone number AND same display
					// name
					// (case insensitive, trimmed comparison)
					String incomingDisplayName = dto.getDisplayName() != null ? dto.getDisplayName().trim() : null;
					for (UserProfile existingUser : existingUsers) {
						String existingDisplayName = existingUser.getDisplayName() != null
								? existingUser.getDisplayName().trim()
								: null;

						if (existingDisplayName != null && incomingDisplayName != null
								&& existingDisplayName.equalsIgnoreCase(incomingDisplayName)) {
							existingUserWithSameName = existingUser;
							break;
						}
					}

					if (existingUserWithSameName != null) {
						// Update existing user with same name
						profile = existingUserWithSameName;
						log.debug("Found existing user profile with ID: {} for update", profile.getId());

						// Validate user profile constraints for update (exclude current user)
						try {
							validationHelper.validateUserProfile(dto.getPhoneNumber(), dto.getDisplayName(),
									dto.getEmailId(), profile.getId() // Exclude current user from validation
							);
						} catch (ResourceException e) {
							log.warn("User profile validation failed during update: {}", e.getMessage());
							throw e;
						}

						// Update basic fields
						modelMapper.map(dto, profile);
						updateUserFields(profile, dto);

					} else {
						// Phone number with new name - treat as new user creation
						log.info("Phone number {} with new display name {} - treating as new user creation",
								dto.getPhoneNumber(), dto.getDisplayName());

						// Validate as new user
						try {
							validationHelper.validateUserProfile(dto.getPhoneNumber(), dto.getDisplayName(),
									dto.getEmailId(), null);
						} catch (ResourceException e) {
							log.warn("User profile validation failed for new name: {}", e.getMessage());
							throw e;
						}

						// Check for existing auth details for this phone number
						List<UserProfile> existingProfilesForAuth = userProfileRepo
								.findByUsernameAndInactive(dto.getPhoneNumber(), false);

						UserAuthDetails existingAuthDetails = null;
						if (!CollectionUtils.isEmpty(existingProfilesForAuth)) {
							Optional<UserAuthDetails> optionalAuthDetails = existingProfilesForAuth.stream()
									.map(UserProfile::getAuthDetails).filter(Objects::nonNull).findFirst();

							if (optionalAuthDetails.isPresent()) {
								existingAuthDetails = optionalAuthDetails.get();
								dto.setPrimaryAccount(false);
							}
						}

						profile = createNewUserProfile(dto);

						// Set up authentication
						if (existingAuthDetails == null) {
							dto.setPrimaryAccount(true);
							authSetup(dto, profile);
							log.info("Created new authDetails for new user profile: {}", profile.getId());
						} else {
							profile.setAuthDetails(existingAuthDetails);
							log.info("Linked existing authDetails to new user profile: {}", profile.getId());
						}

						isCreate = true; // Change flag to handle as creation
					}
				}
			}

			// Handle sports mappings
			if (!CollectionUtils.isEmpty(dto.getPreferredSports())) {
				if (isCreate) {
					profile.setPreferredSports(buildSportsMappings(profile.getId(), dto.getPreferredSports()));
					log.debug("Created new sports mappings for user: {}", profile.getId());
				} else {
					profile.setPreferredSports(getUserPreferredSportsMappings(profile.getId(), dto.getPreferredSports(),
							profile.getPreferredSports()));
					log.debug("Updated sports mappings for user: {}", profile.getId());
				}
			}

			// Handle expertise mappings
			if (!CollectionUtils.isEmpty(dto.getExpertiseLevel())) {
				if (isCreate) {
					profile.setExpertiseLevel(buildExpertiseMappings(profile.getId(), dto.getExpertiseLevel()));
					log.debug("Created new expertise mappings for user: {}", profile.getId());
				} else {
					profile.setExpertiseLevel(getUserExpertiseMappings(profile.getId(), dto.getExpertiseLevel(),
							profile.getExpertiseLevel()));
					log.debug("Updated expertise mappings for user: {}", profile.getId());
				}
			}

			// Handle role mappings
			handleRoleMappings(profile, dto, isCreate);

			// Send OTP for new registrations if configured
			if (!bypassOtp && sendOtpOnRegistration && isCreate) {
				sendOtp(profile.getPhoneNumber());
				log.info("OTP sent to phone number: {}", profile.getPhoneNumber());
			}

			// Verify role exists
			Roles role = getRbacRole(dto.getRoleId());
			log.debug("Verified role with ID: {}", dto.getRoleId());

			// Save the user profile
			UserProfile saved = userProfileRepo.save(profile);
			log.info("User profile saved with ID: {}", saved.getId());

			// Map coach to academies if applicable
			if (!CollectionUtils.isEmpty(dto.getAcademyId()) && !role.getRoleName().equalsIgnoreCase("PLAYER")) {
				mapUserToAcademy(saved, dto);
				log.info("Mapped user to {} academies", dto.getAcademyId().size());
			}

			return isCreate ? ResponseBuilder.success(saved, ApiResponse.USER_CREATED, HttpStatus.CREATED)
					: ResponseBuilder.success(saved, ApiResponse.USER_UPDATED, HttpStatus.OK);

		} catch (DuplicateKeyException e) {
			log.error("Duplicate key error: {}", e.getMessage());
			return ResponseBuilder.conflict(ApiResponse.DUPLICATE_USER);
		} catch (ResourceNotFoundException e) {
			log.error("Resource not found: {}", e.getMessage());
			return ResponseBuilder.notFound(ApiResponse.RESOURCE_NOT_FOUND);
		} catch (Exception e) {
			log.error("Error {} user: {}", isCreate ? "creating" : "updating", e.getMessage(), e);
			return ResponseBuilder
					.internalServerError(isCreate ? ApiResponse.ERROR_CREATING_USER : ApiResponse.ERROR_UPDATING_USER);
		}
	}

	private UserProfile createNewUserProfile(UserProfileDto dto) {
		UserProfile profile = new UserProfile();
		modelMapper.map(dto, profile);
		profile.setId(UUID.randomUUID().toString());
		profile.setCreatedOn(Timestamp.from(Instant.now()));
		profile.setInactive(false);

		// Apply default values for new users
		applyDefaultValues(profile, dto);

		log.debug("Created new user profile with ID: {}", profile.getId());
		return profile;
	}

	private void handleRoleMappings(UserProfile profile, UserProfileDto dto, boolean isCreate) {
		if (isCreate) {
			// For new users, create fresh role mappings
			Set<Role> rolesToAssignSet = new LinkedHashSet<>();
			rolesToAssignSet.add(dto.getRole() == null ? Role.USER : dto.getRole());
			rolesToAssignSet.add(Role.USER);
			List<Role> rolesToAssign = new ArrayList<>(rolesToAssignSet);
			// Uncomment when ready to use
			// profile.setRoles(getUserRolesMappings(profile.getId(), rolesToAssign,
			// List.of()));
			log.debug("Created role mappings for user: {}", profile.getId());
		} else {
			// For existing users, merge with existing roles
			Set<Role> rolesToEnsureSet = new LinkedHashSet<>();
			rolesToEnsureSet.add(dto.getRole() == null ? Role.USER : dto.getRole());
			rolesToEnsureSet.add(Role.USER);
			List<Role> rolesToEnsure = new ArrayList<>(rolesToEnsureSet);
			// Uncomment when ready to use
			// profile.setRoles(getUserRolesMappings(profile.getId(), rolesToEnsure,
			// profile.getRoles()));
			log.debug("Updated role mappings for user: {}", profile.getId());
		}
	}

	private void authSetup(UserProfileDto dto, UserProfile profile) {
		UserAuthDetails authDetails = null;
		// This check will be after model mapper (in request body primaryAccount won't
		// be present, which then will be set to false)
		if (profile.getAuthDetails() == null) {
			log.info("No existing authDetails found for username '{}'. Creating new authDetails.",
					profile.getUsername());
			authDetails = new UserAuthDetails();
			profile.setPrimaryAccount(true);
		}

		if (!StringUtils.hasText(dto.getPassword())) {
			dto.setPassword(PasswordGenerator.generateDefaultPassword(dto.getPhoneNumber(), dto.getDisplayName()));
			authDetails.setDefaultPassword(true);
		}

		authDetails
				.setPasswordHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512(dto.getPassword())));
		authDetails.setOtpHashed(passwordEncoder.encode(PasswordGenerator.hashPasswordWithSHA512("1234")));

		UserAuthDetails savedUserAuth = userAuthDetailsRepo.save(authDetails);

		profile.setAuthDetails(savedUserAuth);
	}

	private void sendOtp(String phone) {
		try {
			log.debug("Sending OTP to: {}", phone);
			RestTemplate rest = new RestTemplate();
			String otp = generateOtp();
			String url = otpUrl.replace(PHONE_NUMBER, phone).replace(OTP, otp);
			rest.getForObject(url, String.class);
			log.debug("OTP sent successfully to: {}", phone);
		} catch (Exception e) {
			log.error("Failed to send OTP to {}: {}", phone, e.getMessage());
		}
	}

	private String generateOtp() {
		return otpByPassUsernames.contains("default") ? "1234" : String.valueOf(new Random().nextInt(9000) + 1000);
	}

	/**
	 * Apply default values for new users
	 */
	private void applyDefaultValues(UserProfile profile, UserProfileDto dto) {
		log.debug("Applying default values for new user");

		// Set user type
		profile.setUserType(dto.getUserType() == null ? UserType.PLAYER : dto.getUserType());

		// Set role
		profile.setRole(dto.getRole() == null ? Role.USER : dto.getRole());

		// Set username if not provided
		if (dto.getUsername() == null || dto.getUsername().isBlank()) {
			profile.setUsername(dto.getPhoneNumber());
		}

//		// Set password or default
//		if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
//			profile.setPasswordHashed(passwordEncoder.encode(dto.getPassword()));
//		} else {
//			profile.setPasswordHashed(passwordEncoder.encode("1234"));
//		    profile.setDefaultPassword(false);
//		}
//
//		// Set default OTP
//		profile.setOtpHashed(passwordEncoder.encode("1234"));

		// Set profile picture based on gender
		if (dto.getGender() == Gender.MALE) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlMale);
		} else if (dto.getGender() == Gender.FEMALE) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlFemale);
		}
	}

	/**
	 * Update fields for existing users
	 */
	private void updateUserFields(UserProfile profile, UserProfileDto dto) {
		log.debug("Updating fields for existing user");

		// Update user type if provided
		if (dto.getUserType() != null) {
			profile.setUserType(dto.getUserType());
		}

		// Update role if provided
		if (dto.getRole() != null) {
			profile.setRole(dto.getRole());
		}

		// Update username if provided
		if (StringUtils.hasText(dto.getUsername())) {
			profile.setUsername(dto.getUsername());
		} else if (StringUtils.hasText(dto.getPhoneNumber())) {
			profile.setUsername(dto.getPhoneNumber());
		}

		// Update profile picture based on gender
		if (dto.getGender() == Gender.MALE && !StringUtils.hasText(profile.getProfilePictureUrl())) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlMale);
		} else if (dto.getGender() == Gender.FEMALE && !StringUtils.hasText(profile.getProfilePictureUrl())) {
			profile.setProfilePictureUrl(defaultProfilePictureUrlFemale);
		}
	}

	private List<UserPreferredSportsMapping> buildSportsMappings(String userId, Set<Sports> sports) {
		log.debug("Building sports mappings for user {}", userId);
		return sports.stream().map(s -> {
			UserPreferredSportsMapping m = new UserPreferredSportsMapping();
			m.setUserProfile(UserProfile.builder().id(userId).build());
			m.setSport(s);
			return m;
		}).collect(Collectors.toList());
	}

	private List<UserExpertiseMapping> buildExpertiseMappings(String userId, Map<Sports, SkillLevel> expertise) {
		log.debug("Building expertise mappings for user {}", userId);
		return expertise.entrySet().stream().map(e -> {
			UserExpertiseMapping m = new UserExpertiseMapping();
			m.setUserProfile(UserProfile.builder().id(userId).build());
			m.setSport(e.getKey());
			m.setExpertise(e.getValue());
			return m;
		}).collect(Collectors.toList());
	}

	private List<UserPreferredSportsMapping> getUserPreferredSportsMappings(String userId, Set<Sports> sports,
			List<UserPreferredSportsMapping> existingPreferredSports) {
		log.debug("Processing sports mappings for user {}", userId);

		Map<Sports, UserPreferredSportsMapping> existingSports = CollectionUtils.isEmpty(existingPreferredSports)
				? new HashMap<>()
				: existingPreferredSports.stream().collect(Collectors.toMap(UserPreferredSportsMapping::getSport,
						userPreferredSportsMapping -> userPreferredSportsMapping, (existing, replacement) -> existing // Keep
																														// first
																														// in
																														// case
																														// of
																														// duplicates
				));

		// Create result list starting with newly requested sports
		List<UserPreferredSportsMapping> result = new ArrayList<>();
		sports.forEach(sport -> {
			if (existingSports.containsKey(sport)) {
				// Reuse existing mapping
				result.add(existingSports.get(sport));
				log.debug("Reusing existing sport mapping for: {}", sport);
			} else {
				// Create new mapping
				UserPreferredSportsMapping mapping = new UserPreferredSportsMapping();
				mapping.setUserProfile(UserProfile.builder().id(userId).build());
				mapping.setSport(sport);
				result.add(mapping);
				log.debug("Creating new sport mapping for: {}", sport);
			}
		});

		return result;
	}

	private List<UserExpertiseMapping> getUserExpertiseMappings(String userId, Map<Sports, SkillLevel> skillLevels,
			List<UserExpertiseMapping> expertiseMappingList) {
		log.debug("Processing expertise mappings for user {}", userId);

		Map<Sports, UserExpertiseMapping> existingSkillLevels = CollectionUtils.isEmpty(expertiseMappingList)
				? new HashMap<>()
				: expertiseMappingList.stream().collect(Collectors.toMap(UserExpertiseMapping::getSport,
						userExpertiseMapping -> userExpertiseMapping, (existing, replacement) -> existing // Keep first
																											// in case
																											// of
																											// duplicates
				));

		// Process requested expertise mappings
		List<UserExpertiseMapping> result = new ArrayList<>();
		skillLevels.forEach((sport, level) -> {
			if (existingSkillLevels.containsKey(sport)) {
				// Update existing mapping
				UserExpertiseMapping mapping = existingSkillLevels.get(sport);
				mapping.setExpertise(level);
				result.add(mapping);
				log.debug("Updating expertise for sport {}: {}", sport, level);
			} else {
				// Create new mapping
				UserExpertiseMapping mapping = new UserExpertiseMapping();
				mapping.setUserProfile(UserProfile.builder().id(userId).build());
				mapping.setSport(sport);
				mapping.setExpertise(level);
				result.add(mapping);
				log.debug("Creating new expertise for sport {}: {}", sport, level);
			}
		});

		return result;
	}

	@Transactional
	private void mapUserToAcademy(UserProfile savedUser, UserProfileDto userProfileDto) {
		if (CollectionUtils.isEmpty(userProfileDto.getAcademyId())) {
			log.debug("No academies to map for user {}", savedUser.getId());
			return;
		}

		log.info("Mapping user {} to {} academies", savedUser.getId(), userProfileDto.getAcademyId().size());

		try {
			// Get existing mappings for this user
			List<CoachAcademyMapping> existingMappings = coachAcademyMappingRepo
					.findByCoachUserProfileId(savedUser.getId());
			log.debug("Found {} existing academy mappings", existingMappings.size());

			// Create lookup map by academy ID
			Map<String, CoachAcademyMapping> existingMappingsMap = existingMappings.stream()
					.filter(m -> m.getAcademy() != null && m.getAcademy().getId() != null)
					.collect(Collectors.toMap(m -> m.getAcademy().getId(), m -> m, (existing, replacement) -> {
						log.warn("Duplicate coach-academy mapping found for academy: {}",
								existing.getAcademy().getId());
						return existing; // In case of duplicates, keep the first one
					}));

			// Fetch all academies at once
			List<Academy> academies = academyRepo.findAllById(userProfileDto.getAcademyId());
			log.debug("Found {} academies from {} requested IDs", academies.size(),
					userProfileDto.getAcademyId().size());

			if (CollectionUtils.isEmpty(academies)) {
				log.warn("No valid academies found for IDs: {}", userProfileDto.getAcademyId());
				return;
			}

			// Create or update mappings
			List<CoachAcademyMapping> mappingsToSave = new ArrayList<>();
			for (Academy academy : academies) {
				String academyId = academy.getId();

				if (existingMappingsMap.containsKey(academyId)) {
					// Update existing mapping
					CoachAcademyMapping mapping = existingMappingsMap.get(academyId);
					log.info("Updating existing academy mapping for coach {} and academy {}", savedUser.getId(),
							academyId);

					mapping.setRoleId(userProfileDto.getRoleId());

					if (!ObjectUtils.isEmpty(userProfileDto.getDesignation())) {
						mapping.setDesignation(userProfileDto.getDesignation());
					}

					if (userProfileDto.getExperienceInMonths() != null) {
						mapping.setExperienceInMonths(userProfileDto.getExperienceInMonths());
					}

					mapping.setStatus(Status.ACTIVE);
					mappingsToSave.add(mapping);
				} else {
					// Create new mapping
					log.info("Creating new academy mapping for coach {} and academy {}", savedUser.getId(), academyId);

					CoachAcademyMapping newMapping = new CoachAcademyMapping();
					newMapping.setId(UUID.randomUUID().toString());
					newMapping.setAcademy(academy);
					newMapping.setCoachUserProfile(savedUser);
					newMapping.setCreatedOn(Timestamp.from(Instant.now()));
					newMapping.setRoleId(userProfileDto.getRoleId());
					newMapping.setDesignation(userProfileDto.getDesignation());
					newMapping.setExperienceInMonths(userProfileDto.getExperienceInMonths());
					newMapping.setStatus(Status.ACTIVE);
					mappingsToSave.add(newMapping);
				}
			}

			// Save all mappings in one operation
			if (!mappingsToSave.isEmpty()) {
				coachAcademyMappingRepo.saveAll(mappingsToSave);
				log.info("Successfully saved {} academy mappings for user {}", mappingsToSave.size(),
						savedUser.getId());
			}

		} catch (Exception e) {
			log.error("Error mapping user to academies: {}", e.getMessage(), e);
			throw e; // Re-throw to trigger transaction rollback
		}
	}

	private Roles getRbacRole(Long roleId) {
		if (roleId == null) {
			log.error("Received null roleId");
			throw new NullPointerException("Not a correct role. Please try again");
		}

		Optional<Roles> role = rbacRoleRepo.findById(roleId);
		if (role.isEmpty()) {
			log.error("Role not found with ID: {}", roleId);
			throw new ResourceNotFoundException("Did not find any role with ID: " + roleId);
		}

		return role.get();
	}

	public boolean checkUserExistBeforeOtpForWeb(String domainUrl, String phoneNumber) throws ResourceException {
		try {
			log.debug("Checking if user exists before OTP for domain {} and phone {}", domainUrl, phoneNumber);
			Optional<UserProfile> profile = userProfileRepo.findByPhoneNumberOrEmailId(phoneNumber, phoneNumber);
			if (profile.isEmpty()) {
				log.debug("No user found with phone/email: {}", phoneNumber);
				return false;
			}

			Roles role = academyDomainUtil.getUserRole(profile.get(), domainUrl);
			boolean exists = !ObjectUtils.isEmpty(role);
			log.debug("User exists check result: {} for phone: {}", exists, phoneNumber);
			return exists;
		} catch (Exception e) {
			log.error("Error checking if user exists before sending OTP: {}", e.getMessage(), e);
//            throw new ResourceException("Error checking user existence", HttpStatus.INTERNAL_SERVER_ERROR);
			return false;
		}
	}
}