package com.playmotech.api.core.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Leads;

@Repository
public interface LeadRepo extends JpaRepository<Leads, String>, JpaSpecificationExecutor<Leads> {

//	List<Leads> findAllByIdAndInactiveIsFalse(Iterable<String> id);
	List<Leads> findByIdInAndInactiveFalse(Collection<String> ids);
}
