package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.cache.AcademyConfigCache;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.AcademyLead;
import com.playmotech.api.core.dao_postgres.AcademySportMapping;
import com.playmotech.api.core.dao_postgres.Branch;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyLeadDto;
import com.playmotech.api.core.dto.BranchDto;
import com.playmotech.api.core.dto.CreateAcademyDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.GeoFenceDto;
import com.playmotech.api.core.dto.UpdateAcademyDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyLeadRepo;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.OrgRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AcademyService implements IAcademyService {

	private static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private final ModelMapper modelMapper = new ModelMapper();
	private final IUserProfileService userProfileService;
	private final AcademyLeadRepo academyLeadRepo;
	private final AcademyRepo academyRepo;
	private final IPushNotificationService notificationService;
	private final IStorageService storageService;
	private final OrgRepo orgRepo;
	private final AcademyConfigCache academyConfigCache;

	@Value("${default.icons.academy}")
	private String defaultIconUrlAcademy;

	@Value("${academy-media-base-url}")
	private String academyMediaBaseUrl;

	@Value("${storage.academy-media-bucket}")
	private String academyMediaBucket;

	@Value("${default.org.config.url}")
	private String defaultOrgConfigUrl;

	@Autowired
	public AcademyService(final AcademyRepo academyRepo, final AcademyLeadRepo academyLeadRepo,
			final IUserProfileService userProfileService, final IPushNotificationService notificationService,
			final IStorageService storageService, OrgRepo orgRepo, final AcademyConfigCache academyConfigCache) {
		this.academyRepo = academyRepo;
		this.academyLeadRepo = academyLeadRepo;
		this.userProfileService = userProfileService;
		this.notificationService = notificationService;
		this.storageService = storageService;
		this.orgRepo = orgRepo;
		this.academyConfigCache = academyConfigCache;
	}

	@Override
	public List<AcademyDto> getAcademiesByOrgId(String orgId) {
		List<Academy> academiesByOrgId = academyRepo.findByOrgId(orgId);
		if (academiesByOrgId.isEmpty()) {
			return new ArrayList<>();
		}
		return toDto(academiesByOrgId);
	}

	@Override
	public List<AcademyDto> getAcademiesByAppPackageName(String appPackageName) {
		List<Academy> academiesByAppPackageName = academyRepo.findByAppPackageName(appPackageName);

		if (academiesByAppPackageName.isEmpty()) {
			return new ArrayList<>();
		}
		return toDto(academiesByAppPackageName);
	}

	@Override
	public boolean addLead(AcademyLeadDto academyLeadDto) {
		AcademyLead academyLead = modelMapper.map(academyLeadDto, AcademyLead.class);
		academyLead.setId(UUID.randomUUID().toString());
		academyLead.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
		academyLeadRepo.save(academyLead);
		return true;
	}

	@Override
	public AcademyDto registerAcademy(AcademyDto academyDto) throws ResourceException {

		try {
			UserProfileDto userProfileDto = userProfileService.getUserProfileById(academyDto.getManagerUserId());
			userProfileDto.setRoleId(academyDto.getRoleId().longValue());
			if (userProfileDto.getUserType() != UserType.COACH) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST,
						"Manager User is not a Coach. ID: " + academyDto.getManagerUserId());
			}

			// Check if user already has other academies and get their organization ID
			List<Academy> existingAcademies = academyRepo
					.findByManagerUserIdAndInactiveFalse(academyDto.getManagerUserId());
			if (!existingAcademies.isEmpty()) {
				// Get distinct organization IDs from user's academies
				List<String> distinctOrgIds = existingAcademies.stream().filter(a -> a.getOrg() != null)
						.map(a -> a.getOrg().getId()).distinct().collect(Collectors.toList());

				// If there's at least one organization ID, use the first one
				if (!distinctOrgIds.isEmpty()) {
					String orgId = distinctOrgIds.get(0);
					log.info("Setting organization ID {} from user's existing academies for new academy", orgId);

					// Find the organization and set it in academyDto
					orgRepo.findById(orgId).ifPresent(org -> {
						academyDto.setOrgId(orgId);
					});
				}
			}
		} catch (ResourceException e) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
					"Manager User not found. ID: " + academyDto.getManagerUserId());
		}

		Academy academy = modelMapper.map(academyDto, Academy.class);
		academy.setId(UUID.randomUUID().toString());
		academy.setCreatedOn(Timestamp.from(Instant.now()));
		academy.setInactive(false);
		academy.setIconUrl(defaultIconUrlAcademy);
		if (StringUtils.isEmpty(academy.getInternalId())) {
			academy.setInternalId(generateRandomCode());
		}
		if (CollectionUtils.isEmpty(academy.getBranches())) {
			academy.setBranches(List.of(getDefaultBranch(academy.getId(), academyDto)));
		}
		if (!CollectionUtils.isEmpty(academyDto.getSports())) {
			academy.setAcademySportMappings(
					getAcademySportMappings(academy.getId(), academyDto.getSports(), new ArrayList<>()));
		}

		// If orgId is set, set the organization
		if (academyDto.getOrgId() != null) {
			orgRepo.findById(academyDto.getOrgId()).ifPresent(academy::setOrg);
		}

		return toDto(academyRepo.save(academy));
	}

	@Override
	public AcademyDto updateAcademy(String academyId, String userId, UpdateAcademyDto updateAcademyDto)
			throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		if (!academy.get().getManagerUserId().equalsIgnoreCase(userId)) {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST,
					"User is not authorized to update Academy. ID: " + academyId);
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getAddressLine1())) {
			academy.get().setAddressLine1(updateAcademyDto.getAddressLine1());
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getAddressLine2())) {
			academy.get().setAddressLine2(updateAcademyDto.getAddressLine2());
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getCity())) {
			academy.get().setCity(updateAcademyDto.getCity());
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getState())) {
			academy.get().setState(updateAcademyDto.getState());
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getCountry())) {
			academy.get().setCountry(updateAcademyDto.getCountry());
		}

		if (StringUtils.isNotEmpty(updateAcademyDto.getPincode())) {
			academy.get().setPincode(updateAcademyDto.getPincode());
		}

		return toDto(academyRepo.save(academy.get()));
	}

	@Override
	public String updateAcademyPicture(String academyId, String userId, FileObjectDto fileObjectDto)
			throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found");
		}
		if (!academy.get().getManagerUserId().equalsIgnoreCase(userId)) {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST, "User is not authorized to update Academy picture");
		}
		if (fileObjectDto != null) {
			String prefix = "academy-media/" + academyId + "/" + UUID.randomUUID() + "_"
					+ fileObjectDto.getOriginalFilename();
			storageService.upload(academyMediaBucket, prefix, fileObjectDto.getContent(),
					fileObjectDto.getContentType());
			academy.get().setIconUrl(academyMediaBaseUrl + prefix);
			academyRepo.save(academy.get());
			return academyMediaBaseUrl + prefix;
		}
		throw new ResourceException(ErrorCodes.INVALID_REQUEST, "FileObjectDto is null");
	}

	@Override
	public AcademyDto registerAcademy(CreateAcademyDto createAcademyDto, Boolean sendOtp) throws ResourceException {
		UserProfileDto userProfileDto;
		boolean isExistingUser = false;

		if (StringUtils.isEmpty(createAcademyDto.getOwnerUserId())) {
			if (StringUtils.isEmpty(createAcademyDto.getPhoneNumber())) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Phone number is required to create Academy");
			}
			if (StringUtils.isEmpty(createAcademyDto.getEmailId())) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Email Id is required to create Academy");
			}

			UserProfileDto createUserProfileDto = new UserProfileDto();
			createUserProfileDto.setGender(createAcademyDto.getOwnerGender());
			createUserProfileDto.setEmailId(createAcademyDto.getEmailId());
			createUserProfileDto.setPhoneNumber(createAcademyDto.getPhoneNumber());
			createUserProfileDto.setDisplayName(createAcademyDto.getOwnerName());
			createUserProfileDto.setUserType(UserType.COACH);
			createUserProfileDto.setRole(Role.ACADEMY_OWNER);
			createUserProfileDto.setRoleId(createAcademyDto.getRoleId().longValue());
			userProfileDto = userProfileService.create(createUserProfileDto, sendOtp);
		} else {
			userProfileDto = userProfileService.getUserProfileById(createAcademyDto.getOwnerUserId());
			isExistingUser = true;
		}

		// Check if user already has other academies and get their organization ID
		if (isExistingUser) {
			List<Academy> existingAcademies = academyRepo.findByManagerUserIdAndInactiveFalse(userProfileDto.getId());
			if (!existingAcademies.isEmpty()) {
				// Get distinct organization IDs from user's academies
				List<String> distinctOrgIds = existingAcademies.stream().filter(a -> a.getOrg() != null)
						.map(a -> a.getOrg().getId()).distinct().collect(Collectors.toList());

				// If there's at least one organization ID, use the first one
				if (!distinctOrgIds.isEmpty()) {
					String orgId = distinctOrgIds.get(0);
					log.info("Setting organization ID {} from user's existing academies for new academy", orgId);

					// Find the organization and set it for the new academy
					orgRepo.findById(orgId).ifPresent(org -> {
						createAcademyDto.setOrgId(orgId);
					});
				}
			}
		}

		Academy academy = modelMapper.map(createAcademyDto, Academy.class);
		academy.setEmailId(userProfileDto.getEmailId());
		academy.setPhoneNumber(userProfileDto.getPhoneNumber());
		academy.setId(UUID.randomUUID().toString());
		academy.setCreatedOn(Timestamp.from(Instant.now()));
		academy.setManagerUserId(userProfileDto.getId());
		academy.setInactive(false);
		academy.setIconUrl(defaultIconUrlAcademy);
		if (StringUtils.isEmpty(academy.getInternalId())) {
			academy.setInternalId(generateRandomCode());
		}
		if (CollectionUtils.isEmpty(academy.getBranches())) {
			academy.setBranches(List.of(getDefaultBranch(academy.getId(), createAcademyDto)));
		}
		if (!CollectionUtils.isEmpty(createAcademyDto.getSports())) {
			academy.setAcademySportMappings(
					getAcademySportMappings(academy.getId(), createAcademyDto.getSports(), new ArrayList<>()));
		}

		// If orgId is set, set the organization
		if (createAcademyDto.getOrgId() != null) {
			orgRepo.findById(createAcademyDto.getOrgId()).ifPresent(academy::setOrg);
		}

		return toDto(academyRepo.save(academy));
	}

	@Override
	@Transactional
	public AcademyDto getAcademyById(String academyId) throws ResourceException {
		Optional<Academy> academyOpt = academyRepo.findById(academyId);
		if (academyOpt.isEmpty() || academyOpt.get().isInactive()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		Academy academy = academyOpt.get();

		// Safely initialize organisation.configs inside the session
		Organisation organisation = academy.getOrg();
		if (organisation != null && organisation.getConfigs() != null) {
			organisation.getConfigs().size(); // Triggers the lazy load safely
		}

		return toDto(academy);
	}

	@Override
	public List<AcademyDto> getAcademyByIds(List<String> academyIds) {
		List<Academy> academies = academyRepo.findByIdInAndInactiveFalse(academyIds);
		if (CollectionUtils.isEmpty(academies)) {
			return new ArrayList<>();
		}
		return toDto(academies);
	}

	@Override
	public List<AcademyDto> getAcademyByManagerUserId(String managerUserId) {
		List<Academy> academies = academyRepo.findByManagerUserIdAndInactiveFalse(managerUserId);
		if (CollectionUtils.isEmpty(academies)) {
			return new ArrayList<>();
		}
		return toDto(academies);
	}

	@Override
	public AcademyDto updateAcademy(AcademyDto academyDto) {
		return toDto(academyRepo.save(modelMapper.map(academyDto, Academy.class)));
	}

	@Override
	public boolean deleteAcademy(String academyId) throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}
		academy.get().setInactive(true);
		academyRepo.save(academy.get());
		return false;
	}

	@Override
	public BranchDto addBranchToAcademy(String academyId, BranchDto branchDto) throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty() || academy.get().isInactive()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		if (CollectionUtils.isEmpty(academy.get().getBranches())) {
			academy.get().setBranches(new ArrayList<>());
		}

		Branch branch = modelMapper.map(branchDto, Branch.class);
		branch.setId(UUID.randomUUID().toString());
		branch.setCreatedOn(Timestamp.from(Instant.now()));

		academy.get().getBranches().add(branch);

		academyRepo.save(academy.get());

		return modelMapper.map(branch, BranchDto.class);
	}

	@Override
	public BranchDto updateBranchOfAcademy(String academyId, String branchId, BranchDto branchDto)
			throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty() || academy.get().isInactive()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		if (CollectionUtils.isEmpty(academy.get().getBranches())
				|| academy.get().getBranches().stream().noneMatch(branch -> branch.getId().equals(branchId))) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Branch not found. ID: " + branchId);
		}

		Branch branch = modelMapper.map(branchDto, Branch.class);
		branch.setId(branchId);
		branch.setUpdatedOn(Timestamp.from(Instant.now()));

		List<Branch> branchSet = academy.get().getBranches().stream().filter(b -> !b.getId().equalsIgnoreCase(branchId))
				.collect(Collectors.toList());
		branchSet.add(branch);
		academy.get().setBranches(branchSet);

		academyRepo.save(academy.get());

		return modelMapper.map(branch, BranchDto.class);
	}

	@Override
	public boolean deleteBranchFromAcademy(String academyId, String branchId) throws ResourceException {
		Optional<Academy> academy = academyRepo.findById(academyId);
		if (academy.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		if (CollectionUtils.isEmpty(academy.get().getBranches())
				|| academy.get().getBranches().stream().noneMatch(branch -> branch.getId().equals(branchId))) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Branch not found. ID: " + branchId);
		}

		academy.get().getBranches().forEach(branch -> {
			if (branch.getId().equalsIgnoreCase(branchId)) {
				branch.setInactive(true);
			}
		});

		academyRepo.save(academy.get());

		return true;
	}

	private Branch getDefaultBranch(String academyId, AcademyDto academyDto) {
		Branch branch = new Branch();
		branch.setId(UUID.randomUUID().toString());
		branch.setAcademy(Academy.builder().id(academyId).build());
		branch.setName(academyDto.getName() + " HQ");
		branch.setInactive(false);
		branch.setAddressLine1(academyDto.getAddressLine1());
		branch.setAddressLine2(academyDto.getAddressLine2());
		branch.setCity(academyDto.getCity());
		branch.setState(academyDto.getState());
		branch.setCountry(academyDto.getCountry());
		branch.setPincode(academyDto.getPincode());
		branch.setPhoneNumber(academyDto.getPhoneNumber());
		branch.setCreatedOn(Timestamp.from(Instant.now()));
		return branch;
	}

	private Branch getDefaultBranch(String academyId, CreateAcademyDto academyDto) {
		Branch branch = new Branch();
		branch.setAcademy(Academy.builder().id(academyId).build());
		branch.setId(UUID.randomUUID().toString());
		branch.setName(academyDto.getName() + " HQ");
		branch.setInactive(false);
		branch.setAddressLine1(academyDto.getAddressLine1());
		branch.setAddressLine2(academyDto.getAddressLine2());
		branch.setCity(academyDto.getCity());
		branch.setState(academyDto.getState());
		branch.setCountry(academyDto.getCountry());
		branch.setPincode(academyDto.getPincode());
		branch.setPhoneNumber(academyDto.getPhoneNumber());
		branch.setCreatedOn(Timestamp.from(Instant.now()));
		return branch;
	}

	private static String generateRandomCode() {
		StringBuilder code = new StringBuilder(5);
		for (int i = 0; i < 5; i++) {
			code.append(CHARACTERS.charAt(ThreadLocalRandom.current().nextInt(CHARACTERS.length())));
		}
		return code.toString();
	}

	private List<AcademySportMapping> getAcademySportMappings(String academyId, List<Sports> sports,
			List<AcademySportMapping> existingSports) {
		Map<Sports, AcademySportMapping> sportsAcademySportMappingMap = existingSports.stream()
				.collect(Collectors.toMap(AcademySportMapping::getSport, s -> s));
		return sports.stream().map(sport -> {
			if (sportsAcademySportMappingMap.containsKey(sport)) {
				return sportsAcademySportMappingMap.get(sport);
			}
			AcademySportMapping academySportMapping = new AcademySportMapping();
			academySportMapping.setAcademy(Academy.builder().id(academyId).build());
			academySportMapping.setSport(sport);
			academySportMapping.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
			return academySportMapping;
		}).collect(Collectors.toList());
	}

	private List<AcademyDto> toDto(List<Academy> academies) {
		return academies.stream().map(academy -> {
			AcademyDto academyDto = new AcademyDto();
			academyDto.setId(academy.getId());
			academyDto.setName(academy.getName());
			academyDto.setManagerUserId(academy.getManagerUserId());
			academyDto.setInternalId(academy.getInternalId());
			academyDto.setEmailId(academy.getEmailId());
			academyDto.setPhoneNumber(academy.getPhoneNumber());
			academyDto.setHeadquarter(academy.getHeadquarter());
			academyDto.setAddressLine1(academy.getAddressLine1());
			academyDto.setAddressLine2(academy.getAddressLine2());
			academyDto.setPincode(academy.getPincode());
			academyDto.setCity(academy.getCity());
			academyDto.setState(academy.getState());
			academyDto.setCountry(academy.getCountry());
			academyDto.setStartTime(academy.getStartTime());
			academyDto.setEndTime(academy.getEndTime());
			academyDto.setIconUrl(academy.getIconUrl());
			academyDto.setOrgId(academy.getOrg() != null ? academy.getOrg().getId() : null);

			// sets org config or default config
			academyConfigCache.setOrgConfigOrDefault(academyDto, academy);

			if (!CollectionUtils.isEmpty(academy.getAcademySportMappings())) {
				academyDto.setSports(academy.getAcademySportMappings().stream().map(AcademySportMapping::getSport)
						.collect(Collectors.toList()));
			}

			if (academy.getGeoFences() != null && !academy.getGeoFences().isEmpty()) {
				academyDto.setGeoFences(academy.getGeoFences().stream()
						.filter(geoFence -> geoFence != null && !geoFence.getDeleted()).map(geoFence -> {
							GeoFenceDto.GeoFenceDtoBuilder builder = GeoFenceDto.builder()
									.geoFenceId(geoFence.getGeoFenceId()).active(!geoFence.getDeleted());

							// Safely set nullable fields
							if (geoFence.getName() != null) {
								builder.name(geoFence.getName());
							}
							if (geoFence.getLatitude() != null) {
								builder.latitude(geoFence.getLatitude());
							}
							if (geoFence.getLongitude() != null) {
								builder.longitude(geoFence.getLongitude());
							}
							if (geoFence.getRadiusInMeters() != null) {
								builder.radius(geoFence.getRadiusInMeters());
							}

							return builder.build();
						}).filter(Objects::nonNull) // Filter out any null DTOs that might have been created
						.collect(Collectors.toList()));
			}

			if (!CollectionUtils.isEmpty(academy.getBranches())) {
				academyDto.setBranches(academy.getBranches().stream().map(branch -> {
					BranchDto branchDto = new BranchDto();
					branchDto.setId(branch.getId());
					branchDto.setName(branch.getName());
					branchDto.setAddressLine1(branch.getAddressLine1());
					branchDto.setAddressLine2(branch.getAddressLine2());
					branchDto.setCity(branch.getCity());
					branchDto.setState(branch.getState());
					branchDto.setCountry(branch.getCountry());
					branchDto.setPincode(branch.getPincode());
					branchDto.setPhoneNumber(branch.getPhoneNumber());
					branchDto.setCourts(0);
					branchDto.setNets(0);
					branchDto.setGym(false);
					branchDto.setTurf(0);
					branchDto.setPlayArea(0);
					branchDto.setOthers(false);
					return branchDto;
				}).collect(Collectors.toList()));
			}
			return academyDto;
		}).collect(Collectors.toList());
	}

	private AcademyDto toDto(Academy academy) {
		return toDto(List.of(academy)).get(0);
	}

	/**
	 * Retrieves the organization configuration based on the request's
	 * {@code Origin} header.
	 * <p>
	 * If an organization matching the domain in the {@code Origin} header is found,
	 * its configuration is merged with the default configuration loaded from S3.
	 * Organization-specific values override the defaults.
	 * <p>
	 * If the {@code Origin} header is missing or empty, or if the organization is
	 * not found, the default configuration from S3 is returned.
	 *
	 * @param request the {@link HttpServletRequest} containing the {@code Origin}
	 *                header
	 * @return a {@link Map} of configuration key-value pairs for the identified
	 *         organization, or default configuration if the organization is not
	 *         found or origin is missing
	 * @throws ResourceException if the organization is not found for the provided
	 *                           origin
	 */
	@Override
	public Map<String, String> getOrgConfigByDomainUrl(HttpServletRequest request) throws ResourceException {
		String origin = request.getHeader("origin");
		log.info("Getting org config for Origin: {}", origin);

		if (!org.springframework.util.StringUtils.hasText(origin)) {
			log.warn("Origin header is missing or empty in the request. Loading default config from S3");
			return academyConfigCache.getDefaultConfig();
		}

		try {
			return orgRepo.findByDomainUrlIgnoreCase(origin).map(org -> {
				Map<String, String> defaultConfig = academyConfigCache.getDefaultConfig();

				if (org.getConfigs() != null && !org.getConfigs().isEmpty()) {
					Map<String, String> orgConfig = org.getConfigs().stream()
							.collect(Collectors.toMap(OrganisationConfig::getKey, OrganisationConfig::getValue));

					log.info("Found org config for domain {}: {}", origin, orgConfig);
					defaultConfig.putAll(orgConfig); // Override defaults with org-specific configs
				}
				return defaultConfig;
			}).orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Organization not found"));
		} catch (ResourceException e) {
			log.error("Error extracting domain from Origin: {}", origin, e);
			return academyConfigCache.getDefaultConfig();
		}

	}
}
