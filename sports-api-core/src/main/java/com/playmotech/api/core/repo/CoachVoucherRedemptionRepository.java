package com.playmotech.api.core.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachVoucherRedemption;

@Repository
public interface CoachVoucherRedemptionRepository extends JpaRepository<CoachVoucherRedemption, String> {
    
    Page<CoachVoucherRedemption> findByCoachIdOrderByRequestedAtDesc(String coachId, Pageable pageable);
    
    List<CoachVoucherRedemption> findByStatusOrderByRequestedAtAsc(String status);
    
    Page<CoachVoucherRedemption> findByStatusAndRequestedAtBetweenOrderByRequestedAtDesc(
            String status, LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    Integer countByCoachIdAndStatus(String coachId, String status);
}
