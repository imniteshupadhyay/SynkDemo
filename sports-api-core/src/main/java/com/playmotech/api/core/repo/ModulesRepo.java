package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.Modules;

public interface ModulesRepo extends JpaRepository<Modules, Long> {

}
