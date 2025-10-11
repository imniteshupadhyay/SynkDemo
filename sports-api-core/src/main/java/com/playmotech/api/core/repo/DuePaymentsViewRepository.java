package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.playmotech.api.core.views.DuePaymentsView;

public interface DuePaymentsViewRepository
		extends JpaRepository<DuePaymentsView, String>, JpaSpecificationExecutor<DuePaymentsView> {

	List<DuePaymentsView> findByTraineeUserId(String userId);

	/**
	 * Find all pending payments that are due in the specified number of days
	 *
	 * @param dueDaysCount Number of days until the payment is due (0 means today)
	 * @return List of pending payments due in the specified number of days
	 */
	List<DuePaymentsView> findByCourseDueDaysCount(Long dueDaysCount);

	/**
	 * Find all upcoming payments that are due in less than the specified number of
	 * days but not yet overdue (dueDaysCount > 0 and dueDaysCount <= maxDays)
	 *
	 * @param maxDays Maximum number of days until payment is due
	 * @return List of upcoming payments due within the specified days
	 */
	@Query("SELECT p FROM DuePaymentsView p WHERE p.courseDueDaysCount > 0 AND p.courseDueDaysCount <= :maxDays")
	List<DuePaymentsView> findUpcomingPaymentsDueWithinDays(Integer maxDays);

	/**
	 * Find all overdue payments (dueDaysCount < 0)
	 *
	 * @return List of overdue payments
	 */
	@Query("SELECT p FROM DuePaymentsView p WHERE p.courseDueDaysCount < 0")
	List<DuePaymentsView> findOverduePayments();
}
