package com.playmotech.api.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PickleballPlayerScoreDto {
    private UserProfileMinDto playerUserProfile;
    private String guestPlayerName;
    private PickleballTeamDto team;
    private Long score;
    private Integer roundNumber;
}
