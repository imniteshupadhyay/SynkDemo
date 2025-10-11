package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceDto {
	private Long geoFenceId;
	private String name;
	private Double latitude;
	private Double longitude;
	private Long radius;
	private boolean active;
}
