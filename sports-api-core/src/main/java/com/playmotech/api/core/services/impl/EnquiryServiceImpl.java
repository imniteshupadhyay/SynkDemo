package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.EnquiryStatus;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.Enquiry;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CourseMinDto;
import com.playmotech.api.core.dto.EnquiryDto;
import com.playmotech.api.core.dto.EnquiryRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.EnquiryRepo;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IEnquiryService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IUserProfileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EnquiryServiceImpl implements IEnquiryService {

	private final EnquiryRepo enquiryRepo;
	private final IUserProfileService userProfileService;
	private final ModelMapper modelMapper;
	private final ICourseService courseService;
	private final IPushNotificationService notificationService;
	private static final String CTA = "ENQUIRY_LIST";

	@Autowired
	public EnquiryServiceImpl(EnquiryRepo enquiryRepo, IUserProfileService userProfileService, ModelMapper modelMapper,
			ICourseService courseService, IPushNotificationService notificationService) {
		this.enquiryRepo = enquiryRepo;
		this.userProfileService = userProfileService;
		this.modelMapper = modelMapper;
		this.courseService = courseService;
		this.notificationService = notificationService;
	}

	@Override
	public EnquiryDto createEnquiry(EnquiryRequestDto enquiryRequestDto, UserDetail userDetail)
			throws ResourceException {
		Enquiry enquiry = new Enquiry();
		enquiry.setId(UUID.randomUUID().toString());
		enquiry.setCreatedOn(Timestamp.from(Instant.now()));
		enquiry.setUpdatedOn(Timestamp.from(Instant.now()));
		enquiry.setStatus(EnquiryStatus.OPEN); // Default status
		enquiry.setUserProfile(UserProfile.builder().id(userDetail.getUserId()).build());
		enquiry.setDescription(enquiryRequestDto.getDescription());
		enquiry.setCourse(Course.builder().id(enquiryRequestDto.getCourseId()).build());
		enquiry.setAcademy(Academy.builder().id(enquiryRequestDto.getAcademyId()).build());
		enquiry.setNotes(enquiryRequestDto.getNotes());
		// Save enquiry
		Enquiry savedEnquiry = enquiryRepo.save(enquiry);

		CompletableFuture.runAsync(() -> {
			try {
				// Send notification to academy
				// notificationService.sendNotificationToAcademy(savedEnquiry);
				CourseDto courseDto = courseService.getCourse(savedEnquiry.getAcademy().getId(),
						savedEnquiry.getCourse().getId());
				List<String> coachIds = new ArrayList<>(
						CollectionUtils.isEmpty(courseDto.getCoaches()) ? new ArrayList<>()
								: courseDto.getCoaches().stream().map(UserProfileMinDto::getId).toList());
				if (!StringUtils.isEmpty(courseDto.getCoachUserId())
						&& !coachIds.contains(courseDto.getCoachUserId())) {
					coachIds.add(courseDto.getCoachUserId());
				}
				if (CollectionUtils.isEmpty(coachIds)) {
					return;
				}

				List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(coachIds);
				for (UserProfileDto userProfileDto : userProfileDtos) {
					if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
						continue;
					}
					log.info("Sending notification to coach: {}", userProfileDto.getDisplayName());
					notificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
							NotificationType.LIVE_NOTIFICATION, "Program Inquiry",
							"New enquiry received for " + courseDto.getTitle(), CTA, CtaType.SCREEN,
							Map.of("academyId", courseDto.getAcademyId()));
				}
				notificationService.addNotification(coachIds, "New enquiry received for " + courseDto.getTitle(),
						CtaType.SCREEN, CTA, Map.of("academyId", courseDto.getAcademyId()));

			} catch (Exception e) {
				log.error("Error while sending notification to academy for new program enquiry.", e);
			}
		});

		return toEnquiryDtos(enquiry.getAcademy().getId(), List.of(savedEnquiry)).get(0);
	}

	@Override
	public EnquiryDto updateEnquiry(String academyId, String enquiryId, EnquiryRequestDto enquiryRequestDto,
			UserDetail userDetail) throws ResourceException {
		Enquiry enquiry = enquiryRepo.findById(enquiryId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "ENQUIRY_NOT_FOUND"));

		enquiry.setUpdatedOn(Timestamp.from(Instant.now()));
		enquiry.setNotes(enquiryRequestDto.getNotes());
		enquiry.setDescription(enquiryRequestDto.getDescription());
		enquiry.setStatus(
				enquiryRequestDto.getStatus() != null ? EnquiryStatus.valueOf(enquiryRequestDto.getStatus().toString())
						: enquiry.getStatus());

		// Save updated enquiry
		Enquiry updatedEnquiry = enquiryRepo.save(enquiry);
		return toEnquiryDtos(academyId, List.of(updatedEnquiry)).get(0);
	}

	@Override
	public List<EnquiryDto> getAllEnquiriesForUser(String academyId, String userId, String status)
			throws ResourceException {
		// Fetch user profile (if needed for further validation)
		UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);

		if (userProfileDto.getUserType() == UserType.PLAYER) {
			List<Enquiry> enquiries;
			if ("OPEN".equalsIgnoreCase(status)) {
				enquiries = enquiryRepo.findByUserProfile_IdAndStatus(userId, EnquiryStatus.OPEN);
			} else if ("CLOSED".equalsIgnoreCase(status)) {
				enquiries = enquiryRepo.findByUserProfile_IdAndStatus(userId, EnquiryStatus.CLOSED);
			} else {
				enquiries = enquiryRepo.findByUserProfile_Id(userId);
			}
			if (enquiries.isEmpty()) {
				return List.of();
			}
			enquiries.sort((e1, e2) -> e2.getCreatedOn().compareTo(e1.getCreatedOn()));
			return toEnquiryDtos(academyId, enquiries);
		} else if (userProfileDto.getUserType() == UserType.COACH) {
			List<Enquiry> enquiries;
			if ("OPEN".equalsIgnoreCase(status)) {
				enquiries = enquiryRepo.findByAcademy_IdAndStatus(academyId, EnquiryStatus.OPEN);
			} else if ("CLOSED".equalsIgnoreCase(status)) {
				enquiries = enquiryRepo.findByAcademy_IdAndStatus(academyId, EnquiryStatus.CLOSED);
			} else {
				enquiries = enquiryRepo.findByAcademy_Id(academyId);
			}
			if (enquiries.isEmpty()) {
				return List.of();
			}
			List<Enquiry> enquiries1 = new ArrayList<>(enquiries);
			enquiries1.sort((e1, e2) -> e2.getCreatedOn().compareTo(e1.getCreatedOn()));
			return toEnquiryDtos(academyId, enquiries1);
		} else {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
		}
	}

	@Override
	public void deleteEnquiry(String academyId, String enquiryId, UserDetail userDetail) throws ResourceException {
		Enquiry enquiry = enquiryRepo.findById(enquiryId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "ENQUIRY_NOT_FOUND"));

		if (!enquiry.getUserProfile().getId().equals(userDetail.getUserId())) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED_ACTION, "UNAUTHORIZED_ACTION");
		}

		// Delete the enquiry
		enquiryRepo.delete(enquiry);
	}

	private List<EnquiryDto> toEnquiryDtos(String academyId, List<Enquiry> enquiries) {
//        List<String> userIds = enquiries.stream().map(enquiry -> enquiry.getUserProfile().getId()).toList();
//        Map<String, UserProfileDto> userProfileDtoMap = new HashMap<>();
//        Map<String, CourseDto> courseDtoMap = new HashMap<>();
//        List<String> courseIds = enquiries.stream().map(enquiry -> enquiry.getCourse().getId()).toList();
//        if (!CollectionUtils.isEmpty(courseIds)) {
//            List<CourseDto> courseDtos = null;
//            try {
//                courseDtos = courseService.getCourses(academyId, courseIds, null, null);
//            } catch (ResourceException e) {
//                log.error("Error while fetching courses for enquiries", e);
//            }
//            courseDtoMap = courseDtos.stream().collect(Collectors.toMap(CourseDto::getId, courseDto -> courseDto));
//        }
//        if (!CollectionUtils.isEmpty(userIds)) {
//            List<UserProfileDto> userProfileDtos = null;
//            try {
//                userProfileDtos = userProfileService.getUserProfileByIds(userIds);
//                userProfileDtoMap = userProfileDtos.stream().collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));
//            } catch (ResourceException e) {
//                log.error("Error while fetching user profiles for enquiries", e);
//            }
//        }

//        Map<String, UserProfileDto> finalUserProfileDtoMap = userProfileDtoMap;
//        Map<String, CourseDto> finalCourseDto = courseDtoMap;

		return enquiries.stream().map(enquiry -> {
			EnquiryDto enquiryDto = new EnquiryDto(enquiry, null, null);
			enquiryDto.setUserProfile(modelMapper.map(enquiry.getUserProfile(), UserProfileMinDto.class));
			enquiryDto.setCourse(modelMapper.map(enquiry.getCourse(), CourseMinDto.class));
			return enquiryDto;
		}).collect(Collectors.toList());
	}
}
