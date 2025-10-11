package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.Master;

public interface MasterRepository extends JpaRepository<Master, Long>, JpaSpecificationExecutor<Master> {

	List<Master> findByCategory(String category);

}
