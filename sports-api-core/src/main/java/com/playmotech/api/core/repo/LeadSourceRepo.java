package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.LeadSource;

public interface LeadSourceRepo extends JpaRepository<LeadSource, String> {
	List<LeadSource> findByNameLike(String name);
}
