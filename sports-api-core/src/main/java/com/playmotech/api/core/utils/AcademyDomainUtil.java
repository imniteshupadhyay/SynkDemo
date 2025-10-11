package com.playmotech.api.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.RolesRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class AcademyDomainUtil {
	private final AcademyRepo academyRepo;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
	private final RolesRepo rolesRepo;
	private final UserProfileRepo userProfileRepo;

	/**
	 * Method to get {@link Academy} on the basis of domainUrl
	 *
	 * @param domainUrl
	 * @return {@link Academy}
	 */
	public List<Academy> getAcademyByUrl(String domainUrl) {
		return academyRepo.findByDomainUrl(domainUrl);
	}

	/**
	 * Method to get the session's user role on the basis of coachAcademyMapping
	 *
	 * @param domainUrl
	 * @return {@link Roles Name}
	 */
	public String getCurrentUserRoleName(String domainUrl) throws ResourceException {
		String userProfile = "";
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();
		if (currentUser.getRole() != null) {
			if (currentUser.getRole().equals(Role.SUPER_ADMIN)) {
				userProfile = Role.SUPER_ADMIN.name();
			} else if (currentUser.getRole().equals(Role.USER)) {
				userProfile = currentUser.getUserType().name();
			} else {
				List<Academy> academy = this.getAcademyByUrl(domainUrl);
				List<String> academyIds = academy.stream().map(each -> each.getId()).toList();
				List<CoachAcademyMapping> currentCoachAcademyMapping = coachAcademyMappingRepo
						.findByAcademy_IdInAndCoachUserProfile_Id(academyIds, currentUser.getId());
				if (currentCoachAcademyMapping.isEmpty()) {
					UserProfile superAdminUser = userProfileRepo.findById(currentUser.getId()).get();
					if (StringUtils.hasText(superAdminUser.getRole().name())
							&& superAdminUser.getRole().equals(Role.SUPER_ADMIN)) {
						userProfile = superAdminUser.getRole().name();
					} else {
						log.error("no role found in user_profile & coach_academy_mapping. hmmmmmmmmmmmmmmmmmmmmmm");
						throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user");
					}
				}
				Roles role = rolesRepo.findById(currentCoachAcademyMapping.get(0).getRoleId()).orElseThrow(
						() -> new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user"));
				userProfile = role.getRoleName();
				log.debug("AcademyDomainUtil - user's role name: {}", userProfile);
			}
		}
		return userProfile;
	}

	public UserProfile getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();
		return currentUser;
	}

	/**
	 * Method to get the session's user role on the basis of coachAcademyMapping
	 *
	 * @param domainUrl
	 * @return {@link Roles}
	 */
	public Roles getCurrentUserRole(String domainUrl) throws ResourceException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();
		if (currentUser.getRole() != null && currentUser.getRole().equals(Role.SUPER_ADMIN)) {
			List<Roles> role = rolesRepo.findByRoleNameContainingIgnoreCase(currentUser.getRole().name());
			return role.get(0);
		} else {
			List<Academy> academy = this.getAcademyByUrl(domainUrl);
			List<String> academyIds = academy.stream().map(each -> each.getId()).toList();
			List<CoachAcademyMapping> currentCoachAcademyMapping = coachAcademyMappingRepo
					.findByAcademy_IdInAndCoachUserProfile_Id(academyIds, currentUser.getId());

			UserProfile superAdminUser = userProfileRepo.findById(currentUser.getId()).get();
			if (!StringUtils.hasText(superAdminUser.getRole().name())
					&& superAdminUser.getRole().equals(Role.SUPER_ADMIN)) {
				log.error("no role found in user_profile & coach_academy_mapping from getCurrentUserRole.");
				throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user");
			}

			return rolesRepo.findById(currentCoachAcademyMapping.get(0).getRoleId()).orElseThrow(
					() -> new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user"));

		}
	}

	public Roles getUserRole(UserProfile userProfile, String domainUrl) throws ResourceException {
		List<Academy> academy = this.getAcademyByUrl(domainUrl);
		List<String> academyIds = academy.stream().map(Academy::getId).toList();
		List<CoachAcademyMapping> currentUserAcademyMapping = coachAcademyMappingRepo
				.findByAcademy_IdInAndCoachUserProfile_Id(academyIds, userProfile.getId());

		if (currentUserAcademyMapping.isEmpty()) {
			if (StringUtils.hasText(userProfile.getRole().name()) && userProfile.getRole().equals(Role.SUPER_ADMIN)) {
				List<Roles> roles = rolesRepo.findByRoleNameContainingIgnoreCase(userProfile.getRole().name());
				return roles.get(0);
			} else {
				log.error("no role found in user_profile & coach_academy_mapping in getUserRole");
				// throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for
				// the user");
				return null;
			}
		}
		Roles role = rolesRepo.findById(currentUserAcademyMapping.get(0).getRoleId())
				.orElseThrow(() -> new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user"));
		return role;
	}

	public String getAcademyDomain(String academyId) throws ResourceException {
		Academy academy = academyRepo.findById(academyId).orElseThrow(
				() -> new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No academy found for the given ID"));

		if (academy.getOrg() != null && StringUtils.hasText(academy.getOrg().getDomainUrl())) {
			return academy.getOrg().getDomainUrl();
		}

		return null;
	}

	public String getUserRole(String userId, String academyId, String domainUrl) throws ResourceException {
		UserProfile currentUser = userProfileRepo.findById(userId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "User not found"));

		Role userRole = currentUser.getRole();
		if (userRole == null) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "User role is not defined");
		}

		if (userRole.equals(Role.SUPER_ADMIN)) {
			return Role.SUPER_ADMIN.name();
		}

		if (userRole.equals(Role.USER)) {
			UserType userType = currentUser.getUserType();
			if (userType == null) {
				throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "User type is not defined");
			}

			if (userType.equals(UserType.PLAYER)) {
				List<TraineeAcademyMapping> mappings = traineeAcademyMappingRepo
						.findByTraineeUserProfileId(currentUser.getId());
				// Can be enhanced if you need to check specific academy IDs
				return mappings.isEmpty() ? "FREE_PLAYER" : "ACADEMY_PLAYER";
			}
		} else {
			List<String> academyIds = new ArrayList<>();

			if (StringUtils.hasText(academyId)) {
				academyIds.add(academyId);
			} else if (StringUtils.hasText(domainUrl)) {
				List<Academy> academies = getAcademyByUrl(domainUrl);
				academyIds = academies.stream().map(Academy::getId).toList();
			} else {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Academy ID or Domain URL must be provided");
			}

			List<CoachAcademyMapping> coachMappings = coachAcademyMappingRepo
					.findByAcademy_IdInAndCoachUserProfile_Id(academyIds, currentUser.getId());

			if (!coachMappings.isEmpty()) {
				Roles role = rolesRepo.findById(coachMappings.get(0).getRoleId()).orElseThrow(
						() -> new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "No role found for the user"));
				return role.getRoleName();
			}
		}

		throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Unable to determine user role");
	}

	public boolean isSuperAdmin(String userId) throws ResourceException {
		UserProfile currentUser = userProfileRepo.findById(userId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "User not found"));

		Role userRole = currentUser.getRole();
		if (userRole == null) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "User role is not defined");
		}

		return userRole.equals(Role.SUPER_ADMIN);
	}

}
