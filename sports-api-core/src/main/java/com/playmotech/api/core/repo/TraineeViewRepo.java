package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.views.TraineeView;

public interface TraineeViewRepo extends JpaRepository<TraineeView, String>, JpaSpecificationExecutor<TraineeView> {

}
