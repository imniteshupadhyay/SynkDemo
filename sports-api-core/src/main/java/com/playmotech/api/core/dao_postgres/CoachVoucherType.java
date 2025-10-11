package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.playmotech.api.core.constants.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity for voucher types that coaches can redeem with their points
 * Enhanced to support both organization-wide and academy-specific vouchers
 */
@Entity
@Table(name = "coach_voucher_type")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachVoucherType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Currency currency = Currency.INR;

    @Column(name = "active", nullable = false)
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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
