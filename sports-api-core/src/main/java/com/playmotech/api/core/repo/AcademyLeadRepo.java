package com.playmotech.api.core.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.AcademyLead;

@Repository
public interface AcademyLeadRepo extends CrudRepository<AcademyLead, String> {
}
