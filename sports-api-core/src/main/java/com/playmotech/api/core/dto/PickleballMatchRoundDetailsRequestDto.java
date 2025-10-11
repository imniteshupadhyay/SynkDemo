package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class PickleballMatchRoundDetailsRequestDto {
    private int round;
    private String roundStartTime;
    private String roundEndTime;
    private List<PickleballPlayerScoreRequestDto> playerScores;
    private String winningPlayerUserId;
    private String winningGuestPlayerName;
    private String winningTeamId;

}
