package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.ScheduleFile;

@Repository
public interface ScheduleFileRepo extends JpaRepository<ScheduleFile, Long> {
}
