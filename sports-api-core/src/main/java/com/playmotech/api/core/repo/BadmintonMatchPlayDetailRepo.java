package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonMatch;
import com.playmotech.api.core.dao_postgres.BadmintonMatchPlayDetail;
import com.playmotech.api.core.dao_postgres.PlayerStats;
import com.playmotech.api.core.dao_postgres.TeamStats;
import com.playmotech.api.core.dao_postgres.TournamentStats;
import com.playmotech.api.core.dto.PlayerMatchStatsDto;

@Repository
public interface BadmintonMatchPlayDetailRepo extends CrudRepository<BadmintonMatchPlayDetail, Long> {
	Optional<BadmintonMatchPlayDetail> findByBadmintonMatch_Id(String matchId);

	List<BadmintonMatchPlayDetail> findAllByBadmintonMatch_IdIn(List<String> matchIds);

	@Query("""
			SELECT new com.playmotech.api.core.dao_postgres.TournamentStats(
			    COALESCE(bmpd.winningPlayerUserProfile.id, bmpd.winningTeam.id, bmpd.winningGuestPlayerName) as participantId,
			    CASE
			        WHEN bmpd.winningPlayerUserProfile IS NOT NULL THEN 'PLAYER'
			        WHEN bmpd.winningTeam IS NOT NULL THEN 'TEAM'
			        ELSE 'GUEST'
			    END as participantType,
			    COUNT(DISTINCT bmpd.id) as matchesWon,
			    COUNT(DISTINCT bmr.id) as roundsWon,
			    SUM(bmrpd.score) as totalPoints)
			FROM BadmintonMatchPlayDetail bmpd
			LEFT JOIN bmpd.badmintonMatchRounds bmr
			LEFT JOIN bmr.badmintonMatchRoundsPlayDetails bmrpd
			WHERE bmpd.tournament.id = :tournamentId
			GROUP BY
			    COALESCE(bmpd.winningPlayerUserProfile.id, bmpd.winningTeam.id, bmpd.winningGuestPlayerName),
			    CASE
			        WHEN bmpd.winningPlayerUserProfile IS NOT NULL THEN 'PLAYER'
			        WHEN bmpd.winningTeam IS NOT NULL THEN 'TEAM'
			        ELSE 'GUEST'
			    END
			""")
	List<TournamentStats> findTournamentStats(@Param("tournamentId") String tournamentId);

	@Query("""
			SELECT new com.playmotech.api.core.dao_postgres.PlayerStats(
			    up.id as playerId,
			    COUNT(DISTINCT bmpd.id) as matchesWon,
			    COUNT(DISTINCT bmr.id) as roundsWon,
			    (SELECT COUNT(DISTINCT bmr2.id)
			     FROM BadmintonMatchRound bmr2
			     JOIN bmr2.badmintonMatchRoundsPlayDetails bmrpd2
			     WHERE bmrpd2.playerUserProfile.id = :playerId) as totalRoundsPlayed,
			    SUM(bmrpd.score) as totalPoints)
			FROM UserProfile up
			LEFT JOIN BadmintonMatchPlayDetail bmpd ON bmpd.winningPlayerUserProfile.id = up.id
			LEFT JOIN bmpd.badmintonMatchRounds bmr
			LEFT JOIN bmr.badmintonMatchRoundsPlayDetails bmrpd ON bmrpd.playerUserProfile.id = up.id
			WHERE up.id = :playerId
			GROUP BY up.id
			""")
	PlayerStats findPlayerStats(@Param("playerId") String playerId);

