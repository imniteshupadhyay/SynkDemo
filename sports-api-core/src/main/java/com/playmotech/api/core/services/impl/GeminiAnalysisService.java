package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.dao_postgres.PromptTemplate;
import com.playmotech.api.core.dao_postgres.VideoAnalytics;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisSource;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dto.AnalyticsItem;
import com.playmotech.api.core.dto.VideoAnalysisRequest;
import com.playmotech.api.core.repo.PromptTemplateRepository;
import com.playmotech.api.core.repo.VideoAnalyticsRepository;
import com.playmotech.api.core.repo.VideoAnalyzerRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiAnalysisService {

	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MS = 2000;

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	private final VideoAnalyticsRepository videoAnalyticsRepository;
	private final VideoAnalyzerRepo videoAnalyzerRepository;
	private final PromptTemplateRepository promptTemplateRepository;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Downloads video from URL and converts to Base64 with retry logic
	 */
	public static Optional<String> convertVideoUrlToBase64(String videoUrl) {
		if (videoUrl == null || videoUrl.trim().isEmpty()) {
			log.error("Video URL is null or empty");
			return Optional.empty();
		}

		Exception lastException = null;

		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				log.info("Attempting to download video (attempt {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, videoUrl);

				URL url = new URL(videoUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
//				connection.setConnectTimeout(180000);   // Increased: 180 sec
//				connection.setReadTimeout(180000);     // Increased: 3 minutes
				connection.connect();

				int responseCode = connection.getResponseCode();
				if (responseCode != 200) {
					log.warn("HTTP request failed with response code: {} for URL: {}", responseCode, videoUrl);
					lastException = new RuntimeException("HTTP " + responseCode);
					continue;
				}

				try (InputStream inputStream = connection.getInputStream();
						ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

					byte[] buffer = new byte[8192];
					int bytesRead;

					while ((bytesRead = inputStream.read(buffer)) != -1) {
						baos.write(buffer, 0, bytesRead);
					}

					byte[] videoBytes = baos.toByteArray();
					String base64Video = Base64.getEncoder().encodeToString(videoBytes);

					log.info("Successfully downloaded and converted video to Base64 on attempt {}", attempt);
					return Optional.of(base64Video);
				}

			} catch (Exception e) {
				lastException = e;
				log.warn("Video download attempt {}/{} failed: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

				if (attempt < MAX_RETRY_ATTEMPTS) {
					try {
						Thread.sleep(RETRY_DELAY_MS * attempt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.error("Thread interrupted during retry delay");
						return Optional.empty();
					}
				}
			}
		}

		log.error("Failed to download video after {} attempts. Last error: {}", MAX_RETRY_ATTEMPTS,
				lastException != null ? lastException.getMessage() : "Unknown error");
		return Optional.empty();
	}

	@Async("taskExecutor")
	public void processVideoAnalysisAsync(Long analysisId, VideoAnalysisRequest request) {
		log.info("Starting async analysis for ID: {} with sport: {} and additionalContext: {}", analysisId,
				request.getSportsType(), request.getAdditionalContext());

		try {
			updateAnalysisStatus(analysisId, AnalysisStatus.IN_PROGRESS, true);

			// Validate video analyzer exists
			Optional<VideoAnalyzer> videoAnalyzerOpt = videoAnalyzerRepository.findById(request.getVideoId());
			if (videoAnalyzerOpt.isEmpty()) {
				log.error("VideoAnalyzer not found for ID: {}", request.getVideoId());
				List<AnalyticsItem> fallbackResult = createFallbackAnalyticsItem(request.getSportsType(),
						"VideoAnalyzer not found - please check video upload", request.getAdditionalContext());
				saveAnalysisResult(analysisId, fallbackResult, request.getAdditionalContext());
				return;
			}

			VideoAnalyzer videoAnalyzer = videoAnalyzerOpt.get();
			String videoUrl = videoAnalyzer.getMediaUrl();

			// Validate video accessibility
			if (!isVideoFileAccessible(videoUrl)) {
				log.error("Video file not accessible at URL: {}", videoUrl);
				List<AnalyticsItem> fallbackResult = createFallbackAnalyticsItem(request.getSportsType(),
						"Video file not accessible - please re-upload the video", request.getAdditionalContext());
				saveAnalysisResult(analysisId, fallbackResult, request.getAdditionalContext());
				return;
			}

			// Convert video to Base64
			log.info("Converting video to Base64 for analysis ID: {}", analysisId);
			Optional<String> videoBase64Opt = convertVideoUrlToBase64(videoUrl);

			if (videoBase64Opt.isEmpty()) {
				log.error("Failed to convert video to Base64 for URL: {}", videoUrl);
				List<AnalyticsItem> fallbackResult = createFallbackAnalyticsItem(request.getSportsType(),
						"Failed to process video file - please try uploading again", request.getAdditionalContext());
				saveAnalysisResult(analysisId, fallbackResult, request.getAdditionalContext());
				return;
			}

			// Generate enhanced prompt with additional context
			String enhancedPrompt = generateEnhancedPromptFromDb(request.getSportsType(), request.getExtraParams(),
					request.getAdditionalContext());

			log.info("Generated enhanced prompt for analysis ID: {} with additional context integrated", analysisId);

			// Call Gemini API with enhanced prompt
			Optional<List<AnalyticsItem>> analysisResultOpt = callGeminiAPIWithRetry(videoBase64Opt.get(),
					enhancedPrompt, videoUrl, analysisId);

			// Handle results with fallback if needed
			List<AnalyticsItem> finalResult;
			if (analysisResultOpt.isEmpty()) {
				log.warn("Gemini API failed to analyze video for analysis ID: {}, using fallback", analysisId);
				finalResult = createFallbackAnalyticsItem(request.getSportsType(),
						"Gemini couldn't analyze this video. Please try again or review manually.",
						request.getAdditionalContext());
			} else {
				finalResult = analysisResultOpt.get();
				log.info("Successfully got analysis result from Gemini API for analysis ID: {}", analysisId);
			}

			// Save final result
			if (!saveAnalysisResult(analysisId, finalResult, request.getAdditionalContext())) {
				log.error("Failed to save analysis result for ID: {}", analysisId);
				updateAnalysisStatus(analysisId, AnalysisStatus.FAILED, false);
				return;
			}

			log.info("Successfully completed async analysis for ID: {} with {} results", analysisId,
					finalResult.size());

		} catch (Exception e) {
			log.error("Unexpected error processing video analysis for ID: {}", analysisId, e);
			try {
				// Enhanced fallback with context
				List<AnalyticsItem> fallbackResult = createFallbackAnalyticsItem(
						request != null ? request.getSportsType() : SportsType.CRICKET,
						"Unexpected error occurred during analysis - please try again",
						request != null ? request.getAdditionalContext() : null);
				saveAnalysisResult(analysisId, fallbackResult, request != null ? request.getAdditionalContext() : null);
			} catch (Exception fallbackException) {
				log.error("Failed to save fallback result for ID: {}", analysisId, fallbackException);
				updateAnalysisStatus(analysisId, AnalysisStatus.FAILED, false);
			}
		}
	}

	/**
	 * Enhanced prompt generation that incorporates user's additional context
	 */
	private String generateEnhancedPromptFromDb(SportsType sportsType, Map<String, Object> extraParams,
			String additionalContext) {
		try {
			log.info("Generating enhanced prompt for sports type: {} with additional context: {}", sportsType,
					additionalContext != null ? "provided" : "none");

			// Get base template from database or generate fallback
			Optional<PromptTemplate> templateOpt = promptTemplateRepository.findBySportsTypeAndDeletedFalse(sportsType);
			String baseTemplate;

			if (templateOpt.isPresent()) {
				baseTemplate = templateOpt.get().getTemplate();
				log.info("Found database template for sports type: {}", sportsType);
			} else {
				log.warn("No database template found for sports type: {}, using enhanced fallback", sportsType);
				baseTemplate = generateEnhancedFallbackTemplate(sportsType);
			}

			// Process extra parameters
			String processedTemplate = processTemplateParameters(baseTemplate, extraParams);

			// Integrate additional context from user
			String finalPrompt = integrateAdditionalContext(processedTemplate, additionalContext, sportsType);

			log.info("Enhanced prompt generation completed for sports type: {}", sportsType);
			return finalPrompt.trim();

		} catch (Exception e) {
			log.error("Error generating enhanced prompt for sports type: {}", sportsType, e);
			String fallbackPrompt = generateEnhancedFallbackTemplate(sportsType);
			return integrateAdditionalContext(fallbackPrompt, additionalContext, sportsType);
		}
	}

	/**
	 * Generates optimized fallback template for video analysis
	 */
	private String generateEnhancedFallbackTemplate(SportsType sportsType) {
		String sportName = sportsType.name().toLowerCase();
		String playerType = determinePlayerType(sportsType);
		String sportSpecificFocus = getSportSpecificFocus(sportsType);

		return String.format(
				"""
						        Analyze this %1$s video of a %3$s frame-by-frame at 1 FPS. Identify key techniques and provide coaching feedback.

						        **ANALYSIS FOCUS:**
						        - Identify significant %1$s actions and techniques
						        - Provide constructive coaching feedback focused on: %6$s
						        - Only include clearly visible actions by the %7$s
						        - Prioritize quality insights over quantity
						        - Focus on technique improvement, form correction, and specific advice

						        **EXAMPLE FORMAT:**
						        [
						            {
						                "timestampOfOutcome": "0:07.5",
						                "shotType": "action",
						                "feedback": "Detailed feedback on technique and specific improvement suggestions."
						            }
						        ]

						        Focus on %9$s-specific techniques and performance indicators relevant to %10$s improvement.
						""",
				sportName, sportName, playerType, sportName, sportName, sportSpecificFocus, playerType, sportName,
				sportName, playerType);
	}

	/**
	 * Integrates user's additional context into the prompt
	 */
	private String integrateAdditionalContext(String basePrompt, String additionalContext, SportsType sportsType) {
		if (additionalContext == null || additionalContext.trim().isEmpty()) {
			return basePrompt;
		}

		String sanitizedContext = sanitizeAdditionalContext(additionalContext);

		String contextIntegration = String.format("""

				**USER SPECIFIC FOCUS:**
				"%s"

				Incorporate this focus into your analysis while maintaining the required JSON format.
				""", sanitizedContext);

		return basePrompt + contextIntegration;
	}

	/**
	 * Sanitizes additional context to prevent prompt injection
	 */
	private String sanitizeAdditionalContext(String additionalContext) {
		if (additionalContext == null)
			return "";

		return additionalContext.trim().replaceAll("(?i)(system|assistant|user):", "") // Remove role indicators
				.replaceAll("[{}\\[\\]\"']", "") // Remove potential JSON/template characters
				.replaceAll("\\n+", " ") // Replace multiple newlines with space
				.replaceAll("\\s+", " ") // Normalize whitespace
				.substring(0, Math.min(additionalContext.length(), 500)); // Limit length
	}

	/**
	 * Enhanced fallback creation with additional context consideration
	 */
	private List<AnalyticsItem> createFallbackAnalyticsItem(SportsType sportsType, String errorReason,
			String additionalContext) {
		log.info("Creating enhanced fallback analytics item for sports type: {} with error: {}", sportsType,
				errorReason);

		AnalyticsItem fallbackItem = new AnalyticsItem();
		fallbackItem.setTimestampOfOutcome("0:00.1");

		// Set sport-specific fallback shot type
		String fallbackShotType = switch (sportsType) {
		case CRICKET -> "General Cricket Play";
		case BASKETBALL -> "General Basketball Movement";
		case FOOTBALL -> "General Football Play";
		case TENNIS -> "General Tennis Rally";
		case BASEBALL -> "General Baseball Play";
		default -> "General Sports Activity";
		};
		fallbackItem.setShotType(fallbackShotType);

		// Create enhanced contextual feedback message
		StringBuilder feedbackBuilder = new StringBuilder();
		feedbackBuilder.append("Automated analysis could not be completed for this video. ");

		if (errorReason != null && !errorReason.trim().isEmpty()) {
			feedbackBuilder.append(errorReason).append(" ");
		}

		feedbackBuilder.append("For optimal analysis results, ensure: ");
		feedbackBuilder.append("1) Clear video quality with good lighting, ");
		feedbackBuilder.append("2) Unobstructed view of the player, ");
		feedbackBuilder.append("3) Stable camera positioning. ");

		// Include user's additional context in fallback if provided
		if (additionalContext != null && !additionalContext.trim().isEmpty()) {
			feedbackBuilder.append("Note: Your specific request for '")
					.append(sanitizeAdditionalContext(additionalContext))
					.append("' will be addressed in manual review. ");
		}

		feedbackBuilder.append("Consider manual review or re-uploading with improved video conditions.");

		fallbackItem.setFeedback(feedbackBuilder.toString());

		log.info("Created enhanced fallback analytics item with context integration");
		return List.of(fallbackItem);
	}

	/**
	 * Process template parameters including nested maps
	 */
	private String processTemplateParameters(String template, Map<String, Object> extraParams) {
		String processedTemplate = template;

		if (extraParams != null && !extraParams.isEmpty()) {
			// Normalize parameters including nested maps
			Map<String, Object> normalizedParams = new HashMap<>();
			normalizedParams.putAll(extraParams);

			for (Map.Entry<String, Object> entry : extraParams.entrySet()) {
				if (entry.getValue() instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
					for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
						normalizedParams.put(nestedEntry.getKey(), nestedEntry.getValue());
					}
				}
			}

			// Replace placeholders
			for (Map.Entry<String, Object> param : normalizedParams.entrySet()) {
				String key = param.getKey();
				String value = param.getValue() != null ? param.getValue().toString() : "";
				String placeholder = "{" + key + "}";
				processedTemplate = processedTemplate.replace(placeholder, value);
			}
		}

		return processedTemplate;
	}

	/**
	 * Helper methods for sport-specific configurations
	 */
	private String determinePlayerType(SportsType sportsType) {
		return switch (sportsType) {
		case CRICKET -> "Player";
		case BASKETBALL -> "Player";
		case FOOTBALL -> "Player";
		case TENNIS -> "Player";
		case BASEBALL -> "Player";
		default -> "Player";
		};
	}

	private String getSportSpecificFocus(SportsType sportsType) {
		return switch (sportsType) {
		case CRICKET -> "batting technique, footwork, shot selection, timing, and stance";
		case BASKETBALL -> "shooting form, ball handling, court positioning, and defensive stance";
		case FOOTBALL -> "ball control, passing accuracy, shooting technique, and tactical positioning";
		case TENNIS -> "stroke technique, footwork, court positioning, and shot selection";
		case BASEBALL -> "swing mechanics, fielding technique, pitching form, and base running";
		default -> "technique, form, and positioning";
		};
	}

	/**
	 * Enhanced validation for prompt templates with context support
	 */
	public boolean validateEnhancedPromptTemplate(SportsType sportsType, Map<String, Object> extraParams,
			String additionalContext) {
		try {
			log.info("Validating enhanced prompt template for sportsType: {} with context: {}", sportsType,
					additionalContext != null ? "provided" : "none");

			String prompt = generateEnhancedPromptFromDb(sportsType, extraParams, additionalContext);

			// Check for unresolved placeholders
			boolean hasUnresolvedPlaceholders = prompt.matches(".*\\{[a-zA-Z0-9_]+\\}.*");

			// Verify context integration
			boolean contextProperlyIntegrated = additionalContext == null || additionalContext.trim().isEmpty()
					|| prompt.toLowerCase().contains("additional user instructions");

			boolean isValid = !hasUnresolvedPlaceholders && contextProperlyIntegrated;

			log.info(
					"Enhanced prompt validation result - unresolved placeholders: {}, context integrated: {}, valid: {}",
					hasUnresolvedPlaceholders, contextProperlyIntegrated, isValid);

			return isValid;

		} catch (Exception e) {
			log.error("Error validating enhanced prompt template for sports type: {}", sportsType, e);
			return false;
		}
	}

	// Optional: Add a method to check if result is a fallback
	private boolean isFallbackResult(List<AnalyticsItem> result) {
		if (result == null || result.size() != 1) {
			return false;
		}

		AnalyticsItem item = result.get(0);
		return "0:00.1".equals(item.getTimestampOfOutcome()) && item.getFeedback() != null
				&& item.getFeedback().contains("Gemini couldn't analyze");
	}

	// Optional: Add a method to retry analysis for fallback results
	public boolean retryAnalysisForFallback(Long analysisId) {
		try {
			Optional<VideoAnalytics> analysisOpt = videoAnalyticsRepository.findById(analysisId);
			if (analysisOpt.isEmpty()) {
				log.error("Analysis record not found for retry, ID: {}", analysisId);
				return false;
			}

			VideoAnalytics analysis = analysisOpt.get();
			if (analysis.getAnalysisJson() != null) {
				TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<List<AnalyticsItem>>() {
				};
				List<AnalyticsItem> currentResult = objectMapper.readValue(analysis.getAnalysisJson(), typeRef);

				if (isFallbackResult(currentResult)) {
					log.info("Detected fallback result for analysis ID: {}, queuing for retry", analysisId);
					// Reset status to pending and requeue for processing
					analysis.setStatus(AnalysisStatus.PENDING);
					analysis.setAnalysisJson(null);
					videoAnalyticsRepository.save(analysis);

					// You would need to trigger the async processing again here
					// This depends on how your system queues analysis requests
					return true;
				}
			}

			return false;
		} catch (Exception e) {
			log.error("Error retrying analysis for ID: {}", analysisId, e);
			return false;
		}
	}

	/**
	 * Calls Gemini API with retry logic and proper error handling
	 */
	private Optional<List<AnalyticsItem>> callGeminiAPIWithRetry(String videoBase64, String prompt,
			String originalVideoUrl, Long analysisId) {
		Exception lastException = null;

		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				log.info("Calling Gemini API (attempt {}/{}) for analysis ID: {}", attempt, MAX_RETRY_ATTEMPTS,
						analysisId);

				Optional<List<AnalyticsItem>> resultOpt = callGeminiAPIWithBase64(videoBase64, prompt,
						originalVideoUrl);

				if (resultOpt.isEmpty()) {
					log.warn("Gemini API returned empty result on attempt {} for analysis ID: {}", attempt, analysisId);
					lastException = new RuntimeException("Empty result from API");
				} else if (!isValidAnalysisResult(resultOpt.get())) {
					log.warn("Gemini API returned invalid result format on attempt {} for analysis ID: {}", attempt,
							analysisId);
					lastException = new RuntimeException("Invalid result format");
				} else {
					log.info("Successfully received valid response from Gemini API on attempt {} for analysis ID: {}",
							attempt, analysisId);
					return resultOpt;
				}

			} catch (Exception e) {
				lastException = e;
				String errorType = determineErrorType(e);
				log.warn("Gemini API call attempt {}/{} failed for analysis ID: {} [{}]: {}", attempt,
						MAX_RETRY_ATTEMPTS, analysisId, errorType, e.getMessage());
			}

			if (attempt < MAX_RETRY_ATTEMPTS) {
				try {
					long delayMs = calculateRetryDelay(attempt, determineErrorType(lastException));
					log.info("Waiting {} ms before retry attempt {} for analysis ID: {}", delayMs, attempt + 1,
							analysisId);
					Thread.sleep(delayMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.error("Thread interrupted during Gemini API retry delay for analysis ID: {}", analysisId);
					return Optional.empty();
				}
			}
		}

		log.error("Failed to call Gemini API after {} attempts for analysis ID: {}. Last error: {}", MAX_RETRY_ATTEMPTS,
				analysisId, lastException != null ? lastException.getMessage() : "Unknown");
		return Optional.empty();
	}

	private String determineErrorType(Exception e) {
		if (e == null)
			return "UNKNOWN";

		String message = e.getMessage();
		if (message == null)
			return "UNKNOWN";

		if (message.contains("JSON format") || message.contains("parsing") || message.contains("Invalid")
				|| message.contains("empty result")) {
			return "FORMAT_ERROR";
		} else if (message.contains("timeout") || message.contains("connection")) {
			return "NETWORK_ERROR";
		} else if (message.contains("rate limit") || message.contains("quota")) {
			return "RATE_LIMIT";
		} else if (message.contains("safety") || message.contains("blocked")) {
			return "SAFETY_FILTER";
		} else {
			return "API_ERROR";
		}
	}

	private long calculateRetryDelay(int attempt, String errorType) {
		long baseDelay = RETRY_DELAY_MS * attempt;

		return switch (errorType) {
		case "RATE_LIMIT" -> baseDelay * 2;
		case "FORMAT_ERROR" -> Math.min(baseDelay, RETRY_DELAY_MS);
		case "NETWORK_ERROR" -> baseDelay + 1000;
		default -> baseDelay;
		};
	}

	private Optional<List<AnalyticsItem>> callGeminiAPIWithBase64(String videoBase64, String prompt,
			String originalVideoUrl) {
		try {
			log.info("Calling Gemini API with Base64 video for original URL: {}", originalVideoUrl);
			log.info("Using Gemini API URL: {}", geminiApiUrl);

			Map<String, Object> requestBody = buildStructuredGeminiRequest(videoBase64, prompt, originalVideoUrl);

			// Log the request structure (omitting video data for brevity)
			try {
				ObjectMapper debugMapper = new ObjectMapper();
				// Deep clone to avoid modifying the original request
				@SuppressWarnings("unchecked")
				Map<String, Object> debugRequest = debugMapper.readValue(debugMapper.writeValueAsString(requestBody),
						new TypeReference<Map<String, Object>>() {
						});

				// Truncate base64 video data for logging
				List<Map<String, Object>> contents = (List<Map<String, Object>>) debugRequest.get("contents");
				if (contents != null && !contents.isEmpty()) {
					Map<String, Object> content = contents.get(0);
					List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
					if (parts != null && parts.size() > 1) {
						Map<String, Object> videoPart = parts.get(1);
						if (videoPart.containsKey("inlineData")) {
							Map<String, Object> inlineData = (Map<String, Object>) videoPart.get("inlineData");
							if (inlineData.containsKey("data")) {
								inlineData.put("data", "[BASE64_DATA_TRUNCATED]");
							}
						}
					}
				}

				log.info("Request structure for Gemini 2.5-flash: {}",
						debugMapper.writerWithDefaultPrettyPrinter().writeValueAsString(debugRequest));
			} catch (Exception e) {
				log.warn("Could not log request structure: {}", e.getMessage());
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Ensure we're using the correct API URL format for Gemini 2.5-flash model
			String fullUrl;
			if (geminiApiUrl.contains("generateContent")) {
				// URL already has the generateContent endpoint
				fullUrl = geminiApiUrl + "?key=" + geminiApiKey;
			} else {
				// Need to ensure the model name and endpoint are correct
				fullUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
						+ geminiApiKey;
				log.info("Using hardcoded Gemini 2.5-flash URL since configured URL may be incorrect");
			}
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

			log.info("Sending request to Gemini API...");
			ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, entity, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				log.error("Gemini API call failed with status: {}, body: {}", response.getStatusCode(),
						response.getBody());
				return Optional.empty();
			}

			log.info("Successfully received response from Gemini API with status: {}", response.getStatusCode());
			log.debug("Response first 200 chars: {}",
					response.getBody().length() > 200 ? response.getBody().substring(0, 200) + "..."
							: response.getBody());
			return parseStructuredGeminiResponse(response.getBody());

		} catch (Exception e) {
			log.error("Failed to call Gemini API for URL: {}", originalVideoUrl, e);
			return Optional.empty();
		}
	}

	private String buildSchemaEnforcedPrompt(String originalPrompt) {
		// Clean up any potential duplicate formatting instructions in the original
		// prompt
		String cleanedPrompt = originalPrompt;

		// Add only the essential reminders to ensure consistent formatting
		return cleanedPrompt + """
				FINAL REMINDER - RESPONSE FORMAT:
				- Return ONLY a valid JSON array with no text before or after
				- Each item must have exactly: "timestampOfOutcome", "shotType", "feedback"
				- Format: [{"timestampOfOutcome": "m:ss.s", "shotType": "description", "feedback": "advice"}]
				""";
	}

	private Map<String, Object> buildStructuredGeminiRequest(String videoBase64, String prompt,
			String originalVideoUrl) {
		log.info("Building request for Gemini 2.5-flash model");
		Map<String, Object> requestBody = new HashMap<>();

		List<Map<String, Object>> contents = new ArrayList<>();
		Map<String, Object> content = new HashMap<>();

		// Create parts list before adding role to ensure we never have an empty content
		// with just 'role'
		List<Map<String, Object>> parts = new ArrayList<>();

		// Add text part first (prompt)
		Map<String, Object> textPart = new HashMap<>();
		textPart.put("text", buildSchemaEnforcedPrompt(prompt));
		parts.add(textPart);

		// Add video part second
		Map<String, Object> videoPart = new HashMap<>();
		Map<String, Object> inlineData = new HashMap<>();
		inlineData.put("mimeType", determineMimeType(originalVideoUrl));
		inlineData.put("data", videoBase64);
		videoPart.put("inlineData", inlineData);
		parts.add(videoPart);

		// Add parts to content
		content.put("parts", parts);

		// Set role to "user" for Gemini 2.5-flash model requests
		content.put("role", "user");

		log.debug("Created content object with parts and role 'user'");

		// Add content to contents array
		contents.add(content);
		requestBody.put("contents", contents);

		// Configure generation parameters specifically for Gemini 2.5-flash model
		Map<String, Object> generationConfig = new HashMap<>();
		generationConfig.put("temperature", 0.2); // Slightly higher temperature for flash model
		generationConfig.put("topK", 40); // Increased for flash model
		generationConfig.put("topP", 0.9); // Adjusted for flash model
		generationConfig.put("maxOutputTokens", 2048); // Reduced for flash model

		// Important: specify JSON response format
		generationConfig.put("responseMimeType", "application/json");

		Map<String, Object> responseSchema = buildAnalyticsItemSchema();
		generationConfig.put("responseSchema", responseSchema);

		requestBody.put("generationConfig", generationConfig);
		requestBody.put("safetySettings", buildSafetySettings());

		log.debug("Built structured request for Gemini 2.5-flash model");
		return requestBody;
	}

	private Map<String, Object> buildAnalyticsItemSchema() {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "array");

		Map<String, Object> itemSchema = new HashMap<>();
		itemSchema.put("type", "object");

		Map<String, Object> properties = new HashMap<>();

		Map<String, Object> timestampProp = new HashMap<>();
		timestampProp.put("type", "string");
		timestampProp.put("description", "Timestamp in format m:ss.s (e.g., 0:07.5)");
		timestampProp.put("pattern", "^\\d{1,2}:\\d{2}\\.\\d{1}$");
		properties.put("timestampOfOutcome", timestampProp);

		Map<String, Object> shotTypeProp = new HashMap<>();
		shotTypeProp.put("type", "string");
		shotTypeProp.put("description", "Brief description of the shot or action performed");
		shotTypeProp.put("minLength", 1);
		properties.put("shotType", shotTypeProp);

		Map<String, Object> feedbackProp = new HashMap<>();
		feedbackProp.put("type", "string");
		feedbackProp.put("description", "Detailed coaching feedback and advice");
		feedbackProp.put("minLength", 10);
		properties.put("feedback", feedbackProp);

		itemSchema.put("properties", properties);
		itemSchema.put("required", Arrays.asList("timestampOfOutcome", "shotType", "feedback"));

		schema.put("items", itemSchema);

		log.debug("Built AnalyticsItem schema with strict validation");
		return schema;
	}

	private Optional<List<AnalyticsItem>> parseStructuredGeminiResponse(String jsonResponse) {
		try {
			log.debug("Processing structured response from Gemini API");
			JsonNode root = objectMapper.readTree(jsonResponse);

			// Extract candidates
			JsonNode candidatesNode = root.get("candidates");
			if (candidatesNode == null || !candidatesNode.isArray() || candidatesNode.size() == 0) {
				log.error("No candidates in response - invalid JSON format");
				return Optional.empty();
			}

			JsonNode firstCandidate = candidatesNode.get(0);
			if (firstCandidate == null) {
				log.error("First candidate is null - invalid JSON format");
				return Optional.empty();
			}

			// Check for schema validation errors
			if (firstCandidate.has("schemaValidationError")) {
				String validationError = firstCandidate.get("schemaValidationError").asText();
				log.error("Schema validation error: {}", validationError);
				return Optional.empty();
			}

			// Check finish reason
			if (firstCandidate.has("finishReason")) {
				String finishReason = firstCandidate.get("finishReason").asText();
				log.debug("Finish reason: {}", finishReason);

				if ("SAFETY".equals(finishReason)) {
					log.error("Content blocked by safety filters");
					return Optional.empty();
				}
				if ("MAX_TOKENS".equals(finishReason)) {
					log.warn("Response may be truncated - consider increasing maxOutputTokens");
				}
				if ("SCHEMA_ERROR".equals(finishReason)) {
					log.error("Response schema validation error");
					return Optional.empty();
				}
			}

			// Extract content
			JsonNode contentNode = firstCandidate.get("content");
			if (contentNode == null) {
				log.error("No content in candidate - invalid JSON format");
				return Optional.empty();
			}

			// Check for role: "model" in the response
			if (contentNode.has("role")) {
				String role = contentNode.get("role").asText();
				log.debug("Response role: {}", role);
			} else {
				log.warn("Response missing 'role' field in content");
			}

			// Validate content has more than just 'role' field
			if (contentNode.size() == 1 && contentNode.has("role")) {
				log.error("Empty content object with only 'role' field - invalid response");
				return parseContentDirectly(firstCandidate);
			}

			// Handle case where 'parts' might be missing but we have a valid candidate
			if (!contentNode.has("parts")) {
				log.warn("Response missing 'parts' field in content - attempting direct extraction");
				return parseContentDirectly(firstCandidate);
			}

			// Get parts
			JsonNode partsNode = contentNode.get("parts");
			if (partsNode == null || !partsNode.isArray() || partsNode.size() == 0) {
				log.error("Empty parts array in content - trying direct extraction");
				return parseContentDirectly(firstCandidate);
			}

			// Extract text or JSON directly
			JsonNode firstPart = partsNode.get(0);
			if (firstPart == null || firstPart.isEmpty()) {
				log.error("First part in parts array is null or empty");
				return parseContentDirectly(firstCandidate);
			}

			String jsonData;

			// Check if we have a structured response with JSON directly
			if (firstPart.has("structuredResponse") || firstPart.has("jsonResponse")) {
				JsonNode structuredData = firstPart.has("structuredResponse") ? firstPart.get("structuredResponse")
						: firstPart.get("jsonResponse");

				jsonData = objectMapper.writeValueAsString(structuredData);
				log.debug("Extracted structured JSON directly from response");
			} else if (firstPart.has("text")) {
				jsonData = firstPart.get("text").asText().trim();
				log.debug("Extracted text from response");
			} else {
				log.error("No text or structured response in parts");
				return parseContentDirectly(firstCandidate);
			}

			// Validate JSON format
			if (!isValidJsonFormat(jsonData)) {
				log.error("Extracted content is not valid JSON format");
				return parseContentDirectly(firstCandidate);
			}

			// Parse the JSON into our model objects
			try {
				TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<List<AnalyticsItem>>() {
				};
				List<AnalyticsItem> result = objectMapper.readValue(jsonData, typeRef);

				if (result == null || result.isEmpty()) {
					log.warn("Structured response returned empty result");
					return parseContentDirectly(firstCandidate);
				}

				// Validate result
				if (!isValidAnalysisResult(result)) {
					log.error("Validation failed for parsed analytics items");
					return parseContentDirectly(firstCandidate);
				}

				log.info("Successfully parsed {} structured analytics items", result.size());
				return Optional.of(result);
			} catch (Exception e) {
				log.error("Failed to parse JSON into analytics items: {}", e.getMessage());
				return parseContentDirectly(firstCandidate);
			}

		} catch (Exception e) {
			log.error("Failed to parse structured response", e);
			return parseGeminiResponseFallback(jsonResponse);
		}
	}

	private boolean isValidJsonFormat(String jsonText) {
		if (jsonText == null || jsonText.trim().isEmpty()) {
			return false;
		}

		String trimmed = jsonText.trim();

		if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
			log.warn("JSON text does not start with [ or end with ]");
			return false;
		}

		try {
			JsonNode testNode = objectMapper.readTree(trimmed);
			if (!testNode.isArray()) {
				log.warn("Parsed JSON is not an array");
				return false;
			}
			return true;
		} catch (Exception e) {
			log.warn("Failed to parse as valid JSON: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Extract analytics data directly from the candidate when the parts structure
	 * is missing
	 */
	private Optional<List<AnalyticsItem>> parseContentDirectly(JsonNode candidate) {
		try {
			log.debug("Attempting to extract data directly from candidate");

			// Check for text field at various levels
			String potentialJson = null;

			// Try to find content in different places
			if (candidate.has("content")) {
				JsonNode content = candidate.get("content");

				// Handle the case where content object only has 'role' field
				if (content.size() == 1 && content.has("role")) {
					log.debug("Content has only 'role' field, checking parent candidate for text");
					// Look for text in the candidate itself as content is empty
					if (candidate.has("text")) {
						potentialJson = candidate.get("text").asText();
						log.debug("Found text directly in candidate");
					}
				} else if (content.has("text")) {
					potentialJson = content.get("text").asText();
					log.debug("Found direct text in content.text");
				} else if (content.has("response")) {
					potentialJson = content.get("response").asText();
					log.debug("Found direct text in content.response");
				}
			} else if (candidate.has("text")) {
				potentialJson = candidate.get("text").asText();
				log.debug("Found direct text in candidate.text");
			}

			// Try examining the entire candidate as JSON
			if (potentialJson == null) {
				potentialJson = objectMapper.writeValueAsString(candidate);
				log.debug("Using entire candidate as potential JSON source");
			}

			// Extract JSON array if found
			String jsonArray = extractJsonArray(potentialJson);
			if (jsonArray == null) {
				log.error("Could not find JSON array in candidate");
				return Optional.empty();
			}

			// Parse the JSON array
			try {
				TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<List<AnalyticsItem>>() {
				};
				List<AnalyticsItem> result = objectMapper.readValue(jsonArray, typeRef);

				if (result == null || result.isEmpty()) {
					log.warn("Direct extraction produced empty result");
					return Optional.empty();
				}

				log.info("Successfully extracted {} items directly from candidate", result.size());
				return Optional.of(result);
			} catch (Exception e) {
				log.error("Failed to parse extracted JSON array: {}", e.getMessage());
				return Optional.empty();
			}

		} catch (Exception e) {
			log.error("Error in direct content parsing", e);
			return Optional.empty();
		}
	}

	private Optional<List<AnalyticsItem>> parseGeminiResponseFallback(String responseBody) {
		try {
			String jsonContent = extractJsonArray(responseBody);
			if (jsonContent == null) {
				log.error("Could not extract JSON array from response");
				return Optional.empty();
			}

			TypeReference<List<AnalyticsItem>> typeRef = new TypeReference<List<AnalyticsItem>>() {
			};
			List<AnalyticsItem> result = objectMapper.readValue(jsonContent, typeRef);

			log.info("Fallback parsing succeeded with {} items", result != null ? result.size() : 0);
			return Optional.ofNullable(result);

		} catch (Exception e) {
			log.error("Fallback parsing also failed", e);
			return Optional.empty();
		}
	}

	private String extractJsonArray(String text) {
		if (text == null)
			return null;

		int start = text.indexOf('[');
		int end = text.lastIndexOf(']');

		if (start == -1 || end == -1 || start >= end) {
			log.error("No valid JSON array found in response");
			return null;
		}

		return text.substring(start, end + 1);
	}

	private List<Map<String, Object>> buildSafetySettings() {
		List<Map<String, Object>> safetySettings = new ArrayList<>();

		String[] categories = { "HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
				"HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT" };

		for (String category : categories) {
			Map<String, Object> setting = new HashMap<>();
			setting.put("category", category);
			setting.put("threshold", "BLOCK_ONLY_HIGH");
			safetySettings.add(setting);
		}

		return safetySettings;
	}

	private String determineMimeType(String videoUrl) {
		if (videoUrl == null)
			return "video/mp4";

		String lowerUrl = videoUrl.toLowerCase();

		if (lowerUrl.endsWith(".mp4"))
			return "video/mp4";
		if (lowerUrl.endsWith(".avi"))
			return "video/x-msvideo";
		if (lowerUrl.endsWith(".mov"))
			return "video/quicktime";
		if (lowerUrl.endsWith(".wmv"))
			return "video/x-ms-wmv";
		if (lowerUrl.endsWith(".mkv"))
			return "video/x-matroska";
		if (lowerUrl.endsWith(".webm"))
			return "video/webm";
		if (lowerUrl.endsWith(".flv"))
			return "video/x-flv";
		if (lowerUrl.endsWith(".mpeg") || lowerUrl.endsWith(".mpg"))
			return "video/mpeg";
		if (lowerUrl.endsWith(".3gp"))
			return "video/3gpp";
		if (lowerUrl.endsWith(".ts"))
			return "video/mp2t";
		if (lowerUrl.endsWith(".m4v"))
			return "video/x-m4v";

		return "video/mp4";
	}

	private boolean isValidAnalysisResult(List<AnalyticsItem> result) {
		if (result == null || result.isEmpty()) {
			log.warn("Analysis result is null or empty");
			return false;
		}

		try {
			boolean isValid = true;

			// Validate against our schema requirements
			for (int i = 0; i < result.size(); i++) {
				AnalyticsItem item = result.get(i);

				if (item == null) {
					log.warn("Item at index {} is null", i);
					return false;
				}

				// Validate timestamp format: m:ss.s
				if (item.getTimestampOfOutcome() == null
						|| !item.getTimestampOfOutcome().matches("^\\d{1,3}:\\d{2}\\.\\d{1}$")) {
					log.warn("Invalid timestamp format at index {}: {}", i,
							item.getTimestampOfOutcome() != null ? item.getTimestampOfOutcome() : "null");
					isValid = false;
				}

				// Validate shotType
				if (item.getShotType() == null || item.getShotType().trim().isEmpty()) {
					log.warn("Missing shotType at index {}", i);
					isValid = false;
				}

				// Validate feedback - must be at least 10 chars
				if (item.getFeedback() == null || item.getFeedback().trim().length() < 10) {
					log.warn("Invalid feedback at index {}: too short or null", i);
					isValid = false;
				}

				// Check for potentially problematic content lengths
				if (item.getFeedback() != null && item.getFeedback().length() > 5000) {
					log.warn("Feedback at index {} is unusually long ({} chars), possible data issue", i,
							item.getFeedback().length());
					return false;
				}

				if (item.getShotType() != null && item.getShotType().length() > 200) {
					log.warn("Shot type at index {} is unusually long ({} chars), possible data issue", i,
							item.getShotType().length());
					return false;
				}
			}

			log.info("All {} structured analytics items passed validation", result.size());
			return isValid;

		} catch (Exception e) {
			log.error("Unexpected error during analysis result validation", e);
			return false;
		}
	}

	private boolean isValidTimestamp(String timestamp) {
		try {
			if (timestamp == null || timestamp.trim().isEmpty()) {
				return false;
			}

			String trimmed = timestamp.trim();

			// Match format: m:ss.s or mm:ss.s (e.g., "0:07.5", "12:34.5")
			if (!trimmed.matches("^\\d{1,2}:\\d{2}\\.\\d{1}$")) {
				return false;
			}

			// Additional validation: parse components to ensure they make sense
			String[] parts = trimmed.split(":");
			if (parts.length != 2)
				return false;

			String minutesPart = parts[0];
			String secondsPart = parts[1];

			// Validate minutes (reasonable range)
			int minutes = Integer.parseInt(minutesPart);
			if (minutes < 0 || minutes > 999) { // Allow up to 999 minutes for long videos
				return false;
			}

			// Validate seconds part (should be ss.s format)
			if (!secondsPart.matches("^\\d{2}\\.\\d{1}$")) {
				return false;
			}

			String[] secondsparts = secondsPart.split("\\.");
			int seconds = Integer.parseInt(secondsparts[0]);
			int tenths = Integer.parseInt(secondsparts[1]);

			// Validate seconds (0-59) and tenths (0-9)
			return seconds >= 0 && seconds <= 59 && tenths >= 0 && tenths <= 9;

		} catch (NumberFormatException e) {
			log.debug("Failed to parse timestamp components: {}", timestamp);
			return false;
		} catch (Exception e) {
			log.debug("Unexpected error validating timestamp: {}", timestamp, e);
			return false;
		}
	}

	private boolean isValidString(String value) {
		try {
			return value != null && !value.trim().isEmpty() && value.trim().length() > 0;
		} catch (Exception e) {
			log.debug("Unexpected error validating string value", e);
			return false;
		}
	}

	private boolean isValidFeedback(String feedback) {
		try {
			if (feedback == null || feedback.trim().isEmpty()) {
				return false;
			}

			String trimmed = feedback.trim();

			// Minimum length check
			if (trimmed.length() < 10) {
				return false;
			}

			// Check for obviously invalid content (all same character, etc.)
			if (isRepeatingPattern(trimmed)) {
				return false;
			}

			return true;

		} catch (Exception e) {
			log.debug("Unexpected error validating feedback", e);
			return false;
		}
	}

	private boolean isReasonableTimestamp(String timestamp) {
		try {
			if (timestamp == null)
				return false;

			String[] parts = timestamp.split(":");
			if (parts.length != 2)
				return false;

			int minutes = Integer.parseInt(parts[0]);

			// Reasonable limits for sports video analysis
			// Most sports clips are under 60 minutes
			if (minutes > 180) { // 3 hours seems like a reasonable max
				return false;
			}

			// Very short timestamps might indicate parsing errors
			if (minutes == 0) {
				String[] secondsParts = parts[1].split("\\.");
				int seconds = Integer.parseInt(secondsParts[0]);
				if (seconds < 1) { // Less than 1 second seems unlikely for meaningful analysis
					return false;
				}
			}

			return true;

		} catch (Exception e) {
			log.debug("Error checking timestamp reasonableness: {}", timestamp, e);
			return false;
		}
	}

	private boolean isRepeatingPattern(String text) {
		try {
			if (text == null || text.length() < 20) {
				return false; // Too short to determine pattern
			}

			// Check if more than 80% of characters are the same
			Map<Character, Integer> charCount = new HashMap<>();
			for (char c : text.toCharArray()) {
				charCount.put(c, charCount.getOrDefault(c, 0) + 1);
			}

			for (int count : charCount.values()) {
				if (count > text.length() * 0.8) {
					return true; // Repeating pattern detected
				}
			}

			return false;

		} catch (Exception e) {
			log.debug("Error checking for repeating pattern in text", e);
			return false; // Assume it's valid if we can't check
		}
	}

	private String generatePromptFromDb(SportsType sportsType, Map<String, Object> extraParams) {
		try {
			log.info("Starting prompt generation for sports type: {}", sportsType);

			Optional<PromptTemplate> templateOpt = promptTemplateRepository.findBySportsTypeAndDeletedFalse(sportsType);

			String template;
			if (templateOpt.isPresent()) {
				template = templateOpt.get().getTemplate();
				log.info("Found prompt template in database for sports type: {}", sportsType);

				if (!template.contains("timestamp_of_outcome") || !template.contains("shot_type")
						|| !template.contains("feedback")) {
					log.warn("Database template missing required field names. Enhancing template.");
					template = enhanceTemplateWithFieldRequirements(template, sportsType);
				}
			} else {
				log.warn("No prompt template found in database for sports type: {}, using fallback", sportsType);
				template = generateFallbackTemplateForSport(sportsType);
				template = enhanceTemplateWithFieldRequirements(template, sportsType);
			}

			Map<String, Object> normalizedParams = new HashMap<>();
			if (extraParams != null) {
				normalizedParams.putAll(extraParams);
				for (Map.Entry<String, Object> entry : extraParams.entrySet()) {
					if (entry.getValue() instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
						for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
							normalizedParams.put(nestedEntry.getKey(), nestedEntry.getValue());
						}
					}
				}
			}

			String prompt = template;
			if (!normalizedParams.isEmpty()) {
				for (Map.Entry<String, Object> param : normalizedParams.entrySet()) {
					String key = param.getKey();
					String value = param.getValue() != null ? param.getValue().toString() : "";
					String placeholder = "{" + key + "}";
					prompt = prompt.replace(placeholder, value);
				}
			}

			if (prompt.contains("{") && prompt.contains("}")) {
				log.warn("Prompt contains unresolved placeholders: {}", prompt);
			}

			log.info("Prompt generation completed for sports type: {}", sportsType);
			return prompt.trim();

		} catch (Exception e) {
			log.error("Error generating prompt from database for sports type: {}", sportsType, e);
			String fallbackPrompt = generateFallbackTemplateForSport(sportsType);
			log.info("Returning fallback prompt template for sports type: {}", sportsType);
			return fallbackPrompt;
		}
	}

	private String generateFallbackTemplateForSport(SportsType sportsType) {
		String sportActionExample = switch (sportsType) {
		case BASKETBALL -> "\"shot_type\": \"Three-point shot\"";
		case FOOTBALL -> "\"shot_type\": \"Free kick\"";
		case TENNIS -> "\"shot_type\": \"Backhand slice\"";
		case BASEBALL -> "\"shot_type\": \"Pitch\"";
		case CRICKET -> "\"shot_type\": \"Cover drive\"";
		default -> "\"shot_type\": \"Cover drive\"";
		};

		return """
				SYSTEM: You are an expert %s video analyst. Analyze the provided video of a {player_type} and output ONLY a JSON array.

				STRICT RULES:
				- Use EXACT keys: "timestamp_of_outcome", "shot_type", "feedback"
				- timestamp_of_outcome: format "m:ss.s" (e.g., "0:07.5")
				- All fields must be non-null, non-empty strings
				- Respond with ONLY JSON, no extra text
				- Analyze ONLY actions clearly visible and identifiable
				- DO NOT guess or exaggerate — skip unclear or obstructed moments
				- Quality over quantity: include fewer items rather than speculative ones

				EXAMPLE:
				[
				  {
				    "timestamp_of_outcome": "0:07.5",
				    %s,
				    "feedback": "Detailed coaching feedback based on clearly visible technique."
				  }
				]

				Analyze frame-by-frame and include ONLY valid actions by the {player_type}.
				"""
				.formatted(sportsType.name().toLowerCase(), sportActionExample);
	}

	private String enhanceTemplateWithFieldRequirements(String originalTemplate, SportsType sportsType) {
		String sportActionExample = switch (sportsType) {
		case BASKETBALL -> "\"shot_type\": \"Three-point shot\"";
		case FOOTBALL -> "\"shot_type\": \"Free kick\"";
		case TENNIS -> "\"shot_type\": \"Backhand slice\"";
		case BASEBALL -> "\"shot_type\": \"Pitch\"";
		case CRICKET -> "\"shot_type\": \"Cover drive\"";
		default -> "\"shot_type\": \"Cover drive\"";
		};

		String enhancement = """
				STRICT RULES:
				- EXACT keys: "timestamp_of_outcome", "shot_type", "feedback"
				- timestamp_of_outcome format: "m:ss.s" (e.g., "0:07.5")
				- All fields must be non-null, non-empty strings
				- Respond ONLY with JSON, no extra text
				- Analyze ONLY clearly visible actions; skip unclear or obstructed moments
				- No guessing, no exaggeration; quality over quantity

				EXAMPLE:
				[
				  {
				    "timestamp_of_outcome": "0:07.5",
				    %s,
				    "feedback": "Detailed coaching feedback based on clearly visible technique."
				  }
				]
				""".formatted(sportActionExample);

		return originalTemplate + enhancement;
	}

	private boolean saveAnalysisResult(Long analysisId, List<AnalyticsItem> analysisResult, String additionalContext) {
		try {
			Optional<VideoAnalytics> analysisOpt = videoAnalyticsRepository.findById(analysisId);
			if (analysisOpt.isEmpty()) {
				log.error("Analysis record not found for ID: {}", analysisId);
				return false;
			}

			VideoAnalytics analysis = analysisOpt.get();
			String analysisJson = objectMapper.writeValueAsString(analysisResult);
			analysis.setAnalysisJson(analysisJson);
			if (isFallbackResult(analysisResult)) {
				analysis.setStatus(AnalysisStatus.FAILED);
				log.info("Saving fallback result with FAILED status for analysis ID: {}", analysisId);
			} else {
				analysis.setStatus(AnalysisStatus.COMPLETED);
				log.info("Saving successful analysis result with COMPLETED status for analysis ID: {}", analysisId);
			}
			analysis.setAnalysisSource(VideoAnalytics.AnalysisSource.GEMINI);

			// Save additionalContext if provided
			if (additionalContext != null && !additionalContext.trim().isEmpty()) {
				analysis.setAdditionalContext(additionalContext);
				log.info("Saved additionalContext for analysis ID: {}", analysisId);
			}

			videoAnalyticsRepository.save(analysis);

			log.info("Saved analysis result with {} items for ID: {} (GEMINI analysis)", analysisResult.size(),
					analysisId);
			return true;

		} catch (Exception e) {
			log.error("Failed to save analysis result for ID: {}", analysisId, e);
			return false;
		}
	}

	@Transactional
	private void updateAnalysisStatus(Long analysisId, AnalysisStatus status, boolean firstAnalyzed) {
		try {
			Optional<VideoAnalytics> analysisOpt = videoAnalyticsRepository.findById(analysisId);
			if (analysisOpt.isEmpty()) {
				log.error("Analysis record not found for status update, ID: {}", analysisId);
				return;
			}

			VideoAnalytics analysis = analysisOpt.get();
			analysis.setStatus(status);
			analysis.setAnalysisSource(AnalysisSource.GEMINI);
			if (AnalysisStatus.FAILED.equals(status)) {
				analysis.setAnalysisJson(null);
				log.info("Cleared analysis JSON for failed analysis ID: {}", analysisId);
			}

			videoAnalyticsRepository.save(analysis);
			log.info("Updated analysis status to {} for ID: {}", status, analysisId);

		} catch (Exception e) {
			log.error("Failed to update status for analysis ID: {} to {}", analysisId, status, e);
		}
	}

	private boolean isVideoFileAccessible(String videoUrl) {
		try {
			if (videoUrl == null || videoUrl.trim().isEmpty()) {
				log.error("Video URL is null or empty");
				return false;
			}

			if (!videoUrl.startsWith("https://") && !videoUrl.startsWith("http://")) {
				log.error("Invalid video URL format: {}", videoUrl);
				return false;
			}

			if (videoUrl.contains("playmotech.com") || videoUrl.contains("amazonaws.com") || videoUrl.contains("s3.")
					|| videoUrl.contains("cloudfront.net") || videoUrl.contains("sta-umedia")) {

				String lowerUrl = videoUrl.toLowerCase();
				if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov")
						|| lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".webm") || lowerUrl.contains(".mp4?")
						|| lowerUrl.contains("_practice_session")
						|| lowerUrl.matches(".*\\.(mp4|avi|mov|mkv|webm).*")) {

					log.info("CDN video URL validation passed: {}", videoUrl);
					return true;
				}
			}

			log.warn("Video URL doesn't match expected CDN or video file patterns: {}", videoUrl);
			return false;

		} catch (Exception e) {
			log.error("Error checking video accessibility for: {}", videoUrl, e);
			return false;
		}
	}

	public List<SportsType> getSupportedSportsTypes() {
		try {
			List<PromptTemplate> templates = promptTemplateRepository.findByDeletedFalse();
			return templates.stream().map(PromptTemplate::getSportsType).distinct().toList();
		} catch (Exception e) {
			log.error("Error getting supported sports types from database", e);
			// Fallback to basic sports types
			return List.of(SportsType.CRICKET, SportsType.BASKETBALL);
		}
	}

	public boolean validatePromptTemplate(SportsType sportsType, Map<String, Object> extraParams) {
		try {
			log.info("validatePromptTemplate called with sportsType: {} and extraParams: {}", sportsType, extraParams);

			Map<String, Object> normalizedParams = new HashMap<>();

			if (extraParams != null) {
				normalizedParams.putAll(extraParams);

				for (Map.Entry<String, Object> entry : extraParams.entrySet()) {
					if (entry.getValue() instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
						for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
							normalizedParams.put(nestedEntry.getKey(), nestedEntry.getValue());
						}
					}
				}
			}

			log.info("Final normalized params for validation: {}", normalizedParams);
			String prompt = generatePromptFromDb(sportsType, normalizedParams);
			log.info("Generated prompt: {}", prompt);

			boolean hasUnresolvedPlaceholders = prompt.matches(".*\\{[a-zA-Z0-9_]+\\}.*");
			boolean valid = !hasUnresolvedPlaceholders;
			log.info("Using improved validation - has unresolved placeholders: {}", hasUnresolvedPlaceholders);
			log.info("Prompt validation result: {}", valid);
			return valid;

		} catch (Exception e) {
			log.error("Error validating prompt template for sports type: {}", sportsType, e);
			return false;
		}
	}

	public boolean savePromptTemplate(SportsType sportsType, String template) {
		try {
			Optional<PromptTemplate> existingOpt = promptTemplateRepository.findBySportsTypeAndDeletedFalse(sportsType);

			if (existingOpt.isPresent()) {
				PromptTemplate existing = existingOpt.get();
				existing.setTemplate(template);
				promptTemplateRepository.save(existing);
				log.info("Updated prompt template for sports type: {}", sportsType);
			} else {
				PromptTemplate newTemplate = new PromptTemplate();
				newTemplate.setSportsType(sportsType);
				newTemplate.setTemplate(template);
				newTemplate.setDeleted(false);
				promptTemplateRepository.save(newTemplate);
				log.info("Added new prompt template for sports type: {}", sportsType);
			}
			return true;

		} catch (Exception e) {
			log.error("Error saving prompt template for sports type: {}", sportsType, e);
			return false;
		}
	}

	public boolean deactivatePromptTemplate(SportsType sportsType) {
		try {
			Optional<PromptTemplate> templateOpt = promptTemplateRepository.findBySportsTypeAndDeletedFalse(sportsType);
			if (templateOpt.isPresent()) {
				PromptTemplate template = templateOpt.get();
				template.setDeleted(true);
				promptTemplateRepository.save(template);
				log.info("Deactivated prompt template for sports type: {}", sportsType);
				return true;
			} else {
				log.warn("No active template found to deactivate for sports type: {}", sportsType);
				return false;
			}
		} catch (Exception e) {
			log.error("Error deactivating prompt template for sports type: {}", sportsType, e);
			return false;
		}
	}
}