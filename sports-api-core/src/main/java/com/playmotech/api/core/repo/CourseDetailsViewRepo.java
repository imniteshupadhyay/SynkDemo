package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.views.CourseDetailsView;

@Repository
public interface CourseDetailsViewRepo
		extends JpaRepository<CourseDetailsView, String>, JpaSpecificationExecutor<CourseDetailsView> {

}
