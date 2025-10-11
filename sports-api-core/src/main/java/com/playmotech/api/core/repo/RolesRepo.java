package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.playmotech.api.core.dao_postgres.Roles;

public interface RolesRepo extends JpaRepository<Roles, Long> {
	boolean existsByIdAndRoleName(Long id, String roleName);

	boolean existsByRoleName(String roleName);

	boolean existsByRoleNameAndIdIsNot(String roleName, Long id);

	List<Roles> findBySequenceGreaterThan(Long sequence);

//	List<Roles> findByRoleNameLike(String roleName);

	List<Roles> findByRoleNameContainingIgnoreCase(String roleName);

	List<Roles> findByRoleName(String name);

}
