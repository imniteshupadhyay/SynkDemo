package com.playmotech.api.core.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentStatsDto {
	private String tournamentId;
	private String tournamentName;
	private Long totalMatchesPlayed;
	private Long totalMatchesCompleted;
	private List<TeamPerformanceDTO> teamPerformances;
	private List<PlayerPerformanceDTO> topPlayerPerformances;

	@Data
	@Builder
	public static class TeamPerformanceDTO {
		private String teamId;
		private String teamName;
		private Long matchesPlayed;
		private Long matchesWon;
		private Double winPercentage;
	}

	@Data
	@Builder
	public static class PlayerPerformanceDTO {
		private String playerId;
		private String playerName;
		private Long matchesPlayed;
		private Long matchesWon;
		private Double winPercentage;
		private Integer totalPoints;
	}
}