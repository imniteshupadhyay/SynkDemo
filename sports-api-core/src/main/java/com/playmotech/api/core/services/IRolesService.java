package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dao_postgres.Modules;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.RolesUserCount;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.ModulesActionsDto;
import com.playmotech.api.core.dto.RoleByIdResponseDto;
import com.playmotech.api.core.dto.RolesRequestDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IRolesService {
	RoleByIdResponseDto getRoleById(String id) throws ResourceException;

	String addEditRole(RolesRequestDto roleReq) throws ResourceException;

	List<Modules> getModulesMasterList() throws ResourceException;

	List<ModulesActionsDto> getModulesActionsList();

	List<RolesUserCount> getRolesList();

	Roles getRoleByName(String name);

	List<Roles> getRolesListForDropdown(String domainUrl) throws ResourceException;

	List<ModulesActionsDto> getModulesActionsDtoListByUser(UserProfile userProfile, String domainUrl);

	List<ModulesActionsDto> getUserBasedRoleActions(String userId, String domainUrl) throws ResourceException;
}
