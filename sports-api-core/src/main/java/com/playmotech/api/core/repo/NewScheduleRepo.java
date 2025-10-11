package com.playmotech.api.core.repo;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.NewSchedule;

public interface NewScheduleRepo extends JpaRepository<NewSchedule, Long>, JpaSpecificationExecutor<NewSchedule> {

	Optional<NewSchedule> findByIdAndDeletedIsFalse(Long id);

	boolean existsByNameAndOrganisationIdAndDeletedFalse(String name, String id);

	boolean existsByNameAndOrganisationIdAndDeletedFalseAndIdIsNot(String name, String id, Long id2);

}
