package com.playmotech.api.core.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Organisation;

@Repository
public interface OrgRepo extends JpaRepository<Organisation, String> {

	Optional<Organisation> findByDomainUrlIgnoreCase(String domainUrl);

}
