package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.dto.UpdateAnalyticsRequest;
import com.playmotech.api.core.dto.VideoAnalysisRequest;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.VideoAnalyticsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/video-analytics")
@Slf4j
public class VideoAnalyticsController {

	private final VideoAnalyticsService videoAnalyticsService;

	/**
	 * Submit a new video analytics request. POST /video-analytics
	 */
	@PostMapping
	public ResponseEntity<ServiceResponse> submitVideoAnalytics(@Valid @RequestBody VideoAnalysisRequest request,
			@RequestParam(name = "reanalyze", required = false, defaultValue = "false") boolean reanalyze) {
		log.info("Submitting analytics for videoId: {}", request.getVideoId());
		log.info("Reanalyze flag: {}", reanalyze);

		try {
			ObjectMapper mapper = new ObjectMapper();
			log.info("Received video analytics request: {}", mapper.writeValueAsString(request));
			log.info("Extra params structure: {}", mapper.writeValueAsString(request.getExtraParams()));

			// Log additionalContext if present
			if (request.getAdditionalContext() != null && !request.getAdditionalContext().trim().isEmpty()) {
				log.info("Additional context provided: {}", request.getAdditionalContext());
			}

			if (request.getExtraParams().containsKey("extraParams")) {
				log.info("Found nested extraParams structure");
				Object nestedParams = request.getExtraParams().get("extraParams");
				log.info("Nested extraParams type: {}, value: {}",
						nestedParams != null ? nestedParams.getClass().getName() : "null", nestedParams);
			}
		} catch (JsonProcessingException e) {
			log.warn("Failed to log request details", e);
		}

		ServiceResponse response = videoAnalyticsService.submitVideoAnalytics(request, reanalyze);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Retry failed analysis.
	 */
	@PostMapping("retry")
	public ResponseEntity<ServiceResponse> retryAnalysisWithParam(@RequestParam Long analyticsId) {
		log.info("Retrying analysis for analytics ID (param): {}", analyticsId);
		ServiceResponse response = videoAnalyticsService.retryAnalysis(analyticsId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Update existing video analytics. PUT /video-analytics
	 */
	@PutMapping
	public ResponseEntity<ServiceResponse> updateVideoAnalytics(@Valid @RequestBody UpdateAnalyticsRequest request) {
		log.info("Updating analytics with ID: {}", request.getAnalyticsId());
		ServiceResponse response = videoAnalyticsService.updateVideoAnalytics(request.getAnalyticsId(), request);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Retrieve analytics by videoId. GET /video-analytics?videoId=xyz
	 */
	@GetMapping
	public ResponseEntity<ServiceResponse> getAnalyticsByVideoId(@RequestParam String videoId) {
		log.info("Fetching analytics by videoId: {}", videoId);
		ServiceResponse response = videoAnalyticsService.getAnalyticsByVideoId(videoId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Retrieve analytics by analyticsId. GET /video-analytics/by-id?analyticsId=123
	 */
	@GetMapping("/by-id")
	public ResponseEntity<ServiceResponse> getAnalyticsById(@RequestParam Long analyticsId) {
		log.info("Fetching analytics by ID: {}", analyticsId);
		ServiceResponse response = videoAnalyticsService.getAnalytics(analyticsId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Delete analytics
	 */
	@DeleteMapping
	public ResponseEntity<ServiceResponse> deleteAnalyticsWithParam(@RequestParam Long analyticsId) {
		log.info("Deleting analytics with ID (param): {}", analyticsId);
		ServiceResponse response = videoAnalyticsService.deleteAnalytics(analyticsId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}
}