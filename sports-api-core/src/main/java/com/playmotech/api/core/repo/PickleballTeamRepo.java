package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.PickleballTeam;
import com.playmotech.api.core.dao_postgres.PickleballTournament;

@Repository
public interface PickleballTeamRepo extends JpaRepository<PickleballTeam, String> {
        List<PickleballTeam> findByAcademy_Id(String academyId);

        List<PickleballTeam> findByAcademyIsNull();

        List<PickleballTeam> findByIdIn(List<String> ids);

        Optional<PickleballTeam> findByIdAndAcademy_Id(String id, String academyId);

        Optional<PickleballTeam> findByIdAndAcademyIsNull(String id);

        List<PickleballTeam> findByPickleballTournament(PickleballTournament tournament);

        List<PickleballTeam> findByPickleballTournament_Id(String tournamentId);

        List<PickleballTeam> findByTeamNameContainingIgnoreCaseAndAcademy_Id(String teamName, String academyId);

        List<PickleballTeam> findByTeamNameContainingIgnoreCaseAndAcademyIsNull(String teamName);

        List<PickleballTeam> findByAcademy_IdAndPickleballTournament_Id(String academyId,
                        String pickleBallTournamentId);

        @Query("SELECT DISTINCT t FROM PickleballTeam t " + "LEFT JOIN t.players tp "
                        + "WHERE t.pickleballTournament.id = :tournamentId")
        List<PickleballTeam> findTeamsByTournamentId(@Param("tournamentId") String tournamentId);

        @Query("SELECT DISTINCT t FROM PickleballTeam t " + "LEFT JOIN t.players tp "
                        + "WHERE t.createdByUserProfile.id = :userId "
                        + "OR tp.playerUserProfile.id = :userId")
        List<PickleballTeam> findTeamsByCreatorOrPlayer(@Param("userId") String userId);

        Optional<PickleballTeam> findByAcademyIsNullAndId(String teamId);

        @Query(value = """
                             SELECT t.id AS teamId,
                                    t.team_name as teamName,
                                    COUNT(DISTINCT btm.match_id) AS matchesPlayed,
                                    COUNT(DISTINCT CASE WHEN bmpd.winning_team_id = t.id THEN btm.match_id END) AS matchesWon,
                             	   COUNT(DISTINCT CASE WHEN bmpd.is_tied = TRUE THEN btm.match_id END) AS matchesTied,
                                    COUNT(DISTINCT CASE
                            WHEN bmpd.winning_team_id != t.id  AND (bmpd.is_tied IS NULL OR bmpd.is_tied = FALSE)
                            THEN btm.match_id
                            END) AS matchesLost,
                        SUM(CASE WHEN bmpd.winning_team_id = t.id THEN COALESCE(bmrpd.score, 0) ELSE 0 END) AS totalPoints
                        FROM pickleball_team_mapping btm
                        JOIN pickleball_teams t ON t.id = btm.team_id
                        JOIN pickleball_matches bm ON bm.id = btm.match_id AND bm.pickleball_tournament_id = :tournamentId
                        LEFT JOIN pickleball_match_play_details bmpd ON bmpd.match_id = bm.id
                        LEFT JOIN pickleball_match_rounds bmr ON bmr.match_play_details_id = bmpd.id
                        LEFT JOIN pickleball_match_round_play_details bmrpd ON bmrpd.match_round_id = bmr.id
                        GROUP BY t.id, t.team_name
                        ORDER BY matchesWon DESC
                        """, nativeQuery = true)
        List<Object[]> getTeamPerformance(@Param("tournamentId") String tournamentId);
}
