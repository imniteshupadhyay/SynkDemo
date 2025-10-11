package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class TournamentPerformanceDto {
	private String tournamentId;
	private String tournamentName;
	private Long totalMatchesPlayed;
	private Long totalMatchesCompleted;
	private List<TeamPerformanceDto> teamPerformances;
	private List<PlayerPerformanceDto> playerPerformances;

	// Getters and setters
}
