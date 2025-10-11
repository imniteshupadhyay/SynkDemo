package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.BadmintonLiveScore;

public interface BadmintonLiveScoreRepository extends JpaRepository<BadmintonLiveScore, Long> {
    List<BadmintonLiveScore> findByMatchId(String matchId);

    void deleteByMatchId(String matchId);

    Optional<BadmintonLiveScore> findByMatchIdAndPlayerId(String matchId, String playerId);

    Optional<BadmintonLiveScore> findByMatchIdAndGuestPlayerName(String matchId, String guestPlayerName);

    // New methods for UNDO functionality to retrieve score history
    List<BadmintonLiveScore> findByMatchIdAndPlayerIdOrderByLastUpdatedDesc(String matchId, String playerId);

    List<BadmintonLiveScore> findByMatchIdAndGuestPlayerNameOrderByLastUpdatedDesc(String matchId,
            String guestPlayerName);

    List<BadmintonLiveScore> findByMatchIdOrderByLastUpdatedDesc(String matchId);
    List<BadmintonLiveScore> findByPlayerId(String playerIds);
}
