package com.playmotech.api.core.dao_postgres;

import lombok.Data;

@Data
public class TournamentStats {
	private String participantId;
	private String participantType;
	private Long matchesWon;
	private Long roundsWon;
	private Long totalPoints;
	private Integer rank;
}