	@Query("""
			SELECT new com.playmotech.api.core.dao_postgres.TeamStats(
			    t.id as teamId,
			                         COUNT(DISTINCT CASE WHEN bmpd.winningTeam.id = :teamId THEN bmpd.id END) as matchesWon,
			                         COUNT(DISTINCT CASE WHEN bmrpd.team.id = :teamId THEN bmr.id END) as totalRoundsPlayed,
			                         COALESCE(SUM(CASE WHEN bmrpd.team.id = :teamId THEN bmrpd.score END), 0) as totalPoints,
			                         COALESCE(MAX(CASE WHEN bmrpd.team.id = :teamId THEN bmrpd.score END), 0) as highestScore)
			                     FROM Team t
			                     LEFT JOIN BadmintonMatchPlayDetail bmpd ON bmpd.winningTeam.id = t.id\s
			                         OR bmpd.id IN (
			                             SELECT DISTINCT bmpd2.id\s
			                             FROM BadmintonMatchPlayDetail bmpd2\s
			                             JOIN bmpd2.badmintonMatchRounds bmr2\s
			                             JOIN bmr2.badmintonMatchRoundsPlayDetails bmrpd2\s
			                             WHERE bmrpd2.team.id = :teamId
			                         )
			                     LEFT JOIN bmpd.badmintonMatchRounds bmr
			                     LEFT JOIN bmr.badmintonMatchRoundsPlayDetails bmrpd
			                     WHERE t.id = :teamId
			                     GROUP BY t.id
			""")
	TeamStats findTeamStats(@Param("teamId") String teamId);

	@Query(value = """
			SELECT new com.playmotech.api.core.dto.PlayerMatchStatsDto(
			    :playerUserId,
			    COUNT(DISTINCT bm.id),
			    SUM(CASE
			        WHEN bmpd.winningPlayerUserProfile.id = :playerUserId
			             OR (bmtp.id IS NOT NULL AND bmpd.winningTeam.id = bmtp.team.id)
			        THEN 1
			        ELSE 0
			    END),
			    SUM(CASE
			        WHEN COALESCE(bmpd.isTied, false) = false
			             AND (
			               (bmpd.winningPlayerUserProfile.id IS NOT NULL AND bmpd.winningPlayerUserProfile.id != :playerUserId)
			               OR (bmpd.winningGuestPlayerName IS NOT NULL)
			               OR (bmpd.winningTeam.id IS NOT NULL AND (bmtp.id IS NULL OR bmpd.winningTeam.id <> bmtp.team.id))
			             )
			        THEN 1
			        ELSE 0
			    END),
			    SUM(CASE
			        WHEN COALESCE(bmpd.isTied, false) = true
			        THEN 1
			        ELSE 0
			    END),
			    SUM(CASE
			        WHEN bmpd.winningPlayerUserProfile.id IS NULL
			             AND bmpd.winningGuestPlayerName IS NULL
			             AND bmpd.winningTeam.id IS NULL
			             AND COALESCE(bmpd.isTied, false) = false
			        THEN 1
			        ELSE 0
			    END)
			)
			FROM BadmintonMatch bm
			LEFT JOIN BadmintonSinglesPlayerMapping bspm ON bspm.badmintonMatch.id = bm.id AND bspm.playerUserProfile.id = :playerUserId
			LEFT JOIN BadmintonMatchTeamPlayerMapping bmtp ON bmtp.badmintonMatch.id = bm.id AND bmtp.playerUserProfile.id = :playerUserId
			LEFT JOIN BadmintonMatchPlayDetail bmpd ON bm.id = bmpd.badmintonMatch.id
			WHERE (bspm.id IS NOT NULL OR bmtp.id IS NOT NULL)
			AND bm.matchStatus = 'ENDED'
			AND COALESCE(bm.inactive, false) = false
			""")
	PlayerMatchStatsDto getPlayerMatchStatistics(@Param("playerUserId") String playerUserId);

	// Repository method with status calculation
	@Query("""
    SELECT bm
    FROM BadmintonMatch bm
    LEFT JOIN BadmintonSinglesPlayerMapping bspm ON bspm.badmintonMatch.id = bm.id AND bspm.playerUserProfile.id = :userId
    LEFT JOIN BadmintonMatchTeamPlayerMapping bmtp ON bmtp.badmintonMatch.id = bm.id AND bmtp.playerUserProfile.id = :userId
    LEFT JOIN BadmintonMatchPlayDetail bmpd ON bm.id = bmpd.badmintonMatch.id
    WHERE (bspm.id IS NOT NULL OR bmtp.id IS NOT NULL)
    AND bm.matchStatus = 'ENDED'
    AND COALESCE(bm.inactive, false) = false
    AND (
        bmpd.isTied = true
        OR bmpd.winningPlayerUserProfile IS NOT NULL
        OR bmpd.winningTeam IS NOT NULL
    )
    ORDER BY bm.createdAtTimestampUtc DESC
    """)
	List<BadmintonMatch> findCompletedMatchesForUser(@Param("userId") String userId);
}
