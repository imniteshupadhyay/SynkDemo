package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonSinglesPlayerMapping;

@Repository
public interface BadmintonSinglesPlayerMappingRepo extends CrudRepository<BadmintonSinglesPlayerMapping, Long> {

	@Query(value = """
			SELECT
			    pp.guest_name AS guestPlayerName,
			    COUNT(DISTINCT pp.match_id) AS matchesPlayed,
			    COUNT(DISTINCT md.match_id) AS matchesWon,
			    COUNT(DISTINCT CASE
			        WHEN (bmpd.winning_guest_player_name != pp.guest_name or bmpd.winning_player_id is not null)
			             AND (bmpd.is_tied IS NULL OR bmpd.is_tied = FALSE)
			        THEN pp.match_id
			    END) AS matchesLost,
			    COUNT(DISTINCT CASE WHEN bmpd.is_tied = TRUE THEN pp.match_id END) AS matchesTied,
			    COALESCE(SUM(md.total_points), 0) AS totalPoints
			FROM
			    badminton_singles_player_mapping pp
			JOIN badminton_matches bm ON bm.id = pp.match_id
			JOIN badminton_match_play_details bmpd ON bmpd.match_id = bm.id
			LEFT JOIN (
			    SELECT
			        bm.id AS match_id,
			        bmpd.winning_guest_player_name,
			        COALESCE(SUM(bmrpd.score), 0) AS total_points
			    FROM badminton_matches bm
			    JOIN badminton_match_play_details bmpd ON bmpd.match_id = bm.id
			    LEFT JOIN badminton_match_rounds bmr ON bmr.match_play_details_id = bmpd.id
			    LEFT JOIN badminton_match_round_play_details bmrpd ON bmrpd.match_round_id = bmr.id
			    WHERE bm.badminton_tournament_id = :tournamentId
			      AND bmpd.winning_player_id IS NULL
			    GROUP BY bm.id, bmpd.winning_guest_player_name
			) md ON pp.match_id = md.match_id AND pp.guest_name = md.winning_guest_player_name
			WHERE
			    bm.badminton_tournament_id = :tournamentId
			    AND pp.player_user_id IS NULL
			GROUP BY
			    pp.guest_name
			ORDER BY
			    matchesWon DESC, totalPoints DESC
			LIMIT :limit
			  		""", nativeQuery = true)
	List<Object[]> getGuestPlayerPerformance(@Param("tournamentId") String tournamentId, @Param("limit") int limit);

	@Query(value = """
			SELECT
			    up.id AS playerId,
			    up.display_name AS playerName,
			    COUNT(DISTINCT bspm.match_id) AS matchesPlayed,
			    COUNT(DISTINCT CASE WHEN bmpd.winning_player_id = up.id THEN bspm.match_id END) AS matchesWon,
			    COUNT(DISTINCT CASE
			        WHEN bmpd.winning_player_id != up.id  AND (bmpd.is_tied IS NULL OR bmpd.is_tied = FALSE)
			        THEN bspm.match_id
			        END) AS matchesLost,
			    COUNT(DISTINCT CASE WHEN bmpd.is_tied = TRUE THEN bspm.match_id END) AS matchesTied,
			    SUM(CASE WHEN bmpd.winning_player_id = up.id THEN COALESCE(bmrpd.score, 0) ELSE 0 END) AS totalPoints
			FROM badminton_singles_player_mapping bspm
			LEFT JOIN user_profiles up ON up.id = bspm.player_user_id
			LEFT JOIN badminton_match_play_details bmpd ON bmpd.match_id = bspm.match_id
			LEFT JOIN badminton_match_rounds bmr ON bmr.match_play_details_id = bmpd.id
			LEFT JOIN badminton_match_round_play_details bmrpd ON bmrpd.match_round_id = bmr.id
			WHERE bspm.match_id IN (
			    SELECT id FROM badminton_matches WHERE badminton_tournament_id = :tournamentId
			) AND bspm.guest_name IS NULL
			GROUP BY up.id, up.display_name
			ORDER BY matchesWon DESC, totalPoints DESC
			LIMIT :limit
			    		""", nativeQuery = true)
	List<Object[]> getPlayerPerformance(@Param("tournamentId") String tournamentId, @Param("limit") int limit);
}
