package com.playmotech.api.core.response.dao;

import lombok.Data;

@Data
public class VideoAnalysisDao {

	private UserProfileDao analysedByUser;

	private String videoFrameTime;

	private String audioMediaUrl;

	private String imageUrl;

	private String comment;
}
