package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BadmintonPlayerScoreDto {
    private UserProfileMinDto playerUserProfile;
    private String guestPlayerName;
    private TeamDto team;
    private Long score;
    private Integer roundNumber;
}
