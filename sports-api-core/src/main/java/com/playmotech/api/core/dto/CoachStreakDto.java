package com.playmotech.api.core.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for coach activity streak information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachStreakDto {
    private String coachId;
    private String coachName;
    private String coachProfilePic;
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastActivityDate;
    private LocalDate streakStartDate;
    
    // Associated contexts
    private String academyId;
    private String academyName;
    private String organisationId;
    private Boolean isAcademySpecific;
}
