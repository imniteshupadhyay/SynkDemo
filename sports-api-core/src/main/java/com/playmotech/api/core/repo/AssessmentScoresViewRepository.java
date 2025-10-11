package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.views.AssessmentScoresView;

@Repository
public interface AssessmentScoresViewRepository extends JpaRepository<AssessmentScoresView, String> {

    /**
     * Find all assessment scores for a specific assessment, ordered by rank in descending order
     * @param assessmentId The ID of the assessment
     * @return List of assessment scores ordered by rank (highest first)
     */
    List<AssessmentScoresView> findByAssessmentIdOrderByAssessmentRankAsc(String assessmentId);
    
    /**
     * Find all assessment scores for a specific player
     * @param playerId The ID of the player
     * @return List of assessment scores for the player
     */
    List<AssessmentScoresView> findByPlayerId(String playerId);
}
