package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.RolesUserCount;

public interface RolesUserCountRepo extends JpaRepository<RolesUserCount, Long> {

}
