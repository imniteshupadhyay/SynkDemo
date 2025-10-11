package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.dao_postgres.PickleballTournamentPlayer;

import jakarta.transaction.Transactional;

public interface PickleTournamentPlayerRepo extends JpaRepository<PickleballTournamentPlayer, Long> {
    List<PickleballTournamentPlayer> findByPickleballTournament_Id(String tournamentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PickleballTournamentPlayer  m WHERE m.playerUserProfile.id = :playerUserId AND m.pickleballTournament.id = :tournamentId")
    void deletePlayerTournamentMapping(@Param("playerUserId") String playerUserId, @Param("tournamentId") String tournamentId);

}
