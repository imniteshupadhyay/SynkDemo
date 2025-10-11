package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.views.CoachAttendanceView;

public interface CoachAttendanceViewRepository
		extends JpaRepository<CoachAttendanceView, Long>, JpaSpecificationExecutor<CoachAttendanceView> {


}