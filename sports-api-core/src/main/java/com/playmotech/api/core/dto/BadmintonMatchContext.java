package com.playmotech.api.core.dto;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class BadmintonMatchContext {
    private String matchId;
    private String player1;
    private String player2;
    private String team1;
    private String team2;
    private int player1Score;
    private int player2Score;
    private long currentSet;
    private int player1Sets;
    private int player2Sets;
    private boolean isDoubles;
    private String servingPlayer;
    private String receivingPlayer;
    private String lastShotType;
    private int rallyLength;
    private String courtSide; // "left" or "right"
    private Duration matchDuration;
    private List<String> recentPoints; // Last 5 points for context
    private Map<String, Integer> shotCounts; // Count of each shot type in the match
    private Map<String, Integer> errorCounts; // Count of errors by type

    private String currentPlayer;
    private String shotType;
    private String scoreTime;
}
