package com.playmotech.api.core.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadmintonMatchRoundDetailsDto {
    private int round;
    private String roundStartTime;
    private String roundEndTime;
    private List<BadmintonPlayerScoreDto> playerScores;
    private UserProfileMinDto winningPlayerUserProfile;
    private String winningGuestPlayerName;
    private TeamDto winningTeam;
}
