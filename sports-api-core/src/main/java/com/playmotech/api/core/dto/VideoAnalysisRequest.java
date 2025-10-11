package com.playmotech.api.core.dto;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;

import lombok.Data;

@Data
public class VideoAnalysisRequest {
	private String videoId;
	private SportsType sportsType;
	private String additionalContext;
	private Map<String, Object> extraParams = new HashMap<>();

	@JsonAnySetter
	public void addExtraParam(String key, Object value) {
		extraParams.put(key, value);
	}

	/**
	 * Process extraParams to handle both direct and nested parameter structures.
	 * Some clients may send parameters directly in extraParams while others might
	 * nest them under extraParams.extraParams.
	 * 
	 * @return A normalized map of parameters for template processing
	 */
	public Map<String, Object> getNormalizedExtraParams() {
		Map<String, Object> normalizedParams = new HashMap<>(extraParams);

		// Check if there's a nested structure with extraParams.extraParams
		if (extraParams.containsKey("extraParams") && extraParams.get("extraParams") instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> nestedParams = (Map<String, Object>) extraParams.get("extraParams");
			normalizedParams.putAll(nestedParams);
		}

		return normalizedParams;
	}
}
