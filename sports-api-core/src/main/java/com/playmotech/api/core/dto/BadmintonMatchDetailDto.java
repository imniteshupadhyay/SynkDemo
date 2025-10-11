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
public class BadmintonMatchDetailDto {
    private List<BadmintonMatchRoundDetailsDto> rounds;
    private UserProfileMinDto winningPlayerUserProfile;
    private String winningGuestPlayerName;
    private TeamDto winningTeam;
    private Boolean isTied;
}
