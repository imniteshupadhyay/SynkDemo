package com.playmotech.api.core.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.VideoAnalyzer;

public interface VideoAnalyzerRepo
		extends JpaRepository<VideoAnalyzer, String>, JpaSpecificationExecutor<VideoAnalyzer> {

//	Optional<VideoAnalyzer> existsByIdAndIsDeletedIsFalse(String id);

	Optional<VideoAnalyzer> findByIdAndDeletedIsFalse(String id);
}