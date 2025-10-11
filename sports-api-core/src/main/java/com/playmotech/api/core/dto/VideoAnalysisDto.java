package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class VideoAnalysisDto {

	private String id;

	private String analysedByUser;

	private String videoFrameTime;

	private String audioMediaUrl;

	private String imageUrl;

	private String comment;
}
