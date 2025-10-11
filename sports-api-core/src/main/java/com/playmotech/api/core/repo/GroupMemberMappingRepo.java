package com.playmotech.api.core.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.GroupMemberMapping;

@Repository
public interface GroupMemberMappingRepo extends CrudRepository<GroupMemberMapping, Long> {
}
