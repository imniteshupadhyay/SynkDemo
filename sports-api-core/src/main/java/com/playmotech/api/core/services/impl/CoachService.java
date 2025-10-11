package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.gson.reflect.TypeToken;
import com.itextpdf.io.exceptions.IOException;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.AttributeType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.CoachPerformanceReport;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.AddCoachToAcademyRequestDto;
import com.playmotech.api.core.dto.Attribute;
import com.playmotech.api.core.dto.Category;
import com.playmotech.api.core.dto.CoachDetailsDto;
import com.playmotech.api.core.dto.CoachPerformanceReportDto;
import com.playmotech.api.core.dto.CourseMinDto;
import com.playmotech.api.core.dto.SubmitCoachPerformanceReportRequestDto;
import com.playmotech.api.core.dto.ToggleCoachStatusDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CoachPerformanceRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.CoachDetails;
import com.playmotech.api.core.response.dao.CoachExportDetails;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CoachService implements ICoachService {

	private final IUserProfileService userProfileService;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final RolesRepo rolesRepo;
	private final IAcademyService academyService;
	private final ModelMapper modelMapper = new ModelMapper();
	private final CoachPerformanceRepo coachPerformanceRepo;
	private final CourseRepo courseRepo;
	private final AcademyDomainUtil academyDomainUtil;

	public CoachService(IUserProfileService userProfileService, CoachAcademyMappingRepo coachAcademyMappingRepo,
			IAcademyService academyService, CoachPerformanceRepo coachPerformanceRepo, RolesRepo rolesRepo,
			CourseRepo courseRepo, AcademyDomainUtil academyDomainUtil) {
		this.userProfileService = userProfileService;
		this.coachAcademyMappingRepo = coachAcademyMappingRepo;
		this.academyService = academyService;
		this.coachPerformanceRepo = coachPerformanceRepo;
		this.rolesRepo = rolesRepo;
		this.courseRepo = courseRepo;
		this.academyDomainUtil = academyDomainUtil;
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
	}

	private List<CoachDetails> mapResultsToCoachDetails(List<Object[]> results) {
		List<CoachDetails> coachDetailsList = new ArrayList<>();

		for (Object[] row : results) {
			CoachDetails coach = new CoachDetails();

			coach.setId(getStringValue(row[0])); // coach_id
			coach.setName(getStringValue(row[1])); // name
			coach.setEmail(getStringValue(row[2])); // email_id
			coach.setUsername(getStringValue(row[3])); // username
			coach.setAge(row[4] != null ? ((Number) row[4]).intValue() : null); // age
			coach.setDesignation(getStringValue(row[5])); // designation
			coach.setAcademyNames(getStringValue(row[6])); // academy_names
			coach.setRole(getStringValue(row[7])); // role
			coach.setQualification(getStringValue(row[8])); // qualification
			coach.setGender(getStringValue(row[9])); // gender
			coach.setDob(row[10] != null ? ((java.sql.Date) row[10]).toLocalDate() : null); // dob
			coach.setAadharCard(getStringValue(row[11])); // aadhar_card
			coach.setPanCard(getStringValue(row[12])); // pan_card

			// Handle text[] arrays
			List<String> programs = new ArrayList<>();
			if (row[13] != null) {
				String[] progArray = (String[]) row[13];
				programs = Arrays.asList(progArray);
			}

			List<String> sports = new ArrayList<>();
			if (row[14] != null) {
				String[] sportArray = (String[]) row[14];
				sports = Arrays.asList(sportArray);
			}

			coach.setAssociatedPrograms(programs); // associated_programs
			coach.setSports(sports); // sports

			coach.setAcademyIds(getStringValue(row[15])); // academy_ids
			coach.setStatus(getStringValue(row[16])); // status
			coach.setAcademyDesignations(getStringValue(row[17])); // academy_designations
			coach.setAcademyJson(getStringValue(row[18])); // academies_json

			// Add created_on field mapping
			coach.setCreatedOn(row[19] != null ? ((java.sql.Timestamp) row[19]).toLocalDateTime() : null); // created_on

			coachDetailsList.add(coach);
		}

		return coachDetailsList;
	}

	@Override
	public ServiceResponse getList(String userId, List<String> academyIds, List<String> courseIds, List<String> sports,
			String academyDomain, boolean export) {
		try {
			log.info("Starting to fetch coach details list");

			String userRole = academyDomainUtil.getCurrentUserRoleName(academyDomain);

			// Convert lists to comma-separated strings or NULL
			String formattedAcademyIds = listToCommaSeparatedString(academyIds);
			String formattedCourseIds = listToCommaSeparatedString(courseIds);
			String formattedSports = listToCommaSeparatedString(sports);

			// Fetch results from repository
			List<Object[]> results = courseRepo.getCoachDetails(userId, formattedAcademyIds, formattedCourseIds,
					formattedSports, userRole, academyDomain);

			if (results.isEmpty()) {
				log.info("No coaches found matching the criteria");
				return ResponseBuilder.success(ApiResponse.NO_RECORD_FOUND);
			}

			if (export) {
				try {
					List<CoachExportDetails> coachDetailsList = mapResultsToCoachExportDetails(results);
					byte[] excelBytes = ExcelGenerator.generateExcel(coachDetailsList, null);
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", HttpStatus.OK);
					responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
					responseMap.put("fileName", "coach_data.xlsx");
					responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

					return ResponseBuilder.success(responseMap, ApiResponse.EXPORT_SUCCESS, HttpStatus.OK);
				} catch (IOException e) {
					log.error("Error exporting attendance data to Excel", e);
					return ResponseBuilder.internalServerError(ApiResponse.ERROR_EXPORTING_DATA);
				}
			} else {
				// Map results to CoachDetails objects
				List<CoachDetails> coachDetailsList = mapResultsToCoachDetails(results);

				return ResponseBuilder.success(coachDetailsList, ApiResponse.LIST_FETCHED_SUCCESSFULLY);
			}

		} catch (Exception e) {
			log.error(
					"Exception occurred while fetching coach details. UserId: {}, AcademyIds: {}, CourseIds: {}, Sports: {}",
					userId, academyIds, courseIds, sports, e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Override
	public void addCoachToAcademy(List<AddCoachToAcademyRequestDto> coaches, String academyId)
			throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		if (academyDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		List<String> existingCoachUserIdInAcademy = coachAcademyMappingRepo
				.findByAcademy_IdAndCoachUserProfile_IdIn(academyId,
						coaches.stream().map(AddCoachToAcademyRequestDto::getCoachUserId).collect(Collectors.toList()))
				.stream().map(coachAcademyMapping -> coachAcademyMapping.getCoachUserProfile().getId()).toList();

		List<CoachAcademyMapping> coachAcademyMappings = new ArrayList<>();
		for (AddCoachToAcademyRequestDto addCoachToAcademyRequestDto : coaches) {
			if (existingCoachUserIdInAcademy.contains(addCoachToAcademyRequestDto.getCoachUserId())) {
				continue;
			}
			CoachAcademyMapping coachAcademyMapping = new CoachAcademyMapping();
			coachAcademyMapping.setId(UUID.randomUUID().toString());
			coachAcademyMapping.setAcademy(Academy.builder().id(academyId).build());
			coachAcademyMapping.setCoachUserProfile(
					UserProfile.builder().id(addCoachToAcademyRequestDto.getCoachUserId()).build());
			coachAcademyMapping.setDesignation(addCoachToAcademyRequestDto.getDesignation());
			coachAcademyMapping.setExperienceInMonths(addCoachToAcademyRequestDto.getExperienceInMonths());
			coachAcademyMapping.setCreatedOn(Timestamp.from(Instant.now()));
			coachAcademyMapping.setLastStatusUpdateEpoch(Timestamp.from(Instant.now()));
			coachAcademyMapping.setStatus(Status.ACTIVE);
			coachAcademyMapping.setRoleId(addCoachToAcademyRequestDto.getRoleId().longValue());
			coachAcademyMappings.add(coachAcademyMapping);
		}

		coachAcademyMappingRepo.saveAll(coachAcademyMappings);
	}

	@Override
	public void updateCoachInAcademy(List<AddCoachToAcademyRequestDto> coaches, String academyId)
			throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		if (academyDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}

		List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo
				.findByAcademy_IdAndCoachUserProfile_IdIn(academyId,
						coaches.stream().map(AddCoachToAcademyRequestDto::getCoachUserId).collect(Collectors.toList()));
		if (CollectionUtils.isEmpty(coachAcademyMappings)) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
					"Coaches not found in academy. Academy ID: " + academyId);
		}

		for (CoachAcademyMapping coachAcademyMapping : coachAcademyMappings) {
			Optional<AddCoachToAcademyRequestDto> coachOptional = coaches.stream().filter(
					coach -> coach.getCoachUserId().equalsIgnoreCase(coachAcademyMapping.getCoachUserProfile().getId()))
					.findFirst();
			if (coachOptional.isPresent()) {
				AddCoachToAcademyRequestDto coach = coachOptional.get();
				coachAcademyMapping.setDesignation(coach.getDesignation());
				coachAcademyMapping.setExperienceInMonths(coach.getExperienceInMonths());
				coachAcademyMapping.setLastStatusUpdateEpoch(Timestamp.from(Instant.now()));
				coachAcademyMapping.setRoleId(coach.getRoleId().longValue());
			}
		}
		coachAcademyMappingRepo.saveAll(coachAcademyMappings);
	}

	@Transactional
	@Override
	public List<CoachDetailsDto> getCoachesByAcademyIdAndNameAndPhoneNumber(String academyId, String name,
			String phoneNumber, String searchTxt, boolean coach) throws ResourceException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		AcademyDto academyDto = academyService.getAcademyById(academyId);
		if (academyDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}
		List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo.findByAcademy_Id(academyId);
		// if coach is true, then filter out only the coaches, else map all users
		if (coach) {
			Roles role = rolesRepo.findByRoleNameContainingIgnoreCase("coach").get(0);
			// coachAcademyMappings = coachAcademyMappings.stream()
			// .filter(each ->
			// each.getCoachUserProfile().getRbacRoles().getId().equals(role.getId()))
			// .toList();
			// Using role id from coach academy mapping
			coachAcademyMappings = coachAcademyMappings.stream().filter(each -> each.getRoleId().equals(role.getId()))
					.toList();
		}
		List<String> coachUserIds = coachAcademyMappings.stream()
				.map(coachAcademyMapping -> coachAcademyMapping.getCoachUserProfile().getId()).toList();
		if (coachUserIds.isEmpty()) {
			return new ArrayList<>();
		}

		/**
		 * Removing current user from the list of coaches, handle on client side
		 */
		List<CoachDetailsDto> coachDetailDtos = coachAcademyMappings.stream().map(coachAcademyMapping -> {
			CoachDetailsDto coachDetailsDto = modelMapper.map(coachAcademyMapping, CoachDetailsDto.class);
			coachDetailsDto.setCoachUserId(coachAcademyMapping.getCoachUserProfile().getId());
			return coachDetailsDto;
		}).toList();

		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIdsAndNameAndPhoneNumber(coachUserIds,
				name, phoneNumber, searchTxt);
		Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
				.collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));
		coachDetailDtos = coachDetailDtos.stream()
				.filter(coachDetailsDto -> userProfileDtoMap.containsKey(coachDetailsDto.getCoachUserId()))
				.collect(Collectors.toList());
		coachDetailDtos.forEach(
				coachDetail -> coachDetail.setUserProfile(userProfileDtoMap.get(coachDetail.getCoachUserId())));
		return sort(coachDetailDtos);
	}

	@Override
	public void removeCoachesFromAcademy(List<String> coaches, String academyId) throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		if (academyDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}
		List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo
				.findByAcademy_IdAndCoachUserProfile_IdIn(academyId, coaches);
		coachAcademyMappingRepo.deleteAll(coachAcademyMappings);
	}

	@Override
	public void toggleCoachesStatusInAcademy(String academyId, ToggleCoachStatusDto toggleCoachStatusDto)
			throws ResourceException {
		AcademyDto academyDto = academyService.getAcademyById(academyId);
		if (academyDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found. ID: " + academyId);
		}
		List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo
				.findByAcademy_IdAndCoachUserProfile_IdIn(academyId, toggleCoachStatusDto.getCoachUserIds());
		for (CoachAcademyMapping coachAcademyMapping : coachAcademyMappings) {
			coachAcademyMapping.setStatus(toggleCoachStatusDto.getStatus());
			coachAcademyMapping.setLastStatusUpdateEpoch(Timestamp.from(Instant.now()));
		}
		coachAcademyMappingRepo.saveAll(coachAcademyMappings);
	}

	@Override
	public List<AcademyDto> getAcademiesByCoachUserId(String coachUserId) throws ResourceException {
		List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo.findByCoachUserProfile_Id(coachUserId);
		List<String> academyIds = coachAcademyMappings.stream()
				.map(coachAcademyMapping -> coachAcademyMapping.getAcademy().getId()).toList();
		List<AcademyDto> managedAcademy = academyService.getAcademyByManagerUserId(coachUserId);

		List<AcademyDto> academyDtos = CollectionUtils.isEmpty(academyIds) ? new ArrayList<>()
				: academyService.getAcademyByIds(academyIds);
		if (CollectionUtils.isEmpty(academyDtos)) {
			academyDtos.addAll(managedAcademy);
		}
		return academyDtos;
	}

	@Override
	public CoachPerformanceReportDto submitCoachPerformanceReport(String coachUserId, String traineeUserId,
			SubmitCoachPerformanceReportRequestDto coachPerformanceReportRequestDto) throws ResourceException {
		if (!StringUtils.isEmpty(coachPerformanceReportRequestDto.getAcademyId())) {
			academyService.getAcademyById(coachPerformanceReportRequestDto.getAcademyId());
		}
		String perfReportId = UUID.randomUUID().toString();
		CoachPerformanceReport coachPerformanceReport = modelMapper.map(coachPerformanceReportRequestDto,
				CoachPerformanceReport.class);
		coachPerformanceReport.setId(perfReportId);
		coachPerformanceReport.setTraineeUserProfile(UserProfile.builder().id(traineeUserId).build());
		coachPerformanceReport.setTitle(coachPerformanceReportRequestDto.getTitle());
		coachPerformanceReport.setCoachUserProfile(UserProfile.builder().id(coachUserId).build());
		coachPerformanceReport.setAcademy(StringUtils.isEmpty(coachPerformanceReportRequestDto.getAcademyId()) ? null
				: Academy.builder().id(coachPerformanceReportRequestDto.getAcademyId()).build());
		coachPerformanceReport.setCourse(StringUtils.isEmpty(coachPerformanceReportRequestDto.getCourseId()) ? null
				: Course.builder().id(coachPerformanceReportRequestDto.getCourseId()).build());
		coachPerformanceReport.setCreatedOn(Timestamp.from(Instant.now()));
		coachPerformanceReport.setReportJson(calculateCategoryRating(coachPerformanceReportRequestDto.getReport()));
		// if (!CollectionUtils.isEmpty(fileObjectDtos)) {
		// List<String> mediaUrlPaths = new ArrayList<>();
		// for (FileObjectDto fileObjectDto: fileObjectDtos) {
		// String prefix = "trainee-performance/" + traineeUserId + "/" + coachUserId +
		// "/" + perfReportId + "_" + fileObjectDto.getOriginalFilename();
		// storageService.upload(performanceMediaBucket, prefix,
		// fileObjectDto.getContent(), fileObjectDto.getContentType());
		// mediaUrlPaths.add(prefix);
		// }
		// traineePerformance.setMediaMappings(buildTraineePerfReportMappings(perfReportId,
		// mediaUrlPaths));
		// }

		coachPerformanceRepo.save(coachPerformanceReport);

		// CompletableFuture.runAsync(() -> {
		// if (traineePerformance.getStatus() == PerformanceReportStatus.SUBMITTED) {
		// try {
		// Map<String, String> extraArg = new HashMap<>();
		// extraArg.put("perfReportId", traineePerformance.getId());
		// extraArg.put("courseId", traineePerformanceDto.getCourseId());
		// UserProfileDto userProfileDto =
		// userProfileService.getUserProfileById(traineeUserId);
		// log.info("Sending performance report notification to user: {}",
		// userProfileDto.getDisplayName());
		// notificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
		// NotificationType.LIVE_NOTIFICATION, PERF_REPORT_SUBMITTED_TITLE,
		// PERF_REPORT_SUBMITTED_BODY, "LIST_TRAINEE_PERF_REPORT", CtaType.SCREEN,
		// extraArg);
		// notificationService.addNotification(List.of(userProfileDto.getId()), "Your
		// coach has submitted a performance report. Click to review it.",
		// CtaType.SCREEN, "LIST_TRAINEE_PERF_REPORT", extraArg);
		// } catch (ResourceException e) {
		// throw new RuntimeException(e);
		// }
		// }
		// });

		return getTraineePerformanceById(perfReportId);
	}

	private String calculateCategoryRating(String reportString) {
		// Deserialize the JSON array to List<Category>
		List<Category> categories = AppConstants.GSON.fromJson(reportString,
				TypeToken.getParameterized(List.class, Category.class).getType());

		for (Category category : categories) {
			// Filter only rating attributes (1–5 and 1-10)
			List<Attribute> ratingAttributes = category.getAttributes().stream()
					.filter(attribute -> attribute.getType() == AttributeType.RATING_1_5
							|| attribute.getType() == AttributeType.RATING_10)
					.toList();

			// Compute average rating
			double averageRating = ratingAttributes.stream()
					.mapToDouble(a -> {
						try {
							return Double.parseDouble(a.getValue());
						} catch (NumberFormatException e) {
							return 0.0; // Handle empty or invalid values
						}
					})
					.average()
					.orElse(0.0);

			// Set rounded-up average as category rating (as String)
			category.setCategoryRating(String.valueOf((int) Math.ceil(averageRating)));
		}

		// Skip categories only if ALL attributes have empty/null values
		List<Category> filteredCategories = categories.stream()
				.filter(c -> c.getAttributes() != null &&
						c.getAttributes().stream()
								.anyMatch(attr -> attr.getValue() != null && !attr.getValue().trim().isEmpty()))
				.toList();

		// Serialize the updated list back to JSON
		return AppConstants.GSON.toJson(filteredCategories);
	}

	private CoachPerformanceReportDto getTraineePerformanceById(String id) throws ResourceException {
		Optional<CoachPerformanceReport> coachPerformanceReport = coachPerformanceRepo.findById(id);
		if (coachPerformanceReport.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Trainee performance not found. ID: " + id);
		}

		CoachPerformanceReportDto coachPerformanceReportDto = modelMapper.map(coachPerformanceReport.get(),
				CoachPerformanceReportDto.class);
		if (coachPerformanceReport.get().getCourse() != null) {
			// CourseDto courseDto =
			// courseService.getCourse(traineePerformance.get().getAcademyId(),
			// traineePerformance.get().getCourseId());
			coachPerformanceReportDto
					.setCourse(modelMapper.map(coachPerformanceReport.get().getCourse(), CourseMinDto.class));
		}
		// UserProfileDto traineeUserProfile =
		// userProfileService.getUserProfileById(traineePerformance.get().getTraineeUserId());
		// UserProfileDto coachUserProfile =
		// userProfileService.getUserProfileById(traineePerformance.get().getCoachUserId());
		coachPerformanceReportDto.setReport(coachPerformanceReport.get().getReportJson());
		coachPerformanceReportDto.setTrainee(
				modelMapper.map(coachPerformanceReport.get().getTraineeUserProfile(), UserProfileMinDto.class));
		coachPerformanceReportDto
				.setCoach(modelMapper.map(coachPerformanceReport.get().getCoachUserProfile(), UserProfileMinDto.class));
		if (coachPerformanceReport.get().getAcademy() != null) {
			coachPerformanceReportDto
					.setAcademy(modelMapper.map(coachPerformanceReport.get().getAcademy(), AcademyMinDto.class));
		}

		return coachPerformanceReportDto;
	}

	private List<CoachDetailsDto> sort(List<CoachDetailsDto> coachDetailsDtos) {
		return coachDetailsDtos.stream()
				.sorted(Comparator.comparing(coachDetailsDto -> coachDetailsDto.getUserProfile().getDisplayName()))
				.toList();
	}

	private List<CoachExportDetails> mapResultsToCoachExportDetails(List<Object[]> results) {
		List<CoachExportDetails> coachDetailsList = new ArrayList<>();

		for (Object[] row : results) {
			CoachExportDetails coach = new CoachExportDetails();

			coach.setName(getStringValue(row[1])); // name
			coach.setEmail(getStringValue(row[2])); // email_id
			coach.setPhoneNumber(getStringValue(row[3])); // username
			coach.setAge(row[4] != null ? (Integer) row[4] : null); // age

			// Designation information
			coach.setDesignation(getStringValue(row[5])); // designation
			coach.setAcademyNames(getStringValue(row[6])); // academy_names
			coach.setAcademyDesignations(getStringValue(row[17])); // academy_designations (combined)

			// Other fields
			coach.setRole(getStringValue(row[7])); // role
			coach.setQualification(getStringValue(row[8])); // qualification
			coach.setGender(getStringValue(row[9])); // gender
			coach.setDob(row[10] != null ? ((java.sql.Date) row[10]).toLocalDate() : null); // dob
			coach.setAadharCard(getStringValue(row[11])); // aadhar_card
			coach.setPanCard(getStringValue(row[12])); // pan_card

			// Handle text[] arrays
			List<String> programs = new ArrayList<>();
			if (row[13] != null) {
				String[] progArray = (String[]) row[13];
				programs = Arrays.asList(progArray);
			}

			List<String> sports = new ArrayList<>();
			if (row[14] != null) {
				String[] sportArray = (String[]) row[14];
				sports = Arrays.asList(sportArray);
			}

			coach.setAssociatedPrograms(programs); // associated_programs
			coach.setSports(sports); // sports
			coach.setStatus(getStringValue(row[16])); // status

			coachDetailsList.add(coach);
		}

		return coachDetailsList;
	}

	private String getStringValue(Object value) {
		return value != null ? value.toString() : null;
	}

	private String listToCommaSeparatedString(List<String> list) {
		return list == null || list.isEmpty() ? null : String.join(",", list);
	}
}
