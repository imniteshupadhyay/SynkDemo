package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class TeamPerformanceDto {
	private String teamId;
	private String teamName;
	private Integer matchesPlayed;
	private Integer matchesWon;
	private Integer matchesTied;
	private Integer matchesLost;
	private Double winPercentage;
	private Double lossPercentage;
	private Integer totalPoints;
}
