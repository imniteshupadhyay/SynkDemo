package com.playmotech.api.core.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.IncentiveActionType;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.dao_postgres.CoachPointTransaction;

@Repository
public interface CoachPointTransactionRepository extends JpaRepository<CoachPointTransaction, String> {
    
    Page<CoachPointTransaction> findByCoachIdOrderByCreatedAtDesc(String coachId, Pageable pageable);
    
    Page<CoachPointTransaction> findByCoachIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String coachId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    List<CoachPointTransaction> findBySourceTypeAndSourceId(IncentiveSourceType sourceType, String sourceId);
    
    Optional<CoachPointTransaction> findBySourceTypeAndSourceIdAndActionType(
            IncentiveSourceType sourceType, String sourceId, IncentiveActionType actionType);
    
    @Query("SELECT SUM(t.points) FROM CoachPointTransaction t WHERE t.coach.id = :coachId AND t.actionType = :actionType")
    Integer sumPointsByCoachIdAndActionType(@Param("coachId") String coachId, 
                                           @Param("actionType") IncentiveActionType actionType);
    
    @Query("SELECT COUNT(t) FROM CoachPointTransaction t WHERE t.coach.id = :coachId AND " +
           "t.sourceType = :sourceType AND t.actionType = :actionType")
    Integer countByCoachIdAndSourceTypeAndActionType(@Param("coachId") String coachId,
                                                    @Param("sourceType") IncentiveSourceType sourceType,
                                                    @Param("actionType") IncentiveActionType actionType);
}
