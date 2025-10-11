package com.playmotech.api.core.dao_postgres;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for tracking coach activity streaks
 * Records consecutive days of activity
 */
@Entity
@Table(name = "coach_activity_streak")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachActivityStreak {
    @Id
    private String id;
    
    @Column(name = "coach_id", nullable = false)
    private String coachId;
    
    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;
    
    @Column(name = "longest_streak", nullable = false) 
    @Builder.Default
    private Integer longestStreak = 0;
    
    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;
    
    @Column(name = "streak_start_date")
    private LocalDate streakStartDate;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academy_id")
    private Academy academy;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;
    
    @Column(name = "is_academy_specific")
    @Builder.Default
    private Boolean isAcademySpecific = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
