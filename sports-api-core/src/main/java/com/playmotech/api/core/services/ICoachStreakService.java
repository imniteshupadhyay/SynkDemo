package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.CoachStreakDto;
import com.playmotech.api.core.exceptions.ResourceException;

/**
 * Service interface for coach activity streak management
 */
public interface ICoachStreakService {

    /**
     * Record coach activity for streak tracking
     * This should be called whenever a coach performs an activity
     * 
     * @param coachId   ID of coach
     * @param academyId ID of academy (optional, can be null)
     * @return Updated streak information
     */
    CoachStreakDto recordActivity(String coachId, String academyId) throws ResourceException;

    /**
     * Get a coach's current streak information (organization-wide)
     * 
     * @param coachId ID of coach
     * @return Current streak information
     */
    CoachStreakDto getCoachStreak(String coachId) throws ResourceException;

    /**
     * Get a coach's academy-specific streak information
     * 
     * @param coachId   ID of coach
     * @param academyId ID of academy
     * @return Current streak information for the academy
     */
    CoachStreakDto getCoachAcademyStreak(String coachId, String academyId) throws ResourceException;

    /**
     * Get all streaks for a coach (organization and academy-specific)
     * 
     * @param coachId ID of coach
     * @return List of streak information
     */
    List<CoachStreakDto> getAllCoachStreaks(String coachId) throws ResourceException;

    /**
     * Get top streaks in an academy
     * 
     * @param academyId ID of academy
     * @param limit     Maximum number of results
     * @return List of top coach streaks
     */
    List<CoachStreakDto> getTopAcademyStreaks(String academyId, int limit) throws ResourceException;

    /**
     * Get top streaks in an organization
     * 
     * @param organisationId ID of organization
     * @param limit          Maximum number of results
     * @return List of top coach streaks
     */
    List<CoachStreakDto> getTopOrganisationStreaks(String organisationId, int limit) throws ResourceException;
}
