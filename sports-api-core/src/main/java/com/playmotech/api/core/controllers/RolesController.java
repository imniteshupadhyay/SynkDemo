package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.Modules;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.RolesUserCount;
import com.playmotech.api.core.dto.ModulesActionsDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.RoleByIdResponseDto;
import com.playmotech.api.core.dto.RolesRequestDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IRolesService;
import com.playmotech.api.core.utils.AcademyDomainUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("roles")
@CrossOrigin("*")
public class RolesController {

	private final IRolesService rolesService;

	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping
	public ResponseEntity<Response<List<ModulesActionsDto>>> getUserBasedRole(HttpServletRequest request,
			@RequestParam(name = "userId", required = false) String userId) {
		try {
			String domainUrl = request.getHeader("origin");
			List<ModulesActionsDto> userBasedRoleActions = rolesService.getUserBasedRoleActions(userId, domainUrl);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ModulesActionsDto>>builder()
					.status(HttpStatus.OK.value()).message("ok").body(userBasedRoleActions).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ModulesActionsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT })
	public ResponseEntity<Response<String>> addEditRole(@RequestBody RolesRequestDto roleReq) {
		try {
			String res = rolesService.addEditRole(roleReq);

			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<String>builder().status(HttpStatus.OK.value()).message(res).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("dropdown")
	public ResponseEntity<Response<List<Roles>>> rolesListForDropdown(HttpServletRequest request,
			@RequestParam(name = "academyId", required = false) String academyId) {
		try {

//			String domainUrl = request.getHeader("origin");

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
				System.out.println("academyDomain: " + academyDomain);
//				if(!StringUtils.hasText(academyDomain)) {
//					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.<List<Roles>>builder()
//							.status(HttpStatus.BAD_REQUEST.value())
//							.message("Missing academyId or domain does not exist")
//							.build());
//				}
			}

			System.out.println("academyDomain: " + academyDomain);
			List<Roles> roles = rolesService.getRolesListForDropdown(academyDomain);

			return ResponseEntity.status(HttpStatus.OK).body(
					Response.<List<Roles>>builder().status(HttpStatus.OK.value()).message("ok").body(roles).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<Roles>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("list")
	public ResponseEntity<Response<List<RolesUserCount>>> getRolesList() {
		List<RolesUserCount> rolesList = rolesService.getRolesList();

		return ResponseEntity.status(HttpStatus.OK).body(Response.<List<RolesUserCount>>builder()
				.status(HttpStatus.OK.value()).body(rolesList).message("ok").build());
	}

	@GetMapping("{id}")
	public ResponseEntity<Response<RoleByIdResponseDto>> getRoleById(@PathVariable String id) {
		try {
			RoleByIdResponseDto res = rolesService.getRoleById(id);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<RoleByIdResponseDto>builder()
					.status(HttpStatus.OK.value()).body(res).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<RoleByIdResponseDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping("modules")
	public ResponseEntity<Response<List<Modules>>> getModulesList() {
		try {
			List<Modules> modulesMasterList = rolesService.getModulesMasterList();

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<Modules>>builder()
					.status(HttpStatus.OK.value()).body(modulesMasterList).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<Modules>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("modules/actions")
	public ResponseEntity<Response<List<ModulesActionsDto>>> getModulesActionsList() {
		List<ModulesActionsDto> modulesActionsList = rolesService.getModulesActionsList();

		return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ModulesActionsDto>>builder()
				.status(HttpStatus.OK.value()).body(modulesActionsList).message("ok").build());
	}
}
