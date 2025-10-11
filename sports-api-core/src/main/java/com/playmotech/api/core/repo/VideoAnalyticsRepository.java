package com.playmotech.api.core.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.dao_postgres.VideoAnalytics;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;

public interface VideoAnalyticsRepository
		extends JpaRepository<VideoAnalytics, Long>, JpaSpecificationExecutor<VideoAnalytics> {

	// Methods with delete check using Spring Data JPA naming conventions
	Optional<VideoAnalytics> findByVideoIdAndDeletedFalse(String videoId);

	Optional<VideoAnalytics> findByIdAndDeletedFalse(Long id);

	List<VideoAnalytics> findByStatusAndDeletedFalse(AnalysisStatus status);

	List<VideoAnalytics> findBySportsTypeAndDeletedFalse(SportsType sportsType);

	List<VideoAnalytics> findByDeletedFalse();

	List<VideoAnalytics> findByCreatedByAndDeletedFalse(String userId);

	Long countByStatusAndDeletedFalse(AnalysisStatus status);

	// Original methods without delete check (for backward compatibility if needed)
	Optional<VideoAnalytics> findByVideoId(String videoId);

	List<VideoAnalytics> findByStatus(AnalysisStatus status);

	List<VideoAnalytics> findBySportsType(SportsType sportsType);

	// Only use @Query for complex date range query
	@Query("SELECT va FROM VideoAnalytics va WHERE va.createdOn >= :startDate AND va.createdOn <= :endDate AND va.deleted = false")
	List<VideoAnalytics> findByDateRangeAndDeletedFalse(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT va FROM VideoAnalytics va WHERE va.createdOn >= :startDate AND va.createdOn <= :endDate")
	List<VideoAnalytics> findByDateRange(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);
}