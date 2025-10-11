package com.playmotech.api.core.validation;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class CoachRoleValidator {
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;
    public static final Set<Long> ACCEPTABLE_ROLE_IDS = Set.of(
            2L, // 2 - ACADEMY_OWNER
            3L, // 3- ADMIN
            4L, // 4- CLUSTER_HEAD
            5L, // 5 - COACH
            6L // 6 - PROGRAM_MANAGER
    );

    /**
     * Method which checks if the requested user is a valid coach (roleId - 5L)
     * in the provided academy
     * 
     * @param coachId   User ID of the coach
     * @param academyId Academy ID
     * @throws ResourceException - If role is null and is not in the
     *                           ACCEPTABLE_ROLE_IDS list
     */
    public void isUserAValidCoach(String coachId, String academyId) throws ResourceException {
        CoachAcademyMapping coachAcademyMapping = coachAcademyMappingRepo
                .findByAcademy_IdAndCoachUserProfile_Id(academyId, coachId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                        "Coach not found in the provided academy"));
        Long roleId = coachAcademyMapping.getRoleId();

        if (roleId == null) {
            throw new ResourceException(ErrorCodes.NOT_FOUND, "Role not present");
        }

        if (!ACCEPTABLE_ROLE_IDS.contains(roleId)) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "The user is not coach");
        }
    }
}
