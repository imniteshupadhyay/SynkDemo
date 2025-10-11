package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class PlayerPerformanceDto {
	private String playerId;
	private String playerName;
	private Integer matchesPlayed;
	private Integer matchesWon;
	private Integer matchesTied;
	private Integer matchesLost;
	private Double winPercentage;
	private Double lossPercentage;
	private Integer totalPoints;

	// Getters and setters
}
