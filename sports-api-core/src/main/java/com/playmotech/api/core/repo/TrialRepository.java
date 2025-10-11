package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;

public interface TrialRepository extends JpaRepository<Trial, String>, JpaSpecificationExecutor<Trial> {
	Optional<Trial> findByIdAndDeletedFalse(String id);

	Optional<Trial> findByIdAndDeletedFalseAndStatus(String id, TrialStatus status);

	List<Trial> findByStatus(TrialStatus status);

	List<Trial> findByLeadIdIn(List<String> leadIds);

}
