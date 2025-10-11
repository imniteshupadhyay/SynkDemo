package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.PromptTemplate;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

	/**
	 * Find active prompt template by sports type
	 */
	Optional<PromptTemplate> findBySportsTypeAndDeletedFalse(SportsType sportsType);

	/**
	 * Find all active prompt templates
	 */
	List<PromptTemplate> findByDeletedFalse();

	/**
	 * Find all prompt templates for a specific sports type (including inactive)
	 */
	List<PromptTemplate> findBySportsType(SportsType sportsType);

	/**
	 * Find latest version of prompt template for a sports type
	 */
	@Query("SELECT pt FROM PromptTemplate pt WHERE pt.sportsType = :sportsType "
			+ "AND pt.deleted = false ORDER BY pt.version DESC LIMIT 1")
	Optional<PromptTemplate> findLatestBySportsType(@Param("sportsType") SportsType sportsType);

	/**
	 * Check if active prompt template exists for sports type
	 */
	boolean existsBySportsTypeAndDeletedFalse(SportsType sportsType);

	/**
	 * Find all prompt templates by created by user
	 */
	List<PromptTemplate> findByCreatedBy(String createdBy);

	/**
	 * Find prompt template by sports type and version
	 */
	Optional<PromptTemplate> findBySportsTypeAndVersion(SportsType sportsType, Integer version);
}