package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.UpdateAnalyticsRequest;
import com.playmotech.api.core.dto.VideoAnalysisRequest;
import com.playmotech.api.core.response.ServiceResponse;

public interface VideoAnalyticsService {

	ServiceResponse submitVideoAnalytics(VideoAnalysisRequest request, boolean reanalyze);

	ServiceResponse updateVideoAnalytics(Long analyticsId, UpdateAnalyticsRequest request);

	ServiceResponse getAnalytics(Long analyticsId);

	ServiceResponse retryAnalysis(Long analyticsId);

	ServiceResponse deleteAnalytics(Long analyticsId);

	ServiceResponse getAnalyticsByVideoId(String videoId);

}
