package com.playmotech.api.core.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.playmotech.api.core.constants.IncentiveActionType;
import com.playmotech.api.core.constants.IncentiveSourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for coach points transaction information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachPointsTransactionDto {
    private String id;
    private String coachId;
    private String coachName;
    private String academyId;
    private String academyName;
    private Integer points;
    private IncentiveActionType actionType; // EARNED, REDEEMED
    private IncentiveSourceType sourceType; // ATTENDANCE, PERFORMANCE_REPORT, etc.
    private String sourceId;
    private String description;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
