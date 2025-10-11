package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.views.AssessmentGlobalScoresView;

@Repository
public interface AssessmentGlobalScoresViewRepository extends 
    JpaRepository<AssessmentGlobalScoresView, String>,
    JpaSpecificationExecutor<AssessmentGlobalScoresView> {

}
