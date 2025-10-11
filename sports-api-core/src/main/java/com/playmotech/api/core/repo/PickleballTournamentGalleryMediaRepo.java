package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.playmotech.api.core.dao_postgres.PickleballTournamentGalleryMedia;

public interface PickleballTournamentGalleryMediaRepo extends JpaRepository<PickleballTournamentGalleryMedia, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM PickleballTournamentGalleryMedia m WHERE m.pickleballTournament.id = :tournamentId AND m.id = :id")
    void deleteByTournamentIdAndId(@Param("tournamentId") String tournamentId, @Param("id") Long id);
}
