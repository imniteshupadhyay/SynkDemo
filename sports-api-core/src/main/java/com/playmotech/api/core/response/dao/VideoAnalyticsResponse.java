package com.playmotech.api.core.response.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisSource;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;
import com.playmotech.api.core.dto.AnalyticsItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalyticsResponse {

	private Long analyticsId;
	private String videoId;
	private Timestamp createdOn;
//	private String createdBy;
//	private Timestamp updatedOn;
//	private String updatedBy;
	private List<AnalyticsItem> analytics;
	private AnalysisStatus status;
	private SportsType sportsType;
	private Map<String, Object> extraParams;
	private AnalysisSource analysisSource;
	private String additionalContext;

}
