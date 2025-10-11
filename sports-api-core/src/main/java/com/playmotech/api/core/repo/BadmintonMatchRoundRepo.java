package com.playmotech.api.core.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonMatchRound;

@Repository
public interface BadmintonMatchRoundRepo extends CrudRepository<BadmintonMatchRound, Long> {

}
