package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig;

@Repository
public interface PaymentReminderConfigRepo extends JpaRepository<PaymentReminderConfig, Long>, JpaSpecificationExecutor<PaymentReminderConfig> {

    /**
     * Find all payment reminder configurations that apply to a specific academy
     * 
     * @param academyId the academy ID
     * @return list of payment reminder configurations
     */
    @Query("SELECT c FROM PaymentReminderConfig c JOIN c.academies a WHERE a.id = :academyId")
    List<PaymentReminderConfig> findByAcademyId(@Param("academyId") String academyId);

    /**
     * Find a specific payment reminder configuration by academy ID and reminder
     * type
     * 
     * @param academyId    the academy ID
     * @param reminderType the reminder type
     * @return the payment reminder configuration if found
     */
    @Query("SELECT c FROM PaymentReminderConfig c JOIN c.academies a WHERE a.id = :academyId AND c.reminderType = :reminderType")
    Optional<PaymentReminderConfig> findByAcademyIdAndReminderType(
            @Param("academyId") String academyId,
            @Param("reminderType") PaymentReminderType reminderType);

    /**
     * Find all enabled payment reminder configurations for all academies
     * 
     * @return list of enabled payment reminder configurations
     */
    List<PaymentReminderConfig> findByEnabledTrue();

    /**
     * Check if a configuration exists for a given academy and reminder type
     * 
     * @param academy      the academy entity
     * @param reminderType the reminder type
     * @return true if configuration exists
     */
    boolean existsByAcademiesContainingAndReminderType(Academy academy, PaymentReminderType reminderType);
}
