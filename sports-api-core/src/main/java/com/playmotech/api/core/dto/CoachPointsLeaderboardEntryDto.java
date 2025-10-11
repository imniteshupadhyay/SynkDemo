package com.playmotech.api.core.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for coach leaderboard entries
 * Enhanced to support both organization-wide and academy-specific points
 * tracking
 * with detailed coach activity information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoachPointsLeaderboardEntryDto {
    private String coachId;
    private UserProfileMinDto coach;
    private Integer rank;
    private Integer totalPoints;
    private Integer currentBalance;
    private Integer vouchersRedeemed;
    private String displayFormat; // "1 Voucher + 1000 points"
    private AcademyMinDto academy; // Coach's primary academy
    private String academyId; // Academy ID (null for org-wide entries)

    // Added fields for dual-scope tracking
    private String organisationId; // Organization ID this entry belongs to
    private Boolean isAcademySpecific; // Whether this is an academy-specific entry

    // Enhanced coach activity metrics
    private Integer weeklyRank;
    private Integer dayStreak; // Current day streak
    private Integer longestStreak; // Longest historical streak

    // Activity details by category
    private Integer attendancePoints; // Points earned from marking attendance
    private Integer performanceReportPoints; // Points earned from performance reports
    private Integer timelinePoints; // Points earned from timeline creation
    private Integer mediaPoints; // Points earned from media analytics

    // Last activity timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActive;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinDate;

    // Coach contact information
    private String phoneNumber;
    private String email;

    // Coach specializations/sports
    private String[] specializations;
}
