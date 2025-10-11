package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.DaywiseActivityMapping;

public interface DaywiseActivityMappingRepo extends JpaRepository<DaywiseActivityMapping, Long>, JpaSpecificationExecutor<DaywiseActivityMapping> {

	List<DaywiseActivityMapping> findByDaywiseActivityId(Long id);

}
