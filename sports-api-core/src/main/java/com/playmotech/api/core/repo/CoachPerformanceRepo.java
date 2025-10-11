package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachPerformanceReport;

@Repository
public interface CoachPerformanceRepo extends CrudRepository<CoachPerformanceReport, String> {
	List<CoachPerformanceReport> findByCoachUserProfile_Id(String coachUserId);
}
