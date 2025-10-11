package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class VideoAnalyzerDto {
    private String id;
    private String mediaUrl;
    private String thumbnailUrl;
    private String title;
    private String playerId;
    private String coachId;
    private String courseId;
    private String academyId;
    private List<VideoAnalysisDto> analysis;
}
