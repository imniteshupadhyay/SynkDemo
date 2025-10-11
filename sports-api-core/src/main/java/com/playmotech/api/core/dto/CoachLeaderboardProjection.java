package com.playmotech.api.core.dto;

import java.time.LocalDateTime;

import com.playmotech.api.core.dao_postgres.UserProfile;

public interface CoachLeaderboardProjection {
    String getCoachId();

    UserProfile getCoach();

    Long getTotalPoints();

    LocalDateTime getLastUpdatedAt();
}
