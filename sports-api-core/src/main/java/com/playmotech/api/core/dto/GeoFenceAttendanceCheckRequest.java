package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceAttendanceCheckRequest {

	private String academyId;
	private String programId;

	// Optional location fields for check-out
	private Double latitude;
	private Double longitude;
}
