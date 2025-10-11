package com.playmotech.api.core.dao_postgres;

import lombok.Data;

@Data
public class PlayerStats {
	private String playerId;
	private Long matchesWon;
	private Long roundsWon;
	private Long totalRoundsPlayed;
	private Long totalPoints;
}
