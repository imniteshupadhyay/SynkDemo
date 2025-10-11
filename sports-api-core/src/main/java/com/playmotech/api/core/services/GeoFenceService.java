package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.GeoFenceRequestDto;
import com.playmotech.api.core.response.ServiceResponse;

/**
 * Service interface for GeoFence operations
 */
public interface GeoFenceService {

	ServiceResponse createGeoFence(GeoFenceRequestDto requestDto);

	ServiceResponse updateGeoFence(GeoFenceRequestDto requestDto);

	ServiceResponse getGeoFences(String academyId);

	ServiceResponse deleteGeoFence(Long geoFenceId);
}
