package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.GeoFence;
import com.playmotech.api.core.dao_postgres.GeoFenceCoachAttendance;
import com.playmotech.api.core.dto.GeoFenceRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.GeoFenceCoachAttendanceRepository;
import com.playmotech.api.core.repo.GeoFenceRepository;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.GeoFenceDao;
import com.playmotech.api.core.services.GeoFenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation of the GeoFence service following VideoAnalyticsServiceImpl
 * patterns
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class GeoFenceServiceImpl implements GeoFenceService {

	private final ModelMapper modelMapper = new ModelMapper();
	private final GeoFenceRepository geoFenceRepository;
	private final AcademyRepo academyRepo;
	private final GeoFenceCoachAttendanceRepository geoFenceCoachAttendanceRepository;

	@Override
	public ServiceResponse createGeoFence(GeoFenceRequestDto requestDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			log.info("Creating geo-fence for academy: {} by user: {}", requestDto.getAcademyId(),
					currentUser.getUserId());

			// --- Validate request ---
			if (requestDto == null || requestDto.getAcademyId() == null || requestDto.getAcademyId().trim().isEmpty()) {
				return ResponseBuilder.error("Academy ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
				return ResponseBuilder.error("Geo-fence name is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			if (requestDto.getLatitude() == null || requestDto.getLongitude() == null
					|| requestDto.getRadiusInMeters() == null) {
				return ResponseBuilder.error("Latitude, longitude, and radiusInMeters are required",
						ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
			}

			if (requestDto.getUserType() == null || requestDto.getUserType().trim().isEmpty()) {
				return ResponseBuilder.error("User type is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// --- Check if academy exists ---
			Optional<Academy> academyOpt = academyRepo.findById(requestDto.getAcademyId());
			if (academyOpt.isEmpty()) {
				return ResponseBuilder.error("Academy not found with ID: " + requestDto.getAcademyId(),
						ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			Academy academy = academyOpt.get();

			// --- Check if a non-deleted geo-fence already exists for this academy ---
			List<GeoFence> existingGeoFenceOpt = geoFenceRepository
					.findByAcademy_IdAndDeletedFalse(requestDto.getAcademyId());

			if (!existingGeoFenceOpt.isEmpty()) {
				return ResponseBuilder.error("An active geo-fence already exists for this academy.",
						ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
			}

			// --- Create new geo-fence entity ---
			GeoFence geoFence = new GeoFence();
			geoFence.setAcademy(academy);
			geoFence.setName(requestDto.getName().trim());
			geoFence.setLatitude(requestDto.getLatitude());
			geoFence.setLongitude(requestDto.getLongitude());
			geoFence.setRadiusInMeters(requestDto.getRadiusInMeters());
			geoFence.setUserType(requestDto.getUserType().toLowerCase().trim());
			geoFence.setSendNotification(
					requestDto.getSendNotification() != null ? requestDto.getSendNotification() : false);
			geoFence.setCreatedBy(currentUser.getUserId());

			Timestamp now = Timestamp.from(Instant.now());
			geoFence.setCreatedOn(now);
			geoFence.setDeleted(false);

			// --- Save to database ---
			GeoFence savedGeoFence = geoFenceRepository.save(geoFence);
			log.info("Created geo-fence with ID: {} for academy: {} by user: {}", savedGeoFence.getGeoFenceId(),
					requestDto.getAcademyId(), currentUser.getUserId());

			// --- Convert to DTO and return ---
			GeoFenceDao geoFenceDto = convertToDto(savedGeoFence);

			return ResponseBuilder.success(geoFenceDto, "Geo-fence created successfully", HttpStatus.CREATED);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to create geo-fence for academy: {}",
					requestDto != null ? requestDto.getAcademyId() : "null", e);
			return ResponseBuilder.error("Failed to create geo-fence: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getGeoFences(String academyId) {
		try {
			log.info("Getting all geo-fences for academy: {}", academyId);

			// --- Validate request ---
			if (academyId == null || academyId.trim().isEmpty()) {
				return ResponseBuilder.error("Academy ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// --- Validate academy exists ---
			if (!academyRepo.existsById(academyId)) {
				return ResponseBuilder.error("Academy not found with ID: " + academyId, ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			// --- Get all geo-fences for academy (non-deleted) ---
			List<GeoFence> geoFences = geoFenceRepository.findByAcademy_IdAndDeletedFalse(academyId);

			if (geoFences == null || geoFences.isEmpty()) {
				log.info("No geo-fences found for academy: {}", academyId);
				return ResponseBuilder.success("No geo-fences found for the given academy");
			}

			GeoFenceDao firstGeoFenceDto = convertToDto(geoFences.get(0));
			log.info("Retrieved {} geo-fences for academy: {}", geoFences.size(), academyId);

			return ResponseBuilder.success(firstGeoFenceDto, "Geo-fence retrieved successfully", HttpStatus.OK);
		} catch (Exception e) {
			log.error("Failed to get geo-fences for academy: {}", academyId, e);
			return ResponseBuilder.error("Failed to retrieve geo-fences: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse updateGeoFence(GeoFenceRequestDto requestDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			log.info("Updating geo-fence with ID: {} for academy: {} by user: {}", requestDto.getGeoFenceId(),
					requestDto.getAcademyId(), currentUser.getUserId());

			// --- Validate request ---
			if (requestDto.getGeoFenceId() == null) {
				return ResponseBuilder.error("Geo-fence ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			if (requestDto.getAcademyId() == null || requestDto.getAcademyId().trim().isEmpty()) {
				return ResponseBuilder.error("Academy ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// --- Find geo-fence ---
			Optional<GeoFence> geoFenceOpt = geoFenceRepository.findByAcademy_IdAndGeoFenceIdAndDeletedFalse(
					requestDto.getAcademyId(), requestDto.getGeoFenceId());

			if (geoFenceOpt.isEmpty()) {
				return ResponseBuilder.error("Geo-fence not found", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			GeoFence geoFence = geoFenceOpt.get();

			// Check if there are any active attendances for this academy's geo-fence
			List<GeoFenceCoachAttendance> activeAttendances = geoFenceCoachAttendanceRepository
					.findByAcademy_IdAndIsActiveTrueAndDeletedFalse(requestDto.getAcademyId());

			if (!activeAttendances.isEmpty()) {
				return ResponseBuilder.error(
						"Cannot update geo-fence with active attendances. Please ensure all coaches have checked out first.",
						ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
			}

			// --- Check for name conflicts if name is being updated ---
			if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()
					&& !requestDto.getName().equals(geoFence.getName())) {

				Optional<GeoFence> existingWithSameName = geoFenceRepository
						.findByAcademy_IdAndNameIgnoreCaseAndDeletedFalse(requestDto.getAcademyId(),
								requestDto.getName());

				if (existingWithSameName.isPresent()
						&& !existingWithSameName.get().getGeoFenceId().equals(requestDto.getGeoFenceId())) {
					return ResponseBuilder.error(
							"Geo-fence with name '" + requestDto.getName() + "' already exists for this academy",
							ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
				}
				geoFence.setName(requestDto.getName().trim());
			}

			// --- Update fields if provided ---
			if (requestDto.getLatitude() != null) {
				geoFence.setLatitude(requestDto.getLatitude());
			}

			if (requestDto.getLongitude() != null) {
				geoFence.setLongitude(requestDto.getLongitude());
			}

			if (requestDto.getRadiusInMeters() != null) {
				geoFence.setRadiusInMeters(requestDto.getRadiusInMeters());
			}

			if (requestDto.getUserType() != null && !requestDto.getUserType().trim().isEmpty()) {
				geoFence.setUserType(requestDto.getUserType().toLowerCase().trim());
			}

			if (requestDto.getSendNotification() != null) {
				geoFence.setSendNotification(requestDto.getSendNotification());
			}

			// --- Update metadata ---
			geoFence.setUpdatedBy(currentUser.getUserId());
			geoFence.setDeleted(false);

			// --- Save updated entity ---
			GeoFence updatedGeoFence = geoFenceRepository.save(geoFence);
			log.info("Updated geo-fence with ID: {} for academy: {} by user: {}", requestDto.getGeoFenceId(),
					requestDto.getAcademyId(), currentUser.getUserId());

			// --- Convert to DTO ---
			GeoFenceDao geoFenceDto = convertToDto(updatedGeoFence);

			return ResponseBuilder.success(geoFenceDto, "Geo-fence updated successfully");

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to update geo-fence with ID: {} for academy: {}",
					requestDto != null ? requestDto.getGeoFenceId() : "null",
					requestDto != null ? requestDto.getAcademyId() : "null", e);
			return ResponseBuilder.error("Failed to update geo-fence: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse deleteGeoFence(Long geoFenceId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			log.info("Deleting geo-fence with ID: {} by user: {}", geoFenceId, currentUser.getUserId());

			// --- Validate request ---
			if (geoFenceId == null) {
				return ResponseBuilder.error("Geo-fence ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// --- Find non-deleted geo-fence ---
			Optional<GeoFence> geoFenceOpt = geoFenceRepository.findById(geoFenceId);
			if (geoFenceOpt.isEmpty() || Boolean.TRUE.equals(geoFenceOpt.get().getDeleted())) {
				return ResponseBuilder.error(
						"Geo-fence not found with ID: " + geoFenceId + " or has already been deleted",
						ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			GeoFence geoFence = geoFenceOpt.get();

			geoFence.setDeleted(true);
			geoFenceRepository.save(geoFence);

			log.info("Deleted geo-fence with ID: {} by user: {}", geoFenceId, currentUser.getUserId());

			return ResponseBuilder.success("Geo-fence deleted successfully");

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to delete geo-fence with ID: {}", geoFenceId, e);
			return ResponseBuilder.error("Failed to delete geo-fence: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Helper method to convert a GeoFence entity to a DTO
	 */
	private GeoFenceDao convertToDto(GeoFence geoFence) {
		if (geoFence == null) {
			return null;
		}

		GeoFenceDao dto = modelMapper.map(geoFence, GeoFenceDao.class);

		// Set academy ID explicitly
		if (geoFence.getAcademy() != null) {
			dto.setAcademyId(geoFence.getAcademy().getId());
		}

		// Convert SQL Timestamp to Instant for better JSON serialization
		if (geoFence.getCreatedOn() != null) {
			dto.setCreatedOn(geoFence.getCreatedOn().toInstant());
		}
		if (geoFence.getUpdatedOn() != null) {
			dto.setUpdatedOn(geoFence.getUpdatedOn().toInstant());
		}

		return dto;
	}

}