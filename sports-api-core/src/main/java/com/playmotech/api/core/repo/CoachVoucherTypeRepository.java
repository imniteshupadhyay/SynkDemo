package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachVoucherType;

@Repository
public interface CoachVoucherTypeRepository extends JpaRepository<CoachVoucherType, Long> {

    // Organization-specific vouchers (not academy-specific)
    List<CoachVoucherType> findByOrganisationIdAndIsAcademySpecificFalseAndActiveTrue(String organisationId);
    
    // Academy-specific vouchers
    List<CoachVoucherType> findByAcademyIdAndIsAcademySpecificTrueAndActiveTrue(String academyId);
    
    // Legacy method - returns only organization-level vouchers
    List<CoachVoucherType> findByOrganisationIdAndActiveTrue(String organisationId);

    // Global vouchers (no organization or academy)
    List<CoachVoucherType> findByOrganisationIdIsNullAndAcademyIdIsNullAndActiveTrue();
    
    // All active vouchers
    List<CoachVoucherType> findByActiveTrue();

    // Get voucher by ID if active
    Optional<CoachVoucherType> findByIdAndActiveIsTrue(Long id);
}
