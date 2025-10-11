package com.playmotech.api.core.response.dao;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for GeoFence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceDao {
    private Long geoFenceId;
    private String academyId;
    private String name;
    private Double latitude;
    private Double longitude;
    private Long radiusInMeters;
    private String userType;
    private Boolean sendNotification;
    private Instant createdOn;
    private Instant updatedOn;
}
