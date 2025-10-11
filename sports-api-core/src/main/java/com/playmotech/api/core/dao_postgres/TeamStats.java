package com.playmotech.api.core.dao_postgres;

import lombok.Data;

@Data
public class TeamStats {
	private String teamId;
	private Long totalRoundsPlayed;
	private Long matchesWon;
	private Long totalPoints;
	private Long highestScore;
}
