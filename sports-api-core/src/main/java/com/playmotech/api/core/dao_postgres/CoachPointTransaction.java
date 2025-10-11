package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.playmotech.api.core.constants.IncentiveActionType;
import com.playmotech.api.core.constants.IncentiveSourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity for storing coach incentive point transactions
 * Records every earning and spending of points
 */
@Entity
@Table(name = "coach_point_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachPointTransaction {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id", nullable = false)
    private UserProfile coach;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academy_id")
    private Academy academy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    // Flag to indicate if this transaction applies to a specific academy or organization-wide
    @Column(name = "is_academy_specific")
    @Builder.Default
    private Boolean isAcademySpecific = false;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IncentiveActionType actionType;

    @Column(name = "source_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IncentiveSourceType sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private UserProfile createdBy;
}
