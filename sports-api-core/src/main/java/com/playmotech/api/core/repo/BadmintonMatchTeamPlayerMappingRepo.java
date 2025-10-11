package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.BadmintonMatchTeamPlayerMapping;

public interface BadmintonMatchTeamPlayerMappingRepo extends JpaRepository<BadmintonMatchTeamPlayerMapping, Long> {

}
