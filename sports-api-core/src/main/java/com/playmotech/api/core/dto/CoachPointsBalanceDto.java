package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for coach points balance information
 * Enhanced to support both organization-wide and academy-specific points tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachPointsBalanceDto {
    private String coachId;
    private String coachName;
    private String coachProfilePic;
    private Integer totalEarned;
    private Integer totalRedeemed;
    private Integer currentBalance;
    private Integer vouchersRedeemed;
    private String displayFormat; // "1 Voucher + 1000 points"
    private Integer rank; // Coach's rank in organization or academy
    
    // Added fields for dual-scope tracking
    private String organisationId; // Organization ID this balance belongs to
    private String academyId; // Academy ID (null for org-wide balances)
    private Boolean isAcademySpecific; // Whether this is an academy-specific balance
}
