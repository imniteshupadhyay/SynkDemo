package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Combined DTO for coach dashboard information including
 * points balance, rank, and activity streak
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachDashboardDto {
    // Coach information
    private String coachId;
    private String coachName;
    private String profilePictureUrl;
    
    // Points information
    private Integer totalPoints;
    private Integer currentBalance;
    private Integer vouchersRedeemed;
    private String displayFormat; // "1 Voucher + 1000 points"
    
    // Rank information
    private Integer weeklyRank;
    private Integer overallRank;
    
    // Streak information
    private Integer dayStreak;
    private Integer longestStreak;
    
    // Academy/Organization context
    private String academyId;
    private String academyName;
    private String organisationId;
}
