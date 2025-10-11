package com.playmotech.api.core.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.PlayerStats;
import com.playmotech.api.core.dao_postgres.TeamStats;
import com.playmotech.api.core.dao_postgres.TournamentStats;
import com.playmotech.api.core.repo.BadmintonMatchPlayDetailRepo;
import com.playmotech.api.core.services.IBadmintonStatsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BadmintonStatsService implements IBadmintonStatsService {

	private final BadmintonMatchPlayDetailRepo matchPlayDetailRepository;

	@Autowired
	public BadmintonStatsService(BadmintonMatchPlayDetailRepo matchPlayDetailRepository) {
		this.matchPlayDetailRepository = matchPlayDetailRepository;
	}

	@Override
	public List<TournamentStats> getTournamentStats(String tournamentId) {
		if (tournamentId == null || tournamentId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tournament ID cannot be null or empty");
		}

		List<TournamentStats> stats = matchPlayDetailRepository.findTournamentStats(tournamentId);

		// Sort and assign ranks
		stats.sort((a, b) -> {
			int compareMatches = b.getMatchesWon().compareTo(a.getMatchesWon());
			if (compareMatches != 0) {
				return compareMatches;
			}

			int compareRounds = b.getRoundsWon().compareTo(a.getRoundsWon());
			if (compareRounds != 0) {
				return compareRounds;
			}

			return b.getTotalPoints().compareTo(a.getTotalPoints());
		});

		// Assign ranks
		for (int i = 0; i < stats.size(); i++) {
			stats.get(i).setRank(i + 1);
		}

		return stats;
	}

	@Override
	public PlayerStats getPlayerStats(String playerId) {
		if (playerId == null || playerId.trim().isEmpty()) {
			throw new IllegalArgumentException("Player ID cannot be null or empty");
		}

		return matchPlayDetailRepository.findPlayerStats(playerId);
	}

	@Override
	public TeamStats getTeamStats(String teamId) {
		if (teamId == null || teamId.trim().isEmpty()) {
			throw new IllegalArgumentException("Team ID cannot be null or empty");
		}

		TeamStats teamStats = matchPlayDetailRepository.findTeamStats(teamId);
		return teamStats;
	}
}
