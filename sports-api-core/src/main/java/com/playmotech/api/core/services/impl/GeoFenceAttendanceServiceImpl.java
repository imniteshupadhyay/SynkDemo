package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.CourseCoachMapping;
import com.playmotech.api.core.dao_postgres.GeoFence;
import com.playmotech.api.core.dao_postgres.GeoFenceCoachAttendance;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AutoCheckOutRequestDto;
import com.playmotech.api.core.dto.GeoFenceAttendanceCheckRequest;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CourseCoachMappingRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.GeoFenceCoachAttendanceRepository;
import com.playmotech.api.core.repo.GeoFenceRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.GeoFenceAttendanceDao;
import com.playmotech.api.core.services.GeoFenceAttendanceService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.utils.GeoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the GeoFenceAttendance service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoFenceAttendanceServiceImpl implements GeoFenceAttendanceService {

	private final GeoFenceCoachAttendanceRepository attendanceRepository;
	private final GeoFenceRepository geoFenceRepository;
	private final AcademyRepo academyRepo;
	private final UserProfileRepo userProfileRepo;
	private final CourseRepo courseRepo;
	private final CourseCoachMappingRepo courseCoachMappingRepo;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final IPushNotificationService pushNotificationService;

	@Transactional
	@Override
	public ServiceResponse checkIn(String coachId, GeoFenceAttendanceCheckRequest requestDto) {
		try {
			log.info("Start check-in for coach: {}", coachId);
			if (requestDto == null || requestDto.getAcademyId() == null || requestDto.getProgramId() == null
					|| requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Validate entities
			Optional<UserProfile> coachOpt = userProfileRepo.findByIdAndInactiveIsFalse(coachId);
			if (coachOpt.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.USER_NOT_FOUND);
			}
			Optional<Academy> academyOpt = academyRepo.findByIdAndInactiveFalse(requestDto.getAcademyId());
			if (academyOpt.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.ACADEMY_NOT_FOUND);
			}
			Optional<Course> courseOpt = courseRepo.findByAcademy_IdAndId(requestDto.getAcademyId(),
					requestDto.getProgramId());
			if (courseOpt.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.COURSE_NOT_FOUND);
			}

			// --- Check if coach is mapped to the course ---
			Optional<CourseCoachMapping> coachMappingOpt = courseCoachMappingRepo
					.findByCoachUserProfile_IdAndCourse_Id(coachId, requestDto.getProgramId());

			if (coachMappingOpt.isEmpty()) {
				return ResponseBuilder.notFound("Coach is not mapped to the course for this academy.");
			}

			// Ensure coach has no active attendance for the same program
			Optional<GeoFenceCoachAttendance> activeAttendanceOpt = attendanceRepository
					.findByCoach_IdAndProgram_IdAndIsActiveTrueAndDeletedFalse(coachId, requestDto.getProgramId());

			if (activeAttendanceOpt.isPresent()) {
				return ResponseBuilder.conflict("Coach already has an active attendance for this program");
			}

			// Find the nearest valid geo-fence for coach check-in
			GeoFence nearestFence = findNearestFence(requestDto.getAcademyId(), requestDto.getLatitude(),
					requestDto.getLongitude());

			if (nearestFence == null) {
				return ResponseBuilder.forbidden("No valid geo-fence configured for this academy");
			}

			// Verify coach is within the nearest fence boundary
			boolean withinFence = GeoUtils.isPointWithinRadius(requestDto.getLatitude(), requestDto.getLongitude(),
					nearestFence.getLatitude(), nearestFence.getLongitude(), nearestFence.getRadiusInMeters());

			if (!withinFence) {
				return ResponseBuilder.forbidden("Coach is outside the geo-fence boundary");
			}

			// --- Create attendance record ---
			GeoFenceCoachAttendance attendance = GeoFenceCoachAttendance.builder().academy(academyOpt.get())
					.program(courseOpt.get()).coach(coachOpt.get()).checkInLatitude(requestDto.getLatitude())
					.checkInLongitude(requestDto.getLongitude()).geofenceLatitude(nearestFence.getLatitude())
					.geofenceLongitude(nearestFence.getLongitude())
					.geofenceRadiusMeters(nearestFence.getRadiusInMeters())
					.checkInDistanceMeters(GeoUtils.calculateDistanceInMeters(requestDto.getLatitude(),
							requestDto.getLongitude(), nearestFence.getLatitude(), nearestFence.getLongitude()))
					.checkInTime(Instant.now()).isActive(true).deleted(false).autoCheckedOut(false).build();

			attendance = attendanceRepository.save(attendance);

			GeoFenceAttendanceDao dao = mapToDao(attendance);
			return ResponseBuilder.success(dao, ApiResponse.DATA_ADDED_SUCCESSFULLY, HttpStatus.CREATED);
		} catch (Exception e) {
			log.error("Error in check-in: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Finds the nearest valid geo-fence for the given location
	 * 
	 * @param academyId The academy ID to search for fences
	 * @param latitude  The latitude of the point to check
	 * @param longitude The longitude of the point to check
	 * @return The nearest GeoFence, or null if no valid fences found
	 */
	private GeoFence findNearestFence(String academyId, double latitude, double longitude) {
		try {
			// Validate inputs
			if (academyId == null) {
				log.warn("findNearestFence called with null academyId");
				return null;
			}

			List<String> userTypes = List.of("coach", "both");
			List<GeoFence> fences = geoFenceRepository.findByAcademy_IdAndUserTypeInAndDeletedFalse(academyId,
					userTypes);

			if (fences == null || fences.isEmpty()) {
				log.debug("No geo-fences found for academy: {}", academyId);
				return null;
			}

			GeoFence nearestFence = null;
			Double minDistance = null;
			int validFenceCount = 0;
			int invalidFenceCount = 0;

			for (GeoFence fence : fences) {
				// Skip fences with missing or invalid coordinates
				if (fence.getLatitude() == null || fence.getLongitude() == null || fence.getRadiusInMeters() == null) {
					log.debug("Skipping fence with missing coordinates or radius. Fence ID: {}", fence.getGeoFenceId());
					invalidFenceCount++;
					continue;
				}

				// Skip invalid radius
				if (fence.getRadiusInMeters() <= 0) {
					log.debug("Skipping fence with invalid radius: {} m. Fence ID: {}", fence.getRadiusInMeters(),
							fence.getGeoFenceId());
					invalidFenceCount++;
					continue;
				}

				double distance = GeoUtils.calculateDistanceInMeters(latitude, longitude, fence.getLatitude(),
						fence.getLongitude());

				// If this is the first valid fence or closer than current nearest
				if (minDistance == null || distance < minDistance) {
					minDistance = distance;
					nearestFence = fence;
				}
				validFenceCount++;
			}

			if (nearestFence != null) {
				log.debug("Found nearest fence: ID={}, Distance={}m, Total valid fences={}, Invalid fences={}",
						nearestFence.getGeoFenceId(), minDistance, validFenceCount, invalidFenceCount);
			} else {
				log.warn("No valid geo-fences found for academy: {}. Total fences checked: {}, Valid: {}, Invalid: {}",
						academyId, fences.size(), validFenceCount, invalidFenceCount);
			}

			return nearestFence;
		} catch (Exception e) {
			log.error("Error finding nearest fence for academy: {}, location: ({}, {}). Error: {}", academyId, latitude,
					longitude, e.getMessage(), e);
			return null;
		}
	}

	@Transactional
	@Override
	public ServiceResponse checkOut(String coachId, GeoFenceAttendanceCheckRequest requestDto) {
		try {
			log.info("Start check-out for coach: {}", coachId);
			if (requestDto == null || requestDto.getAcademyId() == null || requestDto.getProgramId() == null
					|| requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			Optional<GeoFenceCoachAttendance> attendanceOpt = attendanceRepository
					.findByCoach_IdAndAcademy_IdAndProgram_IdAndIsActiveTrueAndDeletedFalse(coachId,
							requestDto.getAcademyId(), requestDto.getProgramId());
			if (attendanceOpt.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.ATTENDANCE_NOT_FOUND);
			}

			// Require coach to be within fence for manual checkout
//			boolean withinFence = isWithinCoachFence(requestDto.getAcademyId(), requestDto.getLatitude(),
//					requestDto.getLongitude());
//			if (!withinFence) {
//				return ResponseBuilder.forbidden("Coach is outside the geo-fence");
//			}

			GeoFenceCoachAttendance attendance = attendanceOpt.get();
			attendance.setCheckOutLatitude(requestDto.getLatitude());
			attendance.setCheckOutLongitude(requestDto.getLongitude());
			attendance.setCheckOutDistanceMeters(computeNearestCoachFenceDistanceMeters(requestDto.getAcademyId(),
					requestDto.getLatitude(), requestDto.getLongitude()));
			attendance.setCheckOutTime(Instant.now());
			attendance.setIsActive(false);
			attendance.setAutoCheckedOut(Boolean.FALSE);

			attendance = attendanceRepository.save(attendance);

			GeoFenceAttendanceDao dao = mapToDao(attendance);
			return ResponseBuilder.success(dao, ApiResponse.DATA_UPDATED_SUCCESSFULLY, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error in check-out: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Transactional
	@Override
	public ServiceResponse autoCheckOut(String coachId, AutoCheckOutRequestDto requestDto) {
		try {
			log.info("Start auto check-out for coach: {}", requestDto != null ? coachId : null);
			if (requestDto == null || coachId == null || requestDto.getLatitude() == null
					|| requestDto.getLongitude() == null) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			List<GeoFenceCoachAttendance> activeAttendances = attendanceRepository
					.findByCoach_IdAndIsActiveTrueAndDeletedFalse(coachId);
			if (activeAttendances.isEmpty()) {
				return ResponseBuilder.notFound(ApiResponse.ATTENDANCE_NOT_FOUND);
			}

			int updated = 0;
			for (GeoFenceCoachAttendance att : activeAttendances) {
				String academyId = att.getAcademy().getId();
//				boolean withinFence = isWithinCoachFence(academyId, requestDto.getLatitude(),
//						requestDto.getLongitude());
//				if (!withinFence) {
				att.setCheckOutLatitude(requestDto.getLatitude());
				att.setCheckOutLongitude(requestDto.getLongitude());
				att.setCheckOutDistanceMeters(computeNearestCoachFenceDistanceMeters(academyId,
						requestDto.getLatitude(), requestDto.getLongitude()));
				att.setCheckOutTime(Instant.now());
				att.setIsActive(false);
				att.setAutoCheckedOut(Boolean.TRUE);
				attendanceRepository.save(att);
				updated++;
//				}
			}

			if (updated == 0) {
				return ResponseBuilder
						.notAcceptable("All active attendances are within the geo-fence. No check-outs performed.");
			}

			return ResponseBuilder.success(ApiResponse.DATA_UPDATED_SUCCESSFULLY, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error in auto check-out: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public ServiceResponse verifyCoachLocation(String coachId, String academyId, Double latitude, Double longitude) {
		try {
			log.info("Verify coach location for coach: {}", coachId);

			// Validate request params early
			if (latitude == null || longitude == null) {
				return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
			}

			// Latitude/Longitude range validation
			if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
				return ResponseBuilder.badRequest("Invalid latitude/longitude values");
			}

			// Get coach mappings (only ACTIVE ones)
			List<CoachAcademyMapping> coachMappings;
			if (academyId != null && !academyId.isBlank()) {
				coachMappings = coachAcademyMappingRepo.findByCoachUserProfile_IdAndAcademy_IdAndStatus(coachId,
						academyId, Status.ACTIVE);
			} else {
				coachMappings = coachAcademyMappingRepo.findByCoachUserProfile_IdAndStatus(coachId, Status.ACTIVE);
			}

			if (coachMappings.isEmpty()) {
				log.warn("Coach {} has no active academy mapping{}", coachId,
						academyId != null ? " for academy " + academyId : "");
				return ResponseBuilder.badRequest("Coach not mapped to any active academy");
			}

			// Gather all academies where coach is mapped
			List<String> academyIds = coachMappings.stream().map(mapping -> mapping.getAcademy().getId())
					.filter(Objects::nonNull).toList();

			if (academyIds.isEmpty()) {
				return ResponseBuilder.badRequest("No valid academy IDs found for coach");
			}

			// Fetch geo fences for those academies
			List<GeoFence> fences = geoFenceRepository.findByAcademy_IdInAndUserTypeInAndDeletedFalse(academyIds,
					List.of("coach", "both"));

			if (fences.isEmpty()) {
				log.warn("No active geo-fences configured for academies: {}", academyIds);
				return ResponseBuilder.success(
						Map.of("hasOngoingSession", false, "isWithinFence", false, "fences", List.of()),
						"No geo-fences configured for coach academies", HttpStatus.OK);
			}

			// Fetch coach's active attendances
			List<GeoFenceCoachAttendance> activeAttendances = attendanceRepository
					.findByCoach_IdAndIsActiveTrueAndDeletedFalse(coachId);

			boolean withinFence = false;
			double nearestDistance = Double.MAX_VALUE;
			GeoFence nearestFence = null;
			List<Map<String, Object>> insideFences = new ArrayList<>();

			for (GeoFence fence : fences) {
				if (fence.getLatitude() == null || fence.getLongitude() == null || fence.getRadiusInMeters() == null) {
					continue;
				}

				double distance = GeoUtils.calculateDistanceInMeters(latitude, longitude, fence.getLatitude(),
						fence.getLongitude());

				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearestFence = fence;
				}

				if (distance <= fence.getRadiusInMeters()) {
					withinFence = true;

					Map<String, Object> fenceInfo = new HashMap<>();
					fenceInfo.put("academyId", fence.getAcademy() != null ? fence.getAcademy().getId() : null);
					fenceInfo.put("academyName", fence.getAcademy() != null ? fence.getAcademy().getName() : null);
					fenceInfo.put("distanceFromFenceCenter", distance);
					fenceInfo.put("fenceRadius", fence.getRadiusInMeters());
					fenceInfo.put("fenceLatitude", fence.getLatitude());
					fenceInfo.put("fenceLongitude", fence.getLongitude());

					insideFences.add(fenceInfo);
				}
			}

			// Send notification only if inside at least one fence and no active attendance
			if (withinFence && activeAttendances.isEmpty()) {
				String academyName = nearestFence != null && nearestFence.getAcademy() != null
						? nearestFence.getAcademy().getName()
						: "the academy";
				sendGeoFenceNotification(coachId, academyId, academyName);
			}

			// Build response body
			Map<String, Object> body = new HashMap<>();
			body.put("hasOngoingSession", !activeAttendances.isEmpty());
			body.put("isWithinFence", withinFence);
			body.put("fences", insideFences);

			// Add program ID if there are active attendances
			if (!activeAttendances.isEmpty()) {
				body.put("academyId", activeAttendances.get(0).getAcademy().getId());
				body.put("programId", activeAttendances.get(0).getProgram().getId());
			}

			// Add nearest fence info for convenience
			if (nearestFence != null) {
				body.put("nearestFence", Map.of("academyId",
						nearestFence.getAcademy() != null ? nearestFence.getAcademy().getId() : null, "academyName",
						nearestFence.getAcademy() != null ? nearestFence.getAcademy().getName() : null,
						"distanceFromFenceCenter", nearestDistance, "fenceRadius", nearestFence.getRadiusInMeters(),
						"fenceLatitude", nearestFence.getLatitude(), "fenceLongitude", nearestFence.getLongitude()));
			}

			return ResponseBuilder.success(body, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error verifying coach location: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
		}
	}

	/**
	 * Handles push notification logic when coach enters a geo-fence.
	 */
	private void sendGeoFenceNotification(String coachId, String academyId, String academyName) {
		try {
			userProfileRepo.findByIdAndInactiveIsFalse(coachId).ifPresent(coach -> {
				String notificationTitle = "Arrived at " + academyName;
				String notificationBody = String.format("You're inside %s's geo-fence. Please mark your attendance.",
						academyName);
				String ctaScreen = "COACH_ATTENDANCE";

				Map<String, String> extraArgs = Map.of("academyId", academyId, "academyName", academyName);

				// Push notification
				if (coach.getAndroidFcmPushToken() != null) {
					pushNotificationService.sendMessageToPushToken(coach.getAndroidFcmPushToken(),
							NotificationType.LIVE_NOTIFICATION, notificationTitle, notificationBody, ctaScreen,
							CtaType.SCREEN, extraArgs);
				}

				// Add to notification center
				pushNotificationService.addNotification(List.of(coachId), notificationBody, CtaType.SCREEN, ctaScreen,
						extraArgs);
			});
		} catch (Exception e) {
			log.error("Error sending push notification for geo-fence verification: {}", e.getMessage(), e);
		}
	}

	private boolean isWithinCoachFence(String academyId, double latitude, double longitude) {
		List<String> userTypes = List.of("coach", "both");
		List<GeoFence> fences = geoFenceRepository.findByAcademy_IdAndUserTypeInAndDeletedFalse(academyId, userTypes);
		if (fences == null || fences.isEmpty()) {
			return false;
		}
		for (GeoFence fence : fences) {
			if (fence.getLatitude() == null || fence.getLongitude() == null || fence.getRadiusInMeters() == null) {
				continue;
			}
			boolean inside = GeoUtils.isPointWithinRadius(latitude, longitude, fence.getLatitude(),
					fence.getLongitude(), fence.getRadiusInMeters());
			if (inside) {
				return true;
			}
		}
		return false;
	}

	// Compute nearest distance (in meters) from given location to any applicable
	// coach/both geo-fence
	private Double computeNearestCoachFenceDistanceMeters(String academyId, double latitude, double longitude) {
		List<String> userTypes = List.of("coach", "both");
		List<GeoFence> fences = geoFenceRepository.findByAcademy_IdAndUserTypeInAndDeletedFalse(academyId, userTypes);
		if (fences == null || fences.isEmpty()) {
			return null;
		}
		Double minDistance = null;
		for (GeoFence fence : fences) {
			if (fence.getLatitude() == null || fence.getLongitude() == null) {
				continue;
			}
			double distance = GeoUtils.calculateDistanceInMeters(latitude, longitude, fence.getLatitude(),
					fence.getLongitude());
			if (minDistance == null || distance < minDistance) {
				minDistance = distance;
			}
		}
		return minDistance;
	}

	private GeoFenceAttendanceDao mapToDao(GeoFenceCoachAttendance a) {
		return GeoFenceAttendanceDao.builder().id(a.getId())
				.academyId(a.getAcademy() != null ? a.getAcademy().getId() : null)
				.programId(a.getProgram() != null ? a.getProgram().getId() : null)
				.coachId(a.getCoach() != null ? a.getCoach().getId() : null).checkInLatitude(a.getCheckInLatitude())
				.checkInLongitude(a.getCheckInLongitude()).checkInDistanceMeters(a.getCheckInDistanceMeters())
				.checkOutLatitude(a.getCheckOutLatitude()).checkOutLongitude(a.getCheckOutLongitude())
				.checkOutDistanceMeters(a.getCheckOutDistanceMeters())
				.checkInTime(a.getCheckInTime() != null ? Timestamp.from(a.getCheckInTime()) : null)
				.checkOutTime(a.getCheckOutTime() != null ? Timestamp.from(a.getCheckOutTime()) : null)
				.isActive(a.getIsActive()).build();
	}
}
