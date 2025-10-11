package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PickleballMatchDetailDto {
    private List<PickleballMatchRoundDetailsDto> rounds;
    private UserProfileMinDto winningPlayerUserProfile;
    private String winningGuestPlayerName;
    private PickleballTeamDto winningTeam;
    private Boolean isTied;

}
