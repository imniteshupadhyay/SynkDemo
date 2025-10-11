package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.VideoAnalysis;

public interface VideoAnalysisRepo
		extends JpaRepository<VideoAnalysis, String>, JpaSpecificationExecutor<VideoAnalysis> {

}