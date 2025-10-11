package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for tracking the current points balance for each coach
 * Includes optimistic locking via version column to prevent concurrent updates
 * <p>
 * Enhanced to support both organization-wide and academy-specific point tracking
 */
@Entity
@Table(name = "coach_points_balance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachPointsBalance {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "coach_id")
    private UserProfile coach;

    @ManyToOne
    @JoinColumn(name = "academy_id")
    private Academy academy;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(name = "total_earned")
    @Builder.Default
    private Integer totalEarned = 0;

    @Column(name = "total_redeemed")
    @Builder.Default
    private Integer totalRedeemed = 0;

    @Column(name = "current_balance")
    @Builder.Default
    private Integer currentBalance = 0;

    @Column(name = "vouchers_redeemed")
    @Builder.Default
    private Integer vouchersRedeemed = 0;

    // Flag to indicate if this is a organization-wide balance or academy-specific balance
    @Column(name = "is_academy_specific")
    @Builder.Default
    private Boolean isAcademySpecific = false;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Integer version = 0;

    @Column(name = "last_updated_at")
    @UpdateTimestamp
    private LocalDateTime lastUpdatedAt;
}
