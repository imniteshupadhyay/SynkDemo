package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

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
 * Entity for tracking voucher redemption requests by coaches
 */
@Entity
@Table(name = "coach_voucher_redemption")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachVoucherRedemption {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id", nullable = false)
    private UserProfile coach;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voucher_type_id", nullable = false)
    private CoachVoucherType voucherType;

    @Column(name = "points_spent", nullable = false)
    private Integer pointsSpent;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, FULFILLED

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "processed_by")
    private UserProfile processedBy;

    @Column(name = "voucher_code")
    private String voucherCode;
}
