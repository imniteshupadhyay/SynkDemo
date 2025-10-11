package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

	Optional<Activity> findByIdAndDeletedFalse(Long id);

	List<Activity> findByDeletedFalse();

	boolean existsByNameAndOrganisationIdAndDeletedFalseAndIdIsNot(String name, String organisationId, Long id);

	boolean existsByNameAndOrganisationIdAndDeletedFalse(String name, String organisationId);

	List<Activity> findByIdIn(List<Long> activityIds);
}
