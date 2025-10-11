package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dao_postgres.PlayerStats;
import com.playmotech.api.core.dao_postgres.TeamStats;
import com.playmotech.api.core.dao_postgres.TournamentStats;

public interface IBadmintonStatsService {
	List<TournamentStats> getTournamentStats(String tournamentId);

	PlayerStats getPlayerStats(String playerId);

	TeamStats getTeamStats(String teamId);
}
