package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.PendingPaymentDueView;

public interface PendingPaymentDueViewRepository
		extends JpaRepository<PendingPaymentDueView, String>, JpaSpecificationExecutor<PendingPaymentDueView> {

	List<PendingPaymentDueView> findByTraineeUserId(String userId);

}
