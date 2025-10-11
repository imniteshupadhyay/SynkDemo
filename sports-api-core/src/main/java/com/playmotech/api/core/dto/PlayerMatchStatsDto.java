package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PlayerMatchStatsDto {
    private String playerUserId;
    private Long totalMatchesPlayed;
    private Long matchesWon;
    private Long matchesLost;
    private Long matchesTied;
    private Long matchesWithoutResult;
}
