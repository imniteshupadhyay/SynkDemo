package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.TrialFeedback;

public interface TrialFeedbackRepo
		extends JpaRepository<TrialFeedback, String>, JpaSpecificationExecutor<TrialFeedback> {

}
