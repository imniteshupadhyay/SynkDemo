package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.dao_postgres.PaymentLedger;

public interface PaymentLedgerRepository
		extends JpaRepository<PaymentLedger, Long>, JpaSpecificationExecutor<PaymentLedger> {

	List<PaymentLedger> findByEnrollment_Id(String enrollmentId);

	List<PaymentLedger> findByEnrollment_IdAndCategory(String id, PaymentCategory registrationFee);

}