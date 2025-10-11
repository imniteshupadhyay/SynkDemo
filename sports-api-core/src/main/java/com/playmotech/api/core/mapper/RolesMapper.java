package com.playmotech.api.core.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Modules;
import com.playmotech.api.core.dao_postgres.ModulesActions;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dto.ModulesActionsRequestDto;
import com.playmotech.api.core.dto.ModulesRequestDto;
import com.playmotech.api.core.dto.RolesRequestDto;
import com.playmotech.api.core.utils.EnumUtil.RoleType;

public class RolesMapper {

	public static Roles mapRolesReqDtoToRoles(RolesRequestDto roleReqDto) {
		List<ModulesActions> ma = convertToModulesActionsList(roleReqDto.getModulesActions());
		Roles role = new Roles();
		role.setId(roleReqDto.getId());
		role.setRoleName(roleReqDto.getRoleName());
		role.setDescription(roleReqDto.getDescription());
		role.setSequence(roleReqDto.getSequence());
		role.setModuleActions(ma);

		// Set isEditable based on role name
		if (roleReqDto.isEditable()) {
			role.setEditable(!RoleType.isNonEditable(roleReqDto.getRoleName()));
		} else {
			role.setEditable(roleReqDto.isEditable());
		}
		return role;
	}

	public static List<ModulesActions> convertToModulesActionsList(Collection<ModulesActionsRequestDto> mard) {
		if (mard.isEmpty()) {
			return Collections.emptyList();
		}

		List<ModulesActions> list = new ArrayList<>(mard.size());
		for (ModulesActionsRequestDto modulesActions : mard) {

			list.add(mapModulesActionsReqToModulesActions(modulesActions));
		}
		return list;
	}

	public static ModulesActions mapModulesActionsReqToModulesActions(ModulesActionsRequestDto mard) {
		ModulesActions ma = new ModulesActions();
		ma.setId(mard.getId());
		ma.setActionCode(mard.getActionCode());
		ma.setActionName(mard.getActionName());
		ma.setDisabled(mard.getDisabled());
		ma.setModules(mapModulesReqDtoToModule(mard.getModules()));
		return ma;
	}

	public static Modules mapModulesReqDtoToModule(ModulesRequestDto moduleReq) {
		Modules modules = new Modules();
		BeanUtils.copyProperties(moduleReq, modules);
		return modules;
	}
}