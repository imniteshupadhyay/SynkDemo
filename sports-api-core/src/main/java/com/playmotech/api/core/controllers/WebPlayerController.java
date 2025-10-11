package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserAddEditDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.PlayerService;
import com.playmotech.api.core.services.UserService;
import com.playmotech.api.core.utils.AcademyDomainUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@CrossOrigin("*")
@RequestMapping("web/players")
@RequiredArgsConstructor
public class WebPlayerController {

	private final UserService userService;

	private final PlayerService playerService;
	private final UserProfileRepo userProfileRepository;

	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping
	public ResponseEntity<ServiceResponse> getPlayerById(@RequestParam String userId, HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		String academyDomain = request.getHeader("origin");

		// Check if user is super admin - if yes, don't apply domain filter
		String domainFilter;
		try {
			boolean isSuperAdmin = academyDomainUtil.isSuperAdmin(currentUserProfile.getId());
			domainFilter = isSuperAdmin ? null : academyDomain;
		} catch (ResourceException e) {
			log.error("Error checking super admin status for user: {}", currentUserProfile.getId(), e);
			// Default to applying domain filter if there's an error
			domainFilter = academyDomain;
		}

		ServiceResponse response = playerService.getPlayerById(userId, currentUserProfile.getId(), domainFilter);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("kpi")
	public ResponseEntity<ServiceResponse> getPlayerKpiById(@RequestParam String userId, HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		String academyDomain = request.getHeader("origin");

		// Check if user is super admin - if yes, don't apply domain filter
		String domainFilter;
		try {
			boolean isSuperAdmin = academyDomainUtil.isSuperAdmin(currentUserProfile.getId());
			domainFilter = isSuperAdmin ? null : academyDomain;
		} catch (ResourceException e) {
			log.error("Error checking super admin status for user: {}", currentUserProfile.getId(), e);
			// Default to applying domain filter if there's an error
			domainFilter = academyDomain;
		}

		ServiceResponse response = playerService.getPlayerKpiById(userId, currentUserProfile.getId(), domainFilter);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("coach-kpi")
	public ResponseEntity<ServiceResponse> getCoachKpiById(@RequestParam String userId, HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		String academyDomain = request.getHeader("origin");

		// Check if user is super admin - if yes, don't apply domain filter
		String domainFilter;
		try {
			boolean isSuperAdmin = academyDomainUtil.isSuperAdmin(currentUserProfile.getId());
			domainFilter = isSuperAdmin ? null : academyDomain;
		} catch (ResourceException e) {
			log.error("Error checking super admin status for user: {}", currentUserProfile.getId(), e);
			// Default to applying domain filter if there's an error
			domainFilter = academyDomain;
		}

		ServiceResponse response = playerService.getCoachKpiById(userId, currentUserProfile.getId(), domainFilter);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping
	public ResponseEntity<ServiceResponse> addPlayer(@Valid @RequestBody UserAddEditDto userProfileDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = playerService.addPlayer(userProfileDto, currentUserProfile.getId());

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping
	public ResponseEntity<ServiceResponse> editPlayer(@RequestBody UserAddEditDto userProfileDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = playerService.editPlayer(userProfileDto, currentUserProfile.getId());

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<ServiceResponse> deletePlayer(@PathVariable String id) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = userService.deleteUser(id, currentUserProfile.getId());

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

}
