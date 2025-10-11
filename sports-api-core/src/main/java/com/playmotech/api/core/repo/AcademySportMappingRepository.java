package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.AcademySportMapping;

@Repository
public interface AcademySportMappingRepository extends JpaRepository<AcademySportMapping, Long> {

	List<AcademySportMapping> findByAcademyId(String academyId);
}
