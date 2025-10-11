package com.playmotech.api.core.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachActivityStreak;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CoachStreakDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachActivityStreakRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.ICoachStreakService;
import com.playmotech.api.core.validation.CoachRoleValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachStreakService implements ICoachStreakService {

    private final CoachActivityStreakRepository streakRepository;

    private final UserProfileRepo userProfileRepository;

    private final AcademyRepo academyRepository;

    private final CoachRoleValidator coachRoleValidator;

    @Override
    @Transactional
    public CoachStreakDto recordActivity(String coachId, String academyId) throws ResourceException {
        LocalDate today = LocalDate.now();

        coachRoleValidator.isUserAValidCoach(coachId, academyId);

        // If academy is provided, update both organization and academy-specific streaks
        if (academyId != null) {
            // Get academy
            Academy academy = academyRepository.findById(academyId).orElse(null);
            if (academy != null) {
                // Update academy-specific streak
                CoachActivityStreak academyStreak = updateStreak(coachId, academyId, today, true);

                // Also update organization streak if academy has organization
                // if (academy.getOrg() != null) {
                // updateStreak(coachId, null, today, false);
                // }

                return mapStreakToDto(academyStreak);
            }
        }

        // Otherwise, just update organization streak
        CoachActivityStreak streak = updateStreak(coachId, null, today, false);
        return mapStreakToDto(streak);
    }

    @Override
    public CoachStreakDto getCoachStreak(String coachId) throws ResourceException {
        CoachActivityStreak streak = streakRepository.findByCoachIdAndIsAcademySpecificFalse(coachId)
                .orElseGet(() -> getDefaultStreak(coachId, null, false));

        return mapStreakToDto(streak);
    }

    @Override
    public CoachStreakDto getCoachAcademyStreak(String coachId, String academyId) throws ResourceException {
        // Validate academy
        academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Academy not found"));

        coachRoleValidator.isUserAValidCoach(coachId, academyId);

        CoachActivityStreak streak = streakRepository.findByCoachIdAndAcademyId(coachId, academyId)
                .orElseGet(() -> getDefaultStreak(coachId, academyId, true));

        return mapStreakToDto(streak);
    }

    @Override
    public List<CoachStreakDto> getAllCoachStreaks(String coachId) throws ResourceException {
        List<CoachActivityStreak> streaks = streakRepository.findByCoachId(coachId);
        return streaks.stream()
                .map(this::mapStreakToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CoachStreakDto> getTopAcademyStreaks(String academyId, int limit) throws ResourceException {
        List<CoachActivityStreak> streaks = streakRepository.findByAcademyIdAndIsAcademySpecificTrue(academyId);

        return streaks.stream()
                .sorted((s1, s2) -> s2.getCurrentStreak().compareTo(s1.getCurrentStreak()))
                .limit(limit)
                .map(this::mapStreakToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CoachStreakDto> getTopOrganisationStreaks(String organisationId, int limit) throws ResourceException {
        List<CoachActivityStreak> streaks = streakRepository
                .findByOrganisationIdAndIsAcademySpecificFalse(organisationId);

        return streaks.stream()
                .sorted((s1, s2) -> s2.getCurrentStreak().compareTo(s1.getCurrentStreak()))
                .limit(limit)
                .map(this::mapStreakToDto)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to update a coach's streak
     */
    private CoachActivityStreak updateStreak(String coachId, String academyId, LocalDate today,
            boolean isAcademySpecific) throws ResourceException {
        CoachActivityStreak streak;

        coachRoleValidator.isUserAValidCoach(coachId, academyId);

        if (isAcademySpecific) {
            streak = streakRepository.findByCoachIdAndAcademyId(coachId, academyId)
                    .orElseGet(() -> getDefaultStreak(coachId, academyId, true));
        } else {
            streak = streakRepository.findByCoachIdAndIsAcademySpecificFalse(coachId)
                    .orElseGet(() -> getDefaultStreak(coachId, null, false));
        }

        if (streak.getLastActivityDate() == null) {
            // First activity
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setStreakStartDate(today);
        } else if (streak.getLastActivityDate().equals(today)) {
            // Already recorded activity today, streak count doesn't change
        } else if (streak.getLastActivityDate().equals(today.minusDays(1))) {
            // Consecutive day, increment streak
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);

            // Update the longest streak if needed
            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }
        } else {
            // Streak broken, reset
            streak.setCurrentStreak(1);
            streak.setStreakStartDate(today);
        }

        streak.setLastActivityDate(today);
        return streakRepository.save(streak);
    }

    /**
     * Helper method to create a new streak record
     */
    private CoachActivityStreak getDefaultStreak(String coachId, String academyId, boolean isAcademySpecific) {
        CoachActivityStreak streak = new CoachActivityStreak();
        streak.setId(UUID.randomUUID().toString());
        streak.setCoachId(coachId);
        streak.setCurrentStreak(0);
        streak.setLongestStreak(0);
        streak.setIsAcademySpecific(isAcademySpecific);

        // Set academy if provided
        if (academyId != null) {
            Academy academy = academyRepository.findById(academyId).orElse(null);
            streak.setAcademy(academy);

            // Set organization from academy
            if (academy != null && academy.getOrg() != null) {
                streak.setOrganisation(academy.getOrg());
            }
        }

        return streak;
    }

    /**
     * Helper method to map a streak entity to DTO
     */
    private CoachStreakDto mapStreakToDto(CoachActivityStreak streak) {
        UserProfile coach = userProfileRepository.findById(streak.getCoachId()).orElse(null);

        CoachStreakDto dto = new CoachStreakDto();
        dto.setCoachId(streak.getCoachId());
        dto.setCoachName(coach != null ? coach.getDisplayName() : null);
        dto.setCoachProfilePic(coach != null ? coach.getProfilePictureUrl() : null);
        dto.setCurrentStreak(streak.getCurrentStreak());
        dto.setLongestStreak(streak.getLongestStreak());
        dto.setLastActivityDate(streak.getLastActivityDate());
        dto.setStreakStartDate(streak.getStreakStartDate());
        dto.setIsAcademySpecific(streak.getIsAcademySpecific());

        if (streak.getAcademy() != null) {
            dto.setAcademyId(streak.getAcademy().getId());
            dto.setAcademyName(streak.getAcademy().getName());
        }

        if (streak.getOrganisation() != null) {
            dto.setOrganisationId(streak.getOrganisation().getId());
        }

        return dto;
    }
}
