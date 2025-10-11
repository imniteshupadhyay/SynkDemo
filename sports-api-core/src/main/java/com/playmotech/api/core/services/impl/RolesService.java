package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.Modules;
import com.playmotech.api.core.dao_postgres.ModulesActions;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.RolesUserCount;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UsersActionsMapping;
import com.playmotech.api.core.dto.ActionsDto;
import com.playmotech.api.core.dto.ModulesActionsDto;
import com.playmotech.api.core.dto.ModulesActionsRequestDto;
import com.playmotech.api.core.dto.RoleByIdResponseDto;
import com.playmotech.api.core.dto.RolesRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.mapper.RolesMapper;
import com.playmotech.api.core.repo.ModulesActionsRepo;
import com.playmotech.api.core.repo.ModulesRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.RolesUserCountRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.repo.UsersActionsMappingRepo;
import com.playmotech.api.core.services.IRolesService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.EnumUtil.RoleType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolesService implements IRolesService {

	private final AcademyDomainUtil academyDomainUtil;

	private final UsersActionsMappingRepo usersActionsMappingRepo;
	private final IUserProfileService iUserProfileService;
	private final RolesUserCountRepo rolesUserCountRepo;
	private final ModulesActionsRepo modulesActionsRepo;
	private final UserProfileRepo userProfileRepo;
	private final ModulesRepo modulesRepo;
	private final RolesRepo rolesRepo;

	@Override
	public List<Roles> getRolesListForDropdown(String domainUrl) throws ResourceException {
		try {
			if (StringUtils.hasText(domainUrl)) {
				Roles role = academyDomainUtil.getCurrentUserRole(domainUrl);
				return rolesRepo.findBySequenceGreaterThan(role.getSequence());
			} else {
				return rolesRepo.findBySequenceGreaterThan(2L);
			}

		} catch (ResourceException e) {
			log.error("Exception in RolesServiceImpl.getRolesListForDropDown {}", e);
			throw new ResourceException(e.getErrorCodes(), e.getMessage());
		}
	}

	@Override
	public RoleByIdResponseDto getRoleById(String id) throws ResourceException {
		List<ModulesActionsDto> modulesActionsDto = this.getModulesActionsDtoList();

		try {
			Roles roles = rolesRepo.findById(Long.parseLong(id))
					.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "role does not exist"));

			Map<Modules, List<ModulesActions>> groupedActions = roles.getModuleActions().stream()
					.collect(Collectors.groupingBy(ModulesActions::getModules));

			Set<ModulesActions> allActions = groupedActions.values().stream().flatMap(List::stream)
					.collect(Collectors.toSet());

			modulesActionsDto.forEach(modulesActionsDtoObj -> modulesActionsDtoObj.getActions().forEach(actionDto -> {
				if (allActions.stream()
						.anyMatch(modulesAction -> actionDto.getActionCode().equals(modulesAction.getActionCode())
								&& actionDto.getActionName().equals(modulesAction.getActionName()))) {
					actionDto.setAllowed(true);
				}
			}));

			RoleByIdResponseDto roleByIdDto = new RoleByIdResponseDto();
			roleByIdDto.setRoleName(roles.getRoleName());
			roleByIdDto.setDescription(roles.getDescription());
			roleByIdDto.setEditable(roles.isEditable());
			roleByIdDto.setSequence(roles.getSequence());
			roleByIdDto.setModulesActions(modulesActionsDto);

			return roleByIdDto;
		} catch (ResourceException e) {
			log.warn("Exception when finding role by id: {}", e);
			throw new ResourceException(e.getErrorCodes(), e.getMessage());
		}
	}

	@Override
	public List<ModulesActionsDto> getUserBasedRoleActions(String userId, String domainUrl) throws ResourceException {
		String resolvedUserId;

		// Resolve userId: if provided use it, otherwise get from authenticated user
		if (userId != null && !userId.trim().isEmpty()) {
			resolvedUserId = userId;
		} else {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			resolvedUserId = currentUser.getUserId();
		}

		// Fetch user profile
		UserProfileDto userProfileDto = iUserProfileService.getUserProfileById(resolvedUserId);
		UserProfile userProfile = userProfileRepo.findById(resolvedUserId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST, "User does not exist"));

		try {
			return this.getModulesActionsDtoListByUser(userProfile, domainUrl);
		} catch (Exception e) {
			log.error("Exception in UserServiceImpl.getUserBasedRoleActions ", e);
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
					"Exception when finding user based role actions");
		}
	}

	@Override
	public String addEditRole(RolesRequestDto roleReq) throws ResourceException {
		boolean roleNameDuplicate;
		try {
			if (roleReq.getId() != null) {
				log.info("Edit role since id is present");
				roleNameDuplicate = rolesRepo.existsByRoleNameAndIdIsNot(roleReq.getRoleName(), roleReq.getId());

				Optional<Roles> orgRole = rolesRepo.findById(roleReq.getId());

				if (orgRole.isPresent()) {
					roleReq.setEditable(!RoleType.isNonEditable(orgRole.get().getRoleName()));
				}
			} else {
				log.info("New role detected");
				roleNameDuplicate = rolesRepo.existsByRoleName(roleReq.getRoleName());
			}

			if (roleNameDuplicate) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Same role already exists");
			}

			// Modifying Module Actions
			List<ModulesActionsRequestDto> allowedActions = roleReq.getModulesActions().stream()
					.filter(ModulesActionsRequestDto::getAllowed).toList();

			roleReq.setModulesActions(allowedActions);

			if (!areModuleActionsEqual(roleReq)) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST,
						"Modules or Modules Actions has been edited. Please check and try again");
			}

			Roles role = RolesMapper.mapRolesReqDtoToRoles(roleReq);

			rolesRepo.save(role);

			return roleReq.getId() == null ? "Role added successfully" : "Role updated successfully";
		} catch (Exception e) {
			log.error("Exception in RolesServiceImpl.addEditRole " + e);
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
					roleReq.getId() != null ? "Exception when updating role" : "Exception when saving role");
		}

	}

	@Override
	public List<Modules> getModulesMasterList() throws ResourceException {
		List<Modules> modulesMaster;
		try {
			modulesMaster = modulesRepo.findAll();

			return modulesMaster;
		} catch (Exception e) {
			log.error("Exception occured in UserServiceImpl.getModulesMasterList " + e.getMessage());
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Something unexpected error occured");
		}

	}

	@Override
	public List<ModulesActionsDto> getModulesActionsList() {
		return this.getModulesActionsDtoList();
	}

	@Override
	public List<RolesUserCount> getRolesList() {
		return rolesUserCountRepo.findAll();
	}

	@Override
	public List<ModulesActionsDto> getModulesActionsDtoListByUser(UserProfile userProfile, String domainUrl) {
		try {
			Optional<UsersActionsMapping> userActionsMapping = usersActionsMappingRepo.findByUser(userProfile);

			if (!userActionsMapping.isPresent()) {
				return modulesActionsMaster(userProfile, domainUrl);
			}

			List<ModulesActionsDto> modulesActionsDtoList;
			ObjectMapper mapper = new ObjectMapper();

			modulesActionsDtoList = mapper.readValue(userActionsMapping.get().getActions(),
					new TypeReference<List<ModulesActionsDto>>() {
					});

			if (modulesActionsDtoList.isEmpty()) {
				return modulesActionsMaster(userProfile, domainUrl);
			}
			return modulesActionsDtoList;
		} catch (Exception e) {
			log.error("Exception occured in UserServiceImpl.getModulesActionsDtoListByUser " + e.getMessage());
			return Collections.emptyList();
		}
	}

	@Override
	public Roles getRoleByName(String name) {
//		return rolesRepo.findByRoleNameLike(name).get(0);
		List<Roles> roles = rolesRepo.findByRoleNameContainingIgnoreCase(name);
		return roles.get(0);
	}

	private boolean areModuleActionsEqual(RolesRequestDto roleReq) {
		try {
			List<ModulesActionsRequestDto> receivedModulesActions = roleReq.getModulesActions();
			List<ModulesActions> modulesActions = modulesActionsRepo.findAll();

			Map<String, ModulesActions> modulesActionsMap = modulesActions.stream()
					.collect(Collectors.toMap(ModulesActions::getActionCode, Function.identity()));
			Map<String, ModulesActionsRequestDto> receivedModulesActionsMap = receivedModulesActions.stream()
					.collect(Collectors.toMap(ModulesActionsRequestDto::getActionCode, Function.identity()));

			return receivedModulesActionsMap.keySet().stream().allMatch(modulesActionsMap::containsKey)
					&& areModulesActionsNameEqual(receivedModulesActionsMap.values(), modulesActionsMap.values());
		} catch (Exception e) {
			log.error("Exception in UserServiceImpl.areModuleActionsEqual: {}", e);
			return false;
		}
	}

	public boolean areModulesActionsNameEqual(Collection<ModulesActionsRequestDto> received,
			Collection<ModulesActions> masterModulesActions) {
		try {

			return received.stream().allMatch(each -> masterModulesActions.stream()
					.anyMatch(each2 -> each2.getActionName().equals(each.getActionName())));
		} catch (Exception e) {
			log.error("Exception in areModulesActionsNameEqual: " + e.getMessage());
			return false;
		}
	}

	private List<ModulesActionsDto> modulesActionsMaster(UserProfile userProfile, String domainUrl)
			throws ResourceException {
		List<ModulesActionsDto> modulesActionsDtoList;

		Roles roles = academyDomainUtil.getCurrentUserRole(domainUrl);

		modulesActionsDtoList = this.getModulesActionsDtoList();

		// list of all modules actions
		Map<Long, ModulesActionsDto> modulesMap = modulesActionsDtoList.stream().collect(Collectors
				.toMap(ModulesActionsDto::getId, Function.identity(), this::mergeModulesActions, LinkedHashMap::new));

		// list of modules actions based on user's role
		Map<Long, ModulesActions> rolesMap = roles.getModuleActions().stream()
				.collect(Collectors.toMap(ModulesActions::getId, Function.identity()));

		// each entry from modulesMap
		for (Entry<Long, ModulesActionsDto> entry : modulesMap.entrySet()) {
			// each entries' actions
			for (ActionsDto modulesActionsDto : entry.getValue().getActions()) {
				// checking modulesActionsDto with rolesMap
				if (rolesMap.containsKey(modulesActionsDto.getId())) {
					modulesActionsDto.setAllowed(Boolean.TRUE);
				}
			}
		}
		return new ArrayList<>(modulesMap.values());
	}

	private List<ModulesActionsDto> getModulesActionsDtoList() {
		try {
			log.info("Fetching modules actions dto list");
			List<ModulesActionsDto> modulesActionsDtoList = modulesActionsRepo.getModuleActions();

			// return empty if no data found
			if (modulesActionsDtoList.isEmpty()) {
				log.warn("No modules found, returning empty list");
				return Collections.emptyList();
			}

			// Post-process to eliminate duplicate modules and preserve the order
			Map<Long, ModulesActionsDto> moduleMap = modulesActionsDtoList.stream().collect(Collectors.toMap(
					ModulesActionsDto::getId, Function.identity(), this::mergeModulesActions, LinkedHashMap::new));

			// Sorting actions for each module
			moduleMap.values().forEach(modulesActionsDto -> modulesActionsDto.getActions()
					.sort(Comparator.comparing(ActionsDto::getActionCode)));

			log.info("Done fetching modules actions list");
			return new ArrayList<>(moduleMap.values());
		} catch (Exception e) {
			log.error("Exception occurred in UserServiceImpl.getModulesActionsDtoList: ", e);
			return Collections.emptyList();
		}
	}

	// Helper method for merging actions from duplicate modules
	private ModulesActionsDto mergeModulesActions(ModulesActionsDto existing, ModulesActionsDto replacement) {
		existing.getActions().addAll(replacement.getActions());
		return existing;
	}

}
