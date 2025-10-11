package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Team;

@Repository
public interface TeamRepo extends JpaRepository<Team, String> {
	List<Team> findAllByIdIn(List<String> ids);

	List<Team> findByAcademy_Id(String academyId);

	Optional<Team> findByAcademy_IdAndId(String academyId, String teamId);

	List<Team> findByIdIn(List<String> ids);

	List<Team> findByAcademy_IdAndBadmintonTournament_Id(String academyId, String badmintonTournamentId);

	List<Team> findByAcademy_IdAndIdIn(String academyId, List<String> id);

	List<Team> findByAcademy_IdAndIdInAndBadmintonTournament_Id(String academyId, List<String> id,
			String badmintonTournamentId);

	List<Team> findByAcademyIsNull();

	@Query("SELECT u FROM Team u WHERE u.teamName ILIKE %:searchTxt% AND u.academy IS NULL")
	List<Team> findByAcademyIsNullAndSearchByName(@Param("searchTxt") String searchTxt);

	Optional<Team> findByAcademyIsNullAndId(String teamId);

	@Query("SELECT DISTINCT t FROM Team t " + "LEFT JOIN t.players tp " + "WHERE t.createdByUserProfile.id = :userId "
			+ "   OR tp.playerUserProfile.id = :userId")
	List<Team> findTeamsByCreatorOrPlayer(@Param("userId") String userId);

	@Query("SELECT DISTINCT t FROM Team t " + "LEFT JOIN t.players tp "
			+ "WHERE t.badmintonTournament.id = :badmintonTournamentId")
	List<Team> findTeamsByTournamentId(@Param("badmintonTournamentId") String badmintonTournamentId);

	@Query(value = """
			SELECT
			    t.id AS teamId,
			    t.team_name as teamName,
			    COUNT(DISTINCT btm.match_id) AS matchesPlayed,
			    COUNT(DISTINCT CASE WHEN bmpd.winning_team_id = t.id THEN btm.match_id END) AS matchesWon,
			    COUNT(DISTINCT CASE WHEN bmpd.is_tied = TRUE THEN btm.match_id END) AS matchesTied,
			    COUNT(DISTINCT CASE
			        WHEN bmpd.winning_team_id != t.id  AND (bmpd.is_tied IS NULL OR bmpd.is_tied = FALSE)
			        THEN btm.match_id
			        END) AS matchesLost,
			    SUM(CASE WHEN bmpd.winning_team_id = t.id THEN COALESCE(bmrpd.score, 0) ELSE 0 END) AS totalPoints
			FROM badminton_team_mapping btm
			JOIN teams t ON t.id = btm.team_id
			JOIN badminton_matches bm ON bm.id = btm.match_id AND bm.badminton_tournament_id = :tournamentId
			LEFT JOIN badminton_match_play_details bmpd ON bmpd.match_id = bm.id
			LEFT JOIN badminton_match_rounds bmr ON bmr.match_play_details_id = bmpd.id
			LEFT JOIN badminton_match_round_play_details bmrpd ON bmrpd.match_round_id = bmr.id
			GROUP BY
			    t.id, t.team_name
			ORDER BY matchesWon desc
			 		""", nativeQuery = true)
	List<Object[]> getTeamPerformance(@Param("tournamentId") String tournamentId);
}
