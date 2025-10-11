package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.views.EnrollmentPaymentsView;

public interface EnrollmentPaymentViewRepository
		extends JpaRepository<EnrollmentPaymentsView, String>, JpaSpecificationExecutor<EnrollmentPaymentsView> {

}