package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonTournamentPlayer;

import jakarta.transaction.Transactional;

@Repository
public interface BadmintonTournamentPlayerRepo extends CrudRepository<BadmintonTournamentPlayer, Long> {
	List<BadmintonTournamentPlayer> findByBadmintonTournament_Id(String tournamentId);

	@Modifying
	@Transactional
	@Query("DELETE FROM BadmintonTournamentPlayer m WHERE m.playerUserProfile.id = :playerUserId AND m.badmintonTournament.id = :tournamentId")
	void deletePlayerTournamentMapping(@Param("playerUserId") String playerUserId,
			@Param("tournamentId") String tournamentId);
}
