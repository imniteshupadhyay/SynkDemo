package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.PendingPaymentDueView;

/**
 * Repository for accessing the course_enrollment_details_view
 * which contains information about pending payments
 */
@Repository
public interface PendingPaymentDueViewRepo extends JpaRepository<PendingPaymentDueView, String> {

    /**
     * Find all pending payments that are due in the specified number of days
     * 
     * @param dueDaysCount Number of days until the payment is due (0 means today)
     * @return List of pending payments due in the specified number of days
     */
    List<PendingPaymentDueView> findByDueDaysCount(Integer dueDaysCount);

    /**
     * Find all upcoming payments that are due in less than the specified number of
     * days
     * but not yet overdue (dueDaysCount > 0 and dueDaysCount <= maxDays)
     * 
     * @param maxDays Maximum number of days until payment is due
     * @return List of upcoming payments due within the specified days
     */
    @Query("SELECT p FROM PendingPaymentDueView p WHERE p.dueDaysCount > 0 AND p.dueDaysCount <= :maxDays")
    List<PendingPaymentDueView> findUpcomingPaymentsDueWithinDays(Integer maxDays);

    /**
     * Find all overdue payments (dueDaysCount < 0)
     * 
     * @return List of overdue payments
     */
    @Query("SELECT p FROM PendingPaymentDueView p WHERE p.dueDaysCount < 0")
    List<PendingPaymentDueView> findOverduePayments();
}
