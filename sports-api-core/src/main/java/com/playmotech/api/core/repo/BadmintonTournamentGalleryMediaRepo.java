package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonTournamentGalleryMedia;

import jakarta.transaction.Transactional;

@Repository
public interface BadmintonTournamentGalleryMediaRepo extends CrudRepository<BadmintonTournamentGalleryMedia, Long> {
	@Modifying
	@Transactional
	@Query("DELETE FROM BadmintonTournamentGalleryMedia m WHERE m.badmintonTournament.id = :tournamentId AND m.id = :id")
	void deleteByTournamentIdAndId(@Param("tournamentId") String tournamentId, @Param("id") Long id);
}
