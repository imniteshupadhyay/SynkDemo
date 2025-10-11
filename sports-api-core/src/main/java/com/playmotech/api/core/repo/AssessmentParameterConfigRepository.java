package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig;

@Repository
public interface AssessmentParameterConfigRepository extends JpaRepository<AssessmentParameterConfig, String> {

	List<AssessmentParameterConfig> findBySport(Sports sport);
}
