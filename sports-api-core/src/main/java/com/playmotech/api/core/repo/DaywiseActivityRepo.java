package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.DaywiseActivity;

public interface DaywiseActivityRepo extends JpaRepository<DaywiseActivity, Long>, JpaSpecificationExecutor<DaywiseActivity> {

	List<DaywiseActivity> findByScheduleId(Long id);

}
