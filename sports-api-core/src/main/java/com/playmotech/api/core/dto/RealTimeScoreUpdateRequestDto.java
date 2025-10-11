package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class RealTimeScoreUpdateRequestDto {
    // for updateRealTimeScoreOld method
    // private String matchId;
    private Long roundNumber;
    private PlayerScoreUpdateDto playerScores;
    private String winningPlayerId;
    private String winningGuestPlayerName;
    private String winningTeamId;

    // for updateRealTimeScoreOld method
    private String matchId;
    private String teamId;
    private String playerId;
    private String guestPlayerName;
    private String shotType;
    private Long score;

    // Fields for UNDO functionality
    private String requestId;
    private Boolean isUndo;

    // for client side rendering and live score stuff
    private String metadata;

    // for setting initial metadata at the start of the match
    private Boolean initial;
}
