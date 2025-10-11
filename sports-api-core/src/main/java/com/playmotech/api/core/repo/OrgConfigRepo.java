package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.playmotech.api.core.dao_postgres.OrganisationConfig;

public interface OrgConfigRepo extends JpaRepository<OrganisationConfig, Long> {

	Optional<OrganisationConfig> findByOrgIdAndKey(String orgId, String key);

	List<OrganisationConfig> findByOrgIdInAndKey(Set<String> orgIds, String key);

	@Query("SELECT COALESCE(MAX(o.id), 0) FROM OrganisationConfig o")
	Long findMaxId();
}
