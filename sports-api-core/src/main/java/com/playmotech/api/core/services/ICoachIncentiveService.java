package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.dto.CoachPointRulesDto;
import com.playmotech.api.core.dto.CoachPointsBalanceDto;
import com.playmotech.api.core.dto.CoachPointsLeaderboardEntryDto;
import com.playmotech.api.core.dto.CoachPointsTransactionDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;

/**
 * Interface for Coach Incentive Service Handles point transactions, balance
 * management, and leaderboards
 */
public interface ICoachIncentiveService {

	/**
	 * Award points to a coach for a specific action
	 *
	 * @param coachId    ID of coach
	 * @param sourceType Type of action (ATTENDANCE, PERFORMANCE_REPORT, etc.)
	 * @param sourceId   Reference ID to the source that generated points
	 * @param academyId  ID of academy (optional, can be null for org-level actions)
	 * @param metadata   Additional data about the transaction
	 * @return The transaction DTO with details of awarded points
	 */
	CoachPointsTransactionDto awardPoints(String coachId, IncentiveSourceType sourceType, String sourceId,
			String academyId, Map<String, Object> metadata) throws ResourceException;

	/**
	 * Check if points have already been awarded for a specific source
	 *
	 * @param sourceType Type of action
	 * @param sourceId   Reference ID
	 * @return True if points have already been awarded
	 */
	boolean isAlreadyAwarded(IncentiveSourceType sourceType, String sourceId);

	/**
	 * Get the current points balance for a coach (organization-wide)
	 *
	 * @param coachId ID of coach
	 * @return Coach points balance at organization level
	 */
	CoachPointsBalanceDto getCoachPointsBalance(String coachId, String academyId) throws ResourceException;

	/**
	 * Get the academy-specific points balance for a coach
	 *
	 * @param coachId   ID of coach
	 * @param academyId ID of academy
	 * @return Coach points balance at academy level
	 */
	CoachPointsBalanceDto getCoachAcademyPointsBalance(String coachId, String academyId) throws ResourceException;

	/**
	 * Get the coach's rank in their organization
	 *
	 * @param coachId ID of coach
	 * @return The coach's rank (1-based)
	 */
	int getCoachRank(String coachId, String academyId);

	/**
	 * Get the organization leaderboard with time period filtering and search
	 * capabilities
	 *
	 * @param organisationId ID of organization
	 * @param timePeriod     Time period filter (WEEKLY, MONTHLY, YEARLY, ALL_TIME)
	 * @param searchQuery    Optional search query for coach name
	 * @param pageable       Pagination and sorting information
	 * @return Page of leaderboard entries
	 */
	ServiceResponse getOrganisationLeaderboard(String organisationId, String timePeriod, String searchQuery,
			Pageable pageable) throws ResourceException;

	/**
	 * Get the academy leaderboard with time period filtering and search
	 * capabilities
	 *
	 * @param academyId   ID of academy
	 * @param timePeriod  Time period filter (WEEKLY, MONTHLY, YEARLY, ALL_TIME)
	 * @param searchQuery Optional search query for coach name
	 * @param pageable    Pagination and sorting information
	 * @return Page of leaderboard entries
	 */
	ServiceResponse getAcademyLeaderboard(String academyId, String timePeriod, String searchQuery, Pageable pageable,
			RankLevel rankLevel) throws ResourceException;

	ServiceResponse getAcademyLeaderboardV2(String academyId, String timePeriod, String searchQuery, Pageable pageable,
			RankLevel rankLevel) throws ResourceException;

	/**
	 * Get a single coach's leaderboard entry with rank and stats This is used to
	 * show the current user's position in the leaderboard
	 *
	 * @param coachId    ID of the coach
	 * @param academyId  Optional ID of academy
	 * @param timePeriod Time period filter (WEEKLY, MONTHLY, YEARLY, ALL_TIME)
	 * @return The coach's leaderboard entry with rank information
	 */
	CoachPointsLeaderboardEntryDto getCoachLeaderboardEntry(String coachId, String academyId, String timePeriod)
			throws ResourceException;

	/**
	 * Get transaction history for a coach
	 *
	 * @param coachId  ID of coach
	 * @param pageable Pagination information
	 * @return Page of transactions sorted by date (newest first)
	 */
	Page<CoachPointsTransactionDto> getCoachTransactions(String coachId, Pageable pageable);

	/**
	 * Request redemption of points for a voucher
	 *
	 * @param coachId       ID of coach requesting redemption
	 * @param voucherTypeId ID of voucher type to redeem
	 * @return True if redemption request was successful
	 */
	boolean requestVoucherRedemption(String coachId, String academyId, Long voucherTypeId) throws ResourceException;

	/**
	 * Get available voucher types for an organization Returns organization-specific
	 * and global vouchers
	 *
	 * @param organisationId ID of organization
	 * @return List of available voucher types
	 */
	List<Map<String, Object>> getAvailableVoucherTypes(String organisationId) throws ResourceException;

	/**
	 * Get available voucher types for an academy Returns academy-specific,
	 * organization-specific, and global vouchers
	 *
	 * @param academyId ID of academy
	 * @return List of available voucher types
	 */
	List<Map<String, Object>> getAvailableAcademyVoucherTypes(String academyId) throws ResourceException;

	/**
	 * Update coach points rules
	 *
	 * @param organisationId ID of organization
	 * @param rules          Map of action types to point values
	 */
	void updatePointRules(String organisationId, Map<String, Integer> rules);

	/**
	 * Get point rules for organization or academy
	 *
	 * @param academyId ID of academy
	 * @return Map of action types to point values
	 */
	Map<String, CoachPointRulesDto> getPointRules(String academyId) throws ResourceException;

}