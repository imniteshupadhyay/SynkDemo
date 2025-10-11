package com.playmotech.api.core.response.dao;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisSource;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;

import lombok.Data;

@Data
public class VideoAnalyticsDao {

	private Long id;
	private AnalysisSource analysisSource;
	private AnalysisStatus status;

}
