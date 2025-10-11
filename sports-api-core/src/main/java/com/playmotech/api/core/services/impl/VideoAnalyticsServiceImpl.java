package com.playmotech.api.core.services.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.VideoAnalytics;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dto.AnalyticsItem;
import com.playmotech.api.core.dto.UpdateAnalyticsRequest;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.VideoAnalysisRequest;
import com.playmotech.api.core.repo.VideoAnalyticsRepository;
import com.playmotech.api.core.repo.VideoAnalyzerRepo;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.VideoAnalyticsResponse;
import com.playmotech.api.core.services.VideoAnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class VideoAnalyticsServiceImpl implements VideoAnalyticsService {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\d+):(\\d+(?:\\.\\d+)?)");

	private final VideoAnalyzerRepo videoAnalyzerRepo;
	private final VideoAnalyticsRepository videoAnalyticsRepository;
	private final GeminiAnalysisService geminiAnalysisService;

	@Override
	public ServiceResponse submitVideoAnalytics(VideoAnalysisRequest request, boolean reanalyze) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			// --- Validate request ---
			if (request == null || request.getVideoId() == null || request.getVideoId().trim().isEmpty()) {
				return ResponseBuilder.error("Video ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			if (request.getSportsType() == null) {
				return ResponseBuilder.error("Sports type is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// --- Check existing analytics ---
			Optional<VideoAnalytics> existingOpt = videoAnalyticsRepository
					.findByVideoIdAndDeletedFalse(request.getVideoId());

			if (existingOpt.isPresent()) {
				VideoAnalytics existing = existingOpt.get();

				if (reanalyze || existing.getStatus() == AnalysisStatus.FAILED) {
					log.info("{} video analytics for video: {}", reanalyze ? "Reanalyzing" : "Resubmitting failed",
							request.getVideoId());

					existing.setStatus(AnalysisStatus.PENDING);
					existing.setUpdatedBy(currentUser.getUserId());
					existing.setAdditionalContext(request.getAdditionalContext());
					existing.setExtraParams(convertMapToJson(request.getExtraParams()));
					videoAnalyticsRepository.save(existing);

					markVideoAsAnalysed(request.getVideoId(), currentUser.getUserId());

					geminiAnalysisService.processVideoAnalysisAsync(existing.getId(), request);

					return ResponseBuilder.success(buildResponseData(existing),
							reanalyze ? "Analytics request resubmitted for reanalysis"
									: "Analytics request resubmitted");
				}

				// Conflict – already exists and not failed
				return ResponseBuilder.error("Analytics already exists for video: " + request.getVideoId()
						+ " with status: " + existing.getStatus(), ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
			}

			// --- No existing record — validate extraParams ---
			Map<String, Object> normalizedParams = new HashMap<>(request.getExtraParams());

			if (request.getExtraParams().containsKey("extraParams")) {
				Object nested = request.getExtraParams().get("extraParams");
				if (nested instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> nestedParams = (Map<String, Object>) nested;
					normalizedParams.putAll(nestedParams);
				}
			}

			log.info("Validating prompt template with params: {} for sport: {}", normalizedParams,
					request.getSportsType());

			if (!geminiAnalysisService.validatePromptTemplate(request.getSportsType(), normalizedParams)) {
				return ResponseBuilder.error("Invalid extra parameters for sports type: " + request.getSportsType(),
						ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
			}

			// --- Create new analytics entry ---
			VideoAnalytics analytics = new VideoAnalytics();
			analytics.setVideo(VideoAnalyzer.builder().id(request.getVideoId()).build());
			analytics.setSportsType(request.getSportsType());
			analytics.setStatus(AnalysisStatus.PENDING);
			analytics.setCreatedBy(currentUser.getUserId());
			analytics.setAdditionalContext(request.getAdditionalContext());
			analytics.setExtraParams(convertMapToJson(request.getExtraParams()));
			analytics.setDeleted(false);

			VideoAnalytics savedAnalytics = videoAnalyticsRepository.save(analytics);
			log.info("Created analytics entry with ID: {} for video: {}", savedAnalytics.getId(), request.getVideoId());

			markVideoAsAnalysed(request.getVideoId(), currentUser.getUserId());

			geminiAnalysisService.processVideoAnalysisAsync(savedAnalytics.getId(), request);

			return ResponseBuilder.success(buildResponseData(savedAnalytics),
					"Analytics request submitted for analysis", HttpStatus.CREATED);

		} catch (Exception e) {
			log.error("Failed to submit video analytics request for video: {}", request.getVideoId(), e);
			return ResponseBuilder.error("Failed to submit analytics request: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void markVideoAsAnalysed(String videoId, String updatedBy) {
		Optional<VideoAnalyzer> videoOpt = videoAnalyzerRepo.findById(videoId);
		if (videoOpt.isPresent()) {
			VideoAnalyzer video = videoOpt.get();
			if (!video.isAnalysed()) {
				video.setAnalysed(true);
				videoAnalyzerRepo.save(video);
				log.info("Marked video {} as analysed", video.getId());
			}
		}
	}

	/**
	 * Converts timestamp string (e.g., "0:07.5") to seconds for comparison
	 */
	public static double timestampToSeconds(String timestamp) {
		if (timestamp == null || timestamp.trim().isEmpty()) {
			return 0.0;
		}

		Matcher matcher = TIMESTAMP_PATTERN.matcher(timestamp);
		if (matcher.matches()) {
			int minutes = Integer.parseInt(matcher.group(1));
			double seconds = Double.parseDouble(matcher.group(2));
			return minutes * 60 + seconds;
		}

		// If format doesn't match, try to parse as just seconds
		try {
			return Double.parseDouble(timestamp);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/**
	 * Comparator for sorting AnalyticsItem by timestampOfOutcome
	 */
	public static final Comparator<AnalyticsItem> TIMESTAMP_COMPARATOR = (item1, item2) -> {
		String timestamp1 = item1.getTimestampOfOutcome();
		String timestamp2 = item2.getTimestampOfOutcome();

		double seconds1 = timestampToSeconds(timestamp1);
		double seconds2 = timestampToSeconds(timestamp2);

		return Double.compare(seconds1, seconds2);
	};

	/**
	 * Sort a list of AnalyticsItem by timestamp in ascending order
	 */
	public static void sortByTimestamp(List<AnalyticsItem> analyticsList) {
		if (analyticsList != null) {
			analyticsList.sort(TIMESTAMP_COMPARATOR);
		}
	}

	/**
	 * Check if a list is already sorted by timestamp
	 */
	public static boolean isTimestampSorted(List<AnalyticsItem> analyticsList) {
		if (analyticsList == null || analyticsList.size() <= 1) {
			return true;
		}

		for (int i = 0; i < analyticsList.size() - 1; i++) {
			double current = timestampToSeconds(analyticsList.get(i).getTimestampOfOutcome());
			double next = timestampToSeconds(analyticsList.get(i + 1).getTimestampOfOutcome());

			if (current > next) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Ensures analytics items are sorted by timestamp before saving or returning
	 */
	private void ensureTimestampSorting(List<AnalyticsItem> analyticsList) {
		if (analyticsList != null && !analyticsList.isEmpty()) {
			sortByTimestamp(analyticsList);
			log.debug("Sorted {} analytics items by timestamp", analyticsList.size());
		}
	}

	@Override
	public ServiceResponse updateVideoAnalytics(Long analyticsId, UpdateAnalyticsRequest request) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			if (request == null || request.getAnalytics() == null || request.getAnalytics().trim().isEmpty()) {
				return ResponseBuilder.error("Analytics data is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			VideoAnalytics analytics = videoAnalyticsRepository.findByIdAndDeletedFalse(analyticsId)
					.orElseThrow(() -> new NoSuchElementException("Video analytics not found with ID: " + analyticsId));

			if (analytics.getStatus() == AnalysisStatus.IN_PROGRESS) {
				return ResponseBuilder.error("Cannot update analytics while analysis is in progress",
						ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
			}

			// Parse, sort, and re-serialize the analytics data
			try {
				TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<>() {
				};
				List<AnalyticsItem> analyticsList = objectMapper.readValue(request.getAnalytics(), typeRef);

				// Ensure timestamp sorting
				ensureTimestampSorting(analyticsList);

				// Convert back to JSON
				String sortedAnalyticsJson = objectMapper.writeValueAsString(analyticsList);
				analytics.setAnalysisJson(sortedAnalyticsJson);

			} catch (JsonProcessingException e) {
				log.error("Failed to parse or sort analytics data for ID: {}", analyticsId, e);
				return ResponseBuilder.error("Invalid analytics data format", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// Set updated fields
			analytics.setUpdatedBy(currentUser.getUserId());
			analytics.setStatus(AnalysisStatus.COMPLETED);

			// Set analysis source to SELF since this is a user update
			// analytics.setAnalysisSource(VideoAnalytics.AnalysisSource.SELF);

			// Save changes
			VideoAnalytics updatedAnalytics = videoAnalyticsRepository.save(analytics);
			VideoAnalyticsResponse response = convertToResponse(updatedAnalytics);

			log.info("Updated video analytics with ID: {} by user: {} (SELF analysis)", analyticsId,
					currentUser.getUserId());
			return ResponseBuilder.success(response, "Video analytics updated successfully");

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to update video analytics with ID: {}", analyticsId, e);
			return ResponseBuilder.error("Failed to update video analytics: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getAnalyticsByVideoId(String videoId) {
		try {
			if (videoId == null || videoId.trim().isEmpty()) {
				return ResponseBuilder.error("Video ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			VideoAnalytics analytics = videoAnalyticsRepository.findByVideoIdAndDeletedFalse(videoId)
					.orElseThrow(() -> new NoSuchElementException("Video analytics not found for videoId: " + videoId));

			VideoAnalyticsResponse response = convertToResponse(analytics);

			return ResponseBuilder.success(response, "Video analytics retrieved successfully");
		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to get video analytics for videoId: {}", videoId, e);
			return ResponseBuilder.error("Failed to retrieve video analytics: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getAnalytics(Long analyticsId) {
		try {
			VideoAnalytics analytics = videoAnalyticsRepository.findByIdAndDeletedFalse(analyticsId)
					.orElseThrow(() -> new NoSuchElementException("Video analytics not found with ID: " + analyticsId));

			VideoAnalyticsResponse response = convertToResponse(analytics);
			return ResponseBuilder.success(response, "Video analytics retrieved successfully");
		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to get video analytics with ID: {}", analyticsId, e);
			return ResponseBuilder.error("Failed to retrieve video analytics: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse retryAnalysis(Long analyticsId) {
		try {
			VideoAnalytics analytics = videoAnalyticsRepository.findByIdAndDeletedFalse(analyticsId)
					.orElseThrow(() -> new NoSuchElementException("Video analytics not found with ID: " + analyticsId));

			if (analytics.getStatus() != AnalysisStatus.FAILED) {
				return ResponseBuilder.error("Can only retry failed analyses. Current status: " + analytics.getStatus(),
						ErrorCodes.RESOURCE_CONFLICT, HttpStatus.BAD_REQUEST);
			}

			analytics.setStatus(AnalysisStatus.PENDING);
			videoAnalyticsRepository.save(analytics);

			VideoAnalysisRequest request = new VideoAnalysisRequest();
			request.setVideoId(analytics.getVideo().getId());
			request.setSportsType(analytics.getSportsType());

			if (analytics.getExtraParams() != null && !analytics.getExtraParams().isEmpty()) {
				try {
					TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
					};
					Map<String, Object> extraParams = objectMapper.readValue(analytics.getExtraParams(), typeRef);
					request.setExtraParams(extraParams);
				} catch (Exception e) {
					log.warn("Failed to parse extra params for retry, using empty map", e);
					request.setExtraParams(new HashMap<>());
				}
			}

			geminiAnalysisService.processVideoAnalysisAsync(analytics.getId(), request);

			Map<String, Object> responseData = buildResponseData(analytics);
			return ResponseBuilder.success(responseData, "Analysis retry initiated");

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to retry analysis for ID: {}", analyticsId, e);
			return ResponseBuilder.error("Failed to retry analysis: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse deleteAnalytics(Long analyticsId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			VideoAnalytics analytics = videoAnalyticsRepository.findByIdAndDeletedFalse(analyticsId)
					.orElseThrow(() -> new NoSuchElementException("Video analytics not found with ID: " + analyticsId));

			if (analytics.getStatus() == AnalysisStatus.IN_PROGRESS) {
				return ResponseBuilder.error("Cannot delete analytics while analysis is in progress",
						ErrorCodes.RESOURCE_CONFLICT, HttpStatus.CONFLICT);
			}

			// Soft delete - set deleted flag to true
			analytics.setDeleted(true);
			analytics.setUpdatedBy(currentUser.getUserId());
			videoAnalyticsRepository.save(analytics);

			log.info("Soft deleted video analytics with ID: {} by user: {}", analyticsId, currentUser.getUserId());

			return ResponseBuilder.success("Video analytics deleted successfully");

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to delete video analytics with ID: {}", analyticsId, e);
			return ResponseBuilder.error("Failed to delete video analytics: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private VideoAnalyticsResponse convertToResponse(VideoAnalytics analytics) {
		VideoAnalyticsResponse response = new VideoAnalyticsResponse();
		response.setAnalyticsId(analytics.getId());
		response.setVideoId(analytics.getVideo().getId());
		response.setCreatedOn(analytics.getCreatedOn());
//		response.setCreatedBy(analytics.getCreatedBy());
//		response.setUpdatedOn(analytics.getUpdatedOn());
//		response.setUpdatedBy(analytics.getUpdatedBy());
		response.setStatus(analytics.getStatus());
		response.setSportsType(analytics.getSportsType());
		response.setAnalysisSource(analytics.getAnalysisSource()); // Include analysis source in response
		response.setAdditionalContext(analytics.getAdditionalContext()); // Include additional context in response

		if (analytics.getAnalysisJson() != null && !analytics.getAnalysisJson().isEmpty()) {
			try {
				TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<>() {
				};
				List<AnalyticsItem> analyticsList = objectMapper.readValue(analytics.getAnalysisJson(), typeRef);

				// Ensure timestamp sorting before returning
				ensureTimestampSorting(analyticsList);

				response.setAnalytics(analyticsList);
			} catch (Exception e) {
				log.error("Failed to parse analysis JSON for ID: {}", analytics.getId(), e);
				response.setAnalytics(Collections.emptyList());
			}
		} else {
			response.setAnalytics(Collections.emptyList());
		}

		if (analytics.getExtraParams() != null && !analytics.getExtraParams().isEmpty()) {
			try {
				TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
				};
				response.setExtraParams(objectMapper.readValue(analytics.getExtraParams(), typeRef));
			} catch (Exception e) {
				log.error("Failed to parse extra params JSON for ID: {}", analytics.getId(), e);
				response.setExtraParams(Collections.emptyMap());
			}
		} else {
			response.setExtraParams(Collections.emptyMap());
		}

		return response;
	}

	private Map<String, Object> buildResponseData(VideoAnalytics analytics) {
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("analyticsId", analytics.getId());
		responseData.put("videoId", analytics.getVideo().getId());
		responseData.put("status", analytics.getStatus());
		responseData.put("sportsType", analytics.getSportsType());
		responseData.put("analysisSource", analytics.getAnalysisSource());

		// Include additionalContext if it exists
		if (analytics.getAdditionalContext() != null && !analytics.getAdditionalContext().trim().isEmpty()) {
			responseData.put("additionalContext", analytics.getAdditionalContext());
		}

		// Parse extraParams JSON if present
		if (analytics.getExtraParams() != null && !analytics.getExtraParams().isEmpty()) {
			try {
				TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
				};
				Map<String, Object> extraParamsMap = objectMapper.readValue(analytics.getExtraParams(), typeRef);
				responseData.put("extraParams", extraParamsMap);
			} catch (Exception e) {
				log.error("Failed to parse extra params JSON for ID: {}", analytics.getId(), e);
				responseData.put("extraParams", Collections.emptyMap());
			}
		} else {
			responseData.put("extraParams", Collections.emptyMap());
		}

		// Uncomment if needed
		// responseData.put("createdOn", analytics.getCreatedOn());
		// responseData.put("updatedOn", analytics.getUpdatedOn());
		return responseData;
	}

	public static String convertMapToJson(Map<String, Object> paramMap) {
		if (paramMap == null || paramMap.isEmpty()) {
			return "{}";
		}
		try {
			return objectMapper.writeValueAsString(paramMap);
		} catch (JsonProcessingException e) {
			log.error("Error converting Map to JSON string", e);
			throw new RuntimeException("Error converting Map to JSON string", e);
		}
	}
}