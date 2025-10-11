package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request DTO for creating/updating a GeoFence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceRequestDto {

	private Long geoFenceId;
	private String academyId;
	private String name;
	private Double latitude;
	private Double longitude;
	private Long radiusInMeters;
	private String userType;
	private Boolean sendNotification;

}
