package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class PickleballMatchDetailRequestDto {
    private List<PickleballMatchRoundDetailsRequestDto> rounds;
    private String winningPlayerUserId;
    private String winningGuestPlayerName;
    private String winningTeamId;
    private Boolean isTied;
}
