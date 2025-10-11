package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PickleballMatchRoundDetailsDto {
    private int round;
    private String roundStartTime;
    private String roundEndTime;
    private List<PickleballPlayerScoreDto> playerScores;
    private UserProfileMinDto winningPlayerUserProfile;
    private String winningGuestPlayerName;
    private PickleballTeamDto winningTeam;
}
