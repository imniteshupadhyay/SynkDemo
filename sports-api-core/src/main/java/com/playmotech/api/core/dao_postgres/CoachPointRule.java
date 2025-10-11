package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing point rules for coach incentives
 * Rules define how many points are awarded for different actions
 * Enhanced to support both organization-wide and academy-specific rules
 */
@Entity
@Table(name = "coach_point_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachPointRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academy_id")
    private Academy academy;
    
    @Column(name = "is_academy_specific")
    @Builder.Default
    private Boolean isAcademySpecific = false;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}
