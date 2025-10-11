package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.playmotech.api.core.dao_postgres.ModulesActions;
import com.playmotech.api.core.dto.ModulesActionsDto;

public interface ModulesActionsRepo extends JpaRepository<ModulesActions, Long> {
	boolean existsByActionCode(String actionCode);

	@Query("""
			SELECT new com.playmotech.api.core.dto.ModulesActionsDto(m.id, m.name, new com.playmotech.api.core.dto.ActionsDto(ma.id, ma.actionName, ma.actionCode, ma.disabled))
			FROM Modules m LEFT JOIN ModulesActions ma ON ma.modules.id = m.id
			ORDER BY m.moduleSequence asc
			""")
	List<ModulesActionsDto> getModuleActions();

}
