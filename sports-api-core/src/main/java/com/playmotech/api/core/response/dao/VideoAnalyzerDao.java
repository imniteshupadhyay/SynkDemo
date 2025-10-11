package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisSource;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;

import lombok.Data;

@Data
public class VideoAnalyzerDao {
	private String id;
	private String mediaUrl;
	private String thumbnailUrl;
	private String title;
	private UserProfileDao player;
	private UserProfileDao createdBy;
	private CourseDao course;
	private AcademyDao academy;
	private boolean isAnalysed;
	private List<VideoAnalysisDao> analysis;
	private List<VideoAnalyticsDao> analytics;
	private AnalysisSource analyticsSource;
	private AnalysisStatus analyticsStatus;
	private LocalDateTime updatedOn;
	private LocalDateTime insertedOn;
}
