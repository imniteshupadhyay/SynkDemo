package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Payment;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, String> {
	Optional<Payment> findByIdAndTraineeCourseEnrollment_Id(String id, String enrollmentId);

	List<Payment> findByTraineeCourseEnrollment_Id(String enrollmentId);

	List<Payment> findByTraineeCourseEnrollment_IdIn(List<String> enrollmentId);

	List<Payment> findByTransactionTimeGreaterThanEqualAndTransactionTimeLessThanEqual(String startDate,
			String endDate);
}
