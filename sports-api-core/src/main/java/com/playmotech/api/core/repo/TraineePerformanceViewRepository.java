package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.views.TraineePerformanceReportView;

public interface TraineePerformanceViewRepository extends JpaRepository<TraineePerformanceReportView, String>,
		JpaSpecificationExecutor<TraineePerformanceReportView> {

	List<TraineePerformanceReportView> findByCoachId(String coachId);

}