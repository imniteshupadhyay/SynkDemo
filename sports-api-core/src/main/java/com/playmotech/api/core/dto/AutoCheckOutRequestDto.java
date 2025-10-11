package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for automatic check-out requests when coach exits geo-fence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoCheckOutRequestDto {

	private Double latitude;

	private Double longitude;
}
