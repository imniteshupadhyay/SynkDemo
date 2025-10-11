package com.playmotech.api.core.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.IncentiveActionType;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.TimePeriod;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.CoachActivityStreak;
import com.playmotech.api.core.dao_postgres.CoachPointRule;
import com.playmotech.api.core.dao_postgres.CoachPointTransaction;
import com.playmotech.api.core.dao_postgres.CoachPointsBalance;
import com.playmotech.api.core.dao_postgres.CoachVoucherRedemption;
import com.playmotech.api.core.dao_postgres.CoachVoucherType;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.CoachLeaderboardProjection;
import com.playmotech.api.core.dto.CoachPointRulesDto;
import com.playmotech.api.core.dto.CoachPointsBalanceDto;
import com.playmotech.api.core.dto.CoachPointsLeaderboardEntryDto;
import com.playmotech.api.core.dto.CoachPointsTransactionDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.events.CoachActivityEvent;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.CoachActivityStreakRepository;
import com.playmotech.api.core.repo.CoachPointRuleRepository;
import com.playmotech.api.core.repo.CoachPointTransactionRepository;
import com.playmotech.api.core.repo.CoachPointsBalanceRepository;
import com.playmotech.api.core.repo.CoachVoucherRedemptionRepository;
import com.playmotech.api.core.repo.CoachVoucherTypeRepository;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.CoachPointTransactionErrorService;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICoachIncentiveService;
import com.playmotech.api.core.specification.CoachPointBalanceSpecification;
import com.playmotech.api.core.validation.CoachRoleValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachIncentiveService implements ICoachIncentiveService {

    private final CoachPointRuleRepository pointRuleRepository;

    private final CoachPointTransactionRepository transactionRepository;

    private final CoachPointsBalanceRepository balanceRepository;

    private final CoachAcademyMappingRepo coachAcademyMappingRepo;

    private final CoachVoucherTypeRepository voucherTypeRepository;

    private final CoachVoucherRedemptionRepository voucherRedemptionRepository;

    private final UserProfileRepo userProfileRepository;

    private final AcademyRepo academyRepository;

    private final IAcademyService academyService;

    private final ApplicationEventPublisher eventPublisher;

    private final CoachPointTransactionErrorService coachPointTransactionErrorService;

    private final CoachActivityStreakRepository coachActivityStreakRepository;

    private final CoachRoleValidator coachRoleValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModelMapper modelMapper = new ModelMapper();

    /**
     * Award points to a coach for a specific action
     * Points are awarded at both organization and academy levels when applicable
     * Clear Redis cache for leaderboard queries when points are awarded
     */
    @Override
    @Transactional
    public CoachPointsTransactionDto awardPoints(String coachId, IncentiveSourceType sourceType,
            String sourceId, String academyId, Map<String, Object> metadata) throws ResourceException {
        // Check if points already awarded for this source
        if (isAlreadyAwarded(sourceType, sourceId)) {
            log.error("Points already awarded for source type {} with id {}", sourceType, sourceId);
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "Points already awarded for the selected source type");
        }

        // Get coach user profile
        UserProfile coach = userProfileRepository.findById(coachId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Coach Not Found"));

        // Get academy if provided
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Academy Not Found"));

        // Get organization ID from academy
        String organisationId = academy.getOrg() != null ? academy.getOrg().getId() : null;

        // Get point rule for this source type
        CoachPointRule pointRule = null;

        // First check for academy-specific rule if academy is provided
        if (StringUtils.hasText(academyId)) {
            pointRule = pointRuleRepository.findByActionTypeAndAcademyIdAndIsAcademySpecificTrueAndActiveTrue(
                    sourceType.name(), academyId);
        }

        // If no academy-specific rule found, check for organization rule
        if (pointRule == null && StringUtils.hasText(organisationId)) {
            pointRule = pointRuleRepository.findByActionTypeAndOrganisationIdAndIsAcademySpecificFalseAndActiveTrue(
                    sourceType.name(), organisationId);

            // Fallback to legacy rule (backward compatibility)
            if (pointRule == null) {
                pointRule = pointRuleRepository.findByActionTypeAndOrganisationIdAndActiveTrue(
                        sourceType.name(), organisationId);
            }
        }

        // If still no rule found, check for global rule (no organization or academy)
        if (pointRule == null) {
            pointRule = pointRuleRepository.findByActionTypeAndOrganisationIdAndActiveTrue(sourceType.name(), null);
        }

        // Create point transaction - organization level
        CoachPointTransaction transaction = CoachPointTransaction.builder()
                .id(UUID.randomUUID().toString())
                .coach(coach)
                .academy(academy != null ? academy : null)
                .organisation(academy.getOrg() != null ? academy.getOrg() : null)
                .isAcademySpecific(academy != null ? true : false)
                .points(pointRule.getPointsAwarded())
                .actionType(IncentiveActionType.EARNED)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .metadata(metadataToJson(metadata))
                .createdAt(LocalDateTime.now())
                .createdBy(coach) // Self-awarded by system
                .build();

        try {
            transaction = transactionRepository.save(transaction);
            log.info("Points {} awarded for the {} source type", pointRule.getPointsAwarded(), sourceType);
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            log.error("Something unexpected happened while saving data: {}", e.getMessage());
            log.error("Persisting the details in the [coach_point_transaction_error] table");

            coachPointTransactionErrorService.saveTransactionError(transaction, e);

            throw new RuntimeException("Failed to save CoachPointTransaction", e);
        }

        updateCoachAcademyBalance(coachId, academy.getId(), organisationId, pointRule.getPointsAwarded(), 0, 0);

        // Publish event for coach activity tracking
        eventPublisher.publishEvent(new CoachActivityEvent(this, coachId, academyId));

        return mapTransactionToDto(transaction, coach, academy);
    }

    /**
     * Check if points have already been awarded for a specific source
     */
    @Override
    public boolean isAlreadyAwarded(IncentiveSourceType sourceType, String sourceId) {
        return transactionRepository.findBySourceTypeAndSourceIdAndActionType(
                sourceType, sourceId, IncentiveActionType.EARNED).isPresent();
    }

    /**
     * Get the current points balance for a coach (organization-wide)
     */
    @Override
    public CoachPointsBalanceDto getCoachPointsBalance(String coachId, String academyId) throws ResourceException {
        // Get organization ID from coach's academy mapping
        String organisationId = getCoachOrganisationId(coachId, academyId);
        return getCoachPointsBalance(coachId, organisationId, false, null);
    }

    /**
     * Get the academy-specific points balance for a coach
     */
    @Override
    public CoachPointsBalanceDto getCoachAcademyPointsBalance(String coachId, String academyId)
            throws ResourceException {
        // Get academy
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Academy not found"));

        coachRoleValidator.isUserAValidCoach(coachId, academyId);

        // Get organization ID from academy
        String organisationId = academy.getOrg() != null ? academy.getOrg().getId() : null;

        // Return academy-specific balance
        return getCoachPointsBalance(coachId, organisationId, true, academyId);
    }

    @Override
    public Map<String, CoachPointRulesDto> getPointRules(String academyId) throws ResourceException {
        List<CoachPointRule> rules;

        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));

        if (academy.getOrg() != null) {
            log.info("org is present, finding point rules on the basis of orgId");
            rules = pointRuleRepository.findByOrganisationIdAndActiveTrue(academy.getOrg().getId());
        } else {
            log.info("org is not present, finding point rules on the basis of academyId");
            rules = pointRuleRepository.findByAcademyIdAndIsAcademySpecificTrueAndActiveTrue(academy.getId());
        }

        if (rules.isEmpty()) {
            log.warn("No point rules found on the basis of orgId & academyId. Finding default point rules");
            rules = pointRuleRepository.findByAcademyIdAndOrganisationIdAndActiveTrue(null, null);
        }

        // Convert to map
        Map<String, CoachPointRulesDto> pointRules = rules.stream()
                .map(rule -> modelMapper.map(rule, CoachPointRulesDto.class))
                .sorted(Comparator.comparing(CoachPointRulesDto::getId)) // sort DTOs by id
                .collect(Collectors.toMap(
                        CoachPointRulesDto::getActionType, // key
                        dto -> dto, // value
                        (oldValue, newValue) -> oldValue, // merge function
                        LinkedHashMap::new // preserve sorted order
                ));

        return pointRules;
    }

    /**
     * Get the current points balance for a coach with academy context if requested
     *
     * @param coachId           Coach ID
     * @param organisationId    Organization ID
     * @param isAcademySpecific Whether to get academy-specific balance
     * @param academyId         Academy ID (only used when isAcademySpecific is
     *                          true)
     * @return Coach balance DTO
     * @throws ResourceException
     */
    private CoachPointsBalanceDto getCoachPointsBalance(String coachId, String organisationId,
            boolean isAcademySpecific,
            String academyId) throws ResourceException {
        UserProfile coach = userProfileRepository.findById(coachId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Coach Not Found"));

        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy Not Found"));

        coachRoleValidator.isUserAValidCoach(coachId, academyId);

        // Get the appropriate balance based on scope
        CoachPointsBalance balance = getOrCreateAcademyBalance(coachId,
                academy.getId(),
                academy.getOrg() != null ? academy.getOrg().getId() : null);

        // Get coach rank based on scope
        Integer rank = null;
        // if (academy.getOrg() != null) {
        // rank = balanceRepository.findCoachRankInOrganisation(coachId,
        // academy.getOrg().getId());
        // } else {
        // rank = balanceRepository.findCoachRankInAcademy(coachId, academy.getId());
        // }
        if (academy != null) {
            rank = balanceRepository.findCoachRankInAcademy(coachId, academy.getId());
        }

        return CoachPointsBalanceDto.builder()
                .coachId(coachId)
                .coachName(coach.getDisplayName())
                .coachProfilePic(coach.getProfilePictureUrl())
                .totalEarned(balance.getTotalEarned())
                .totalRedeemed(balance.getTotalRedeemed())
                .currentBalance(balance.getCurrentBalance())
                .vouchersRedeemed(balance.getVouchersRedeemed())
                .displayFormat(formatPointsDisplay(balance))
                .rank(rank != null ? rank : 0)
                .academyId(isAcademySpecific ? academyId : null)
                .organisationId(organisationId)
                .isAcademySpecific(isAcademySpecific)
                .build();
    }

    /**
     * Get the coach's rank in their organization
     */
    @Override
    public int getCoachRank(String coachId, String academyId) {
        // Get organization ID from coach
        String organisationId = getCoachOrganisationId(coachId, academyId);

        Integer rank;
        if (organisationId != null) {
            rank = balanceRepository.findCoachRankInOrganisation(coachId, organisationId);
        } else {
            rank = balanceRepository.findCoachRankInAcademy(coachId, academyId);
        }
        return rank != null ? rank : 0; // Return 0 if no rank found
    }

    /**
     * Get the organization leaderboard with time period filtering and search
     * capabilities
     */
    @Override
    public ServiceResponse getOrganisationLeaderboard(
            String organisationId, String timePeriodStr, String searchQuery, Pageable pageable)
            throws ResourceException {

        log.info("Fetching organization leaderboard for org: {}, period: {}, search: {}, page: {}",
                organisationId, timePeriodStr, searchQuery, pageable.getPageNumber());

        // Convert time period string to enum
        TimePeriod timePeriod = TimePeriod.fromString(timePeriodStr);

        // Get start date for the time period
        LocalDateTime startDate = timePeriod.getStartDate();

        // // Get filtered leaderboard data
        // Page<CoachPointsBalance> leaderboard;

        // if (searchQuery != null && !searchQuery.trim().isEmpty()) {
        // // Search by coach name and filter by time period
        // leaderboard =
        // balanceRepository.findLeaderboardByOrganisationIdWithSearchAndTimeFilter(
        // organisationId, searchQuery, startDate, pageable);
        // } else {
        // // Filter by time period only
        // leaderboard =
        // balanceRepository.findLeaderboardByOrganisationIdWithTimeFilter(
        // organisationId, startDate, pageable);
        // }

        // Create specification for filtering
        Specification<CoachPointsBalance> spec = CoachPointBalanceSpecification
                .organisationLeaderboardSpec(organisationId, startDate, searchQuery);

        // Query using specification
        Page<CoachPointsBalance> leaderboard = balanceRepository.findAll(spec, pageable);
        List<CoachPointsLeaderboardEntryDto> leaderboards;
        // Performance optimization: batch fetch all coach ranks in a single query
        List<String> coachIds = leaderboard.getContent().stream()
                .map(balance -> balance.getCoach().getId())
                .collect(Collectors.toList());

        // Skip batch processing for empty result
        if (coachIds.isEmpty()) {
            return ResponseBuilder.success(Collections.emptyList(), ApiResponse.LIST_FETCHED_SUCCESSFULLY,
                    HttpStatus.OK);
            // return Page.empty(pageable);
        }

        Map<String, CoachActivityStreak> coachStreak = coachActivityStreakRepository.findByCoachIdIn(coachIds)
                .stream()
                .collect(Collectors.toMap(
                        CoachActivityStreak::getCoachId,
                        Function.identity(),
                        (existing, replacement) -> replacement));

        // Create a map of coach IDs to ranks for fast lookup
        Map<String, Integer> coachRankMap = new HashMap<>();

        try {
            // Batch fetch ranks for all coaches in a single query
            List<Map<String, Object>> batchRanks = balanceRepository.findBatchCoachRanksInOrganisation(coachIds,
                    organisationId);

            // Convert results to a lookup map
            for (Map<String, Object> rankData : batchRanks) {
                String coachId = (String) rankData.get("coachId");
                Integer rank = ((Number) rankData.get("rank")).intValue();
                coachRankMap.put(coachId, rank);
            }
        } catch (Exception e) {
            log.warn("Failed to batch fetch coach ranks in organization, using individual queries: {}", e.getMessage());
            // Continue with standard mapping if batch processing fails
        }

        leaderboards = leaderboard.getContent().parallelStream()
                .map(balance -> {
                    String coachId = balance.getCoach().getId();

                    // Get primary academy for this coach (could be cached)
                    AcademyDto primaryAcademy = null;
                    AcademyMinDto academyMinDto = null;
                    try {
                        List<AcademyDto> academiesDto = getAcademiesByCoachUserId(coachId);
                        primaryAcademy = academiesDto.isEmpty() ? null : academiesDto.get(0);
                        academyMinDto = modelMapper.map(primaryAcademy, AcademyMinDto.class);
                    } catch (ResourceException e) {
                        log.warn("Could not get academy for coach {}: {}", coachId, e.getMessage());
                    }

                    // Get rank from pre-populated map or try to get it directly
                    Integer rank = coachRankMap.get(coachId);
                    Integer currentStreak = coachStreak.get(coachId) != null
                            ? coachStreak.get(coachId).getCurrentStreak()
                            : 0;
                    Integer longestStreak = coachStreak.get(coachId) != null
                            ? coachStreak.get(coachId).getLongestStreak()
                            : 0;
                    if (rank == null) {
                        rank = balanceRepository.findCoachRankInOrganisation(coachId, organisationId);
                        rank = (rank != null) ? rank : 0;
                    }

                    return buildLeaderboardEntry(balance, coachId, rank, academyMinDto, organisationId, false,
                            searchQuery, currentStreak, longestStreak);
                })
                .toList();

        // Map leaderboard entries with pre-fetched data
        return ResponseBuilder.success(leaderboards, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK,
                leaderboard.getTotalPages(),
                leaderboard.getTotalElements());
    }

    /**
     * Helper method to build a consistent leaderboard entry for organization
     */
    private CoachPointsLeaderboardEntryDto buildLeaderboardEntry(CoachPointsBalance balance, String coachId,
            Integer rank, AcademyMinDto academy, String organisationId, boolean isAcademySpecific,
            String academyId, Integer currentStreak, Integer longestStreak) {

        return CoachPointsLeaderboardEntryDto.builder()
                .coachId(coachId)
                .coach(modelMapper.map(balance.getCoach(), UserProfileMinDto.class))
                .rank(rank != null ? rank : 0)
                .totalPoints(balance.getTotalEarned())
                .currentBalance(balance.getCurrentBalance())
                .vouchersRedeemed(balance.getVouchersRedeemed())
                .displayFormat(formatPointsDisplay(balance))
                .academy(academy)
                .organisationId(organisationId)
                .isAcademySpecific(isAcademySpecific)
                .academyId(academyId)
                .dayStreak(currentStreak != null ? currentStreak : 0)
                .longestStreak(longestStreak != null ? longestStreak : 0)
                .build();
    }

    /**
     * Get the academy leaderboard with time period filtering and search
     * capabilities
     */
    @Override
    public ServiceResponse getAcademyLeaderboard(
            String academyId, String timePeriodStr, String searchQuery, Pageable pageable, RankLevel rankLevel)
            throws ResourceException {

        log.info("Fetching academy leaderboard for academy: {}, period: {}, search: {}, page: {}",
                academyId, timePeriodStr, searchQuery, pageable.getPageNumber());

        // Get academy
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));

        String organisationId = academy.getOrg() != null ? academy.getOrg().getId() : null;

        // Convert time period string to enum
        TimePeriod timePeriod = TimePeriod.fromString(timePeriodStr);

        // Get start date for the time period
        LocalDateTime startDate = timePeriod.getStartDate();

        Specification<CoachPointsBalance> spec;
        if (Objects.equals(RankLevel.GLOBAL, rankLevel)) {
            spec = CoachPointBalanceSpecification.globalLeaderboardSpec(startDate, searchQuery);
        } else if (Objects.equals(RankLevel.ORG, rankLevel)) {
            log.info("org id is: {}", organisationId);
            if (StringUtils.hasText(organisationId)) {
                spec = CoachPointBalanceSpecification.organisationLeaderboardSpec(organisationId, startDate,
                        searchQuery);
            } else {
                log.warn("org id is null. Showing leaderboard on the basis of academyId: {}", academyId);
                spec = CoachPointBalanceSpecification.academyLeaderboardSpec(academyId, startDate, searchQuery);
            }
        } else {
            spec = CoachPointBalanceSpecification.academyLeaderboardSpec(academyId, startDate, searchQuery);
        }

        // Create specification for filtering

        // Query using specification
        Page<CoachPointsBalance> leaderboard = balanceRepository.findAll(spec, pageable);
        List<CoachPointsLeaderboardEntryDto> leaderboards;

        // Performance optimization: batch fetch all coach ranks in a single query
        List<String> coachIds = leaderboard.getContent().stream()
                .map(balance -> balance.getCoach().getId())
                .collect(Collectors.toList());

        // Skip batch processing for empty result
        if (coachIds.isEmpty()) {
            return ResponseBuilder.success(Collections.emptyList(), ApiResponse.LIST_FETCHED_SUCCESSFULLY,
                    HttpStatus.OK);
        }

        // Always initialize the map here
        Map<String, CoachActivityStreak> coachStreak = coachActivityStreakRepository.findByCoachIdIn(coachIds)
                .parallelStream()
                .collect(Collectors.toMap(
                        CoachActivityStreak::getCoachId,
                        Function.identity(),
                        (existing, replacement) -> replacement));

        // Create a map of coach IDs to ranks for fast lookup
        Map<String, Integer> coachRankMap = new HashMap<>();

        try {
            // Batch fetch ranks for all coaches in a single query
            List<Map<String, Object>> batchRanks = balanceRepository.findBatchCoachRanksInAcademy(coachIds, academyId);

            // Convert results to a lookup map
            for (Map<String, Object> rankData : batchRanks) {
                String coachId = (String) rankData.get("coachId");
                Integer rank = ((Number) rankData.get("rank")).intValue();
                coachRankMap.put(coachId, rank);
            }

            // For any coach without a rank in the map, try to get organization rank if
            // applicable
            if (organisationId != null) {
                for (String coachId : coachIds) {
                    if (!coachRankMap.containsKey(coachId)) {
                        Integer orgRank = balanceRepository.findCoachRankInOrganisation(coachId, organisationId);
                        if (orgRank != null) {
                            coachRankMap.put(coachId, orgRank);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to batch fetch coach ranks, falling back to individual queries: {}", e.getMessage());
            // Continue with normal processing if batch fetch fails
        }

        leaderboards = leaderboard.getContent().stream()
                .map(balance -> {
                    String coachId = balance.getCoach().getId();
                    AcademyMinDto academyMinDto = mapAcademyToDto(balance.getAcademy());
                    // Get rank from the pre-populated map or use default
                    Integer rank = coachRankMap.getOrDefault(coachId, 0);

                    Integer currentStreak = coachStreak.get(coachId) != null
                            ? coachStreak.get(coachId).getCurrentStreak()
                            : 0;
                    Integer longestStreak = coachStreak.get(coachId) != null
                            ? coachStreak.get(coachId).getLongestStreak()
                            : 0;

                    return buildLeaderboardEntry(balance, coachId, rank, academyMinDto, organisationId, true, academyId,
                            currentStreak, longestStreak);
                })
                .toList();

        // Map leaderboard entries with pre-fetched ranks
        return ResponseBuilder.success(leaderboards, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK,
                leaderboard.getTotalPages(), leaderboard.getTotalElements());
    }

    @Override
    public ServiceResponse getAcademyLeaderboardV2(
            String academyId, String timePeriodStr, String searchQuery, Pageable pageable, RankLevel rankLevel)
            throws ResourceException {

        log.info("Fetching academy leaderboard for academy: {}, period: {}, search: {}, rankLevel: {}, page: {}",
                academyId, timePeriodStr, searchQuery, rankLevel, pageable.getPageNumber());

        // Get academy
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));

        String organisationId = academy.getOrg() != null ? academy.getOrg().getId() : null;

        // Convert time period string to enum
        TimePeriod timePeriod = TimePeriod.fromString(timePeriodStr);
        LocalDateTime startDate = timePeriod.getStartDate();

        // Create a new Pageable without sorting (since sorting is handled in the query)
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        // Fetch aggregated leaderboard based on rank level
        Page<CoachLeaderboardProjection> leaderboard;

        if (Objects.equals(RankLevel.GLOBAL, rankLevel)) {
            leaderboard = balanceRepository.findGlobalLeaderboardAggregated(
                    startDate, searchQuery, unsortedPageable);
        } else if (Objects.equals(RankLevel.ORG, rankLevel)) {
            log.info("org id is: {}", organisationId);
            if (StringUtils.hasText(organisationId)) {
                leaderboard = balanceRepository.findOrganisationLeaderboardAggregated(
                        organisationId, startDate, searchQuery, unsortedPageable);
            } else {
                log.warn("org id is null. Showing leaderboard on the basis of academyId: {}", academyId);
                leaderboard = balanceRepository.findAcademyLeaderboardAggregated(
                        academyId, startDate, searchQuery, unsortedPageable);
            }
        } else {
            leaderboard = balanceRepository.findAcademyLeaderboardAggregated(
                    academyId, startDate, searchQuery, unsortedPageable);
        }

        // Rest of your code remains the same...
        if (leaderboard.isEmpty()) {
            return ResponseBuilder.success(Collections.emptyList(), ApiResponse.LIST_FETCHED_SUCCESSFULLY,
                    HttpStatus.OK);
        }

        List<String> coachIds = leaderboard.getContent().stream()
                .map(CoachLeaderboardProjection::getCoachId)
                .collect(Collectors.toList());

        Map<String, CoachActivityStreak> coachStreakMap = coachActivityStreakRepository.findByCoachIdIn(coachIds)
                .parallelStream()
                .collect(Collectors.toMap(
                        CoachActivityStreak::getCoachId,
                        Function.identity(),
                        (existing, replacement) -> replacement));

        List<CoachPointsLeaderboardEntryDto> leaderboards = new ArrayList<>();
        int rank = (int) pageable.getOffset() + 1;

        for (CoachLeaderboardProjection projection : leaderboard.getContent()) {
            String coachId = projection.getCoachId();
            UserProfile coach = projection.getCoach();

            CoachActivityStreak streak = coachStreakMap.get(coachId);
            Integer currentStreak = streak != null ? streak.getCurrentStreak() : 0;
            Integer longestStreak = streak != null ? streak.getLongestStreak() : 0;

            // AcademyMinDto academyMinDto = mapAcademyToDto(academy);

            UserProfileMinDto coachMinDto = modelMapper.map(coach, UserProfileMinDto.class);

            CoachPointsLeaderboardEntryDto entry = CoachPointsLeaderboardEntryDto.builder()
                    .coachId(coachId)
                    .coach(coachMinDto)
                    .rank(rank)
                    .totalPoints(projection.getTotalPoints().intValue())
                    .academy(null)
                    .academyId(null)
                    .organisationId(null)
                    .isAcademySpecific(rankLevel == RankLevel.ACADEMY)
                    .dayStreak(currentStreak)
                    .longestStreak(longestStreak)
                    .lastActive(projection.getLastUpdatedAt())
                    .build();

            leaderboards.add(entry);
            rank++;
        }

        return ResponseBuilder.success(leaderboards, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK,
                leaderboard.getTotalPages(), leaderboard.getTotalElements());
    }

    /**
     * Get a single coach's leaderboard entry with rank and stats
     * This is used to show the current user's position in the leaderboard
     */
    @Override
    public CoachPointsLeaderboardEntryDto getCoachLeaderboardEntry(String coachId, String academyId, String timePeriod)
            throws ResourceException {

        log.info("Fetching leaderboard entry for coach: {}, academy: {}, period: {}",
                coachId, academyId, timePeriod);

        // Determine if we're looking at academy or organization scope
        boolean isAcademySpecific = academyId != null && !academyId.isEmpty();

        // Find the coach's balance record
        CoachPointsBalance balance = null;
        Integer rank = 0;
        Integer currentStreak = 0;
        Integer longestStreak = 0;
        AcademyMinDto academyDto = null;

        // Academy scope
        balance = balanceRepository.findByCoach_IdAndAcademy_Id(coachId, academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Coach not found in this academy's leaderboard"));

        rank = balanceRepository.findCoachRankInAcademy(coachId, academyId);

        CoachActivityStreak coachStreak = coachActivityStreakRepository.findByCoachIdIn(List.of(coachId)).get(0);

        currentStreak = coachStreak.getCurrentStreak();
        longestStreak = coachStreak.getLongestStreak();

        // Get academy details
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));
        academyDto = mapAcademyToDto(academy);

        // Build the leaderboard entry
        return buildLeaderboardEntry(
                balance,
                coachId,
                rank != null ? rank : 0,
                academyDto,
                null,
                isAcademySpecific,
                isAcademySpecific ? academyId : null,
                currentStreak,
                longestStreak);
    }

    /**
     * Get transaction history for a coach
     */
    @Override
    public Page<CoachPointsTransactionDto> getCoachTransactions(String coachId, Pageable pageable) {
        Page<CoachPointTransaction> transactions = transactionRepository.findByCoachIdOrderByCreatedAtDesc(coachId,
                pageable);

        return transactions.map(transaction -> {
            UserProfile coach = transaction.getCoach();
            Academy academy = transaction.getAcademy();
            return mapTransactionToDto(transaction, coach, academy);
        });
    }

    /**
     * Request redemption of points for a voucher
     * Points are always redeemed from the organization-wide balance,
     * since vouchers are organization-level rewards
     */
    @Override
    @Transactional
    public boolean requestVoucherRedemption(String coachId, String academyId, Long voucherTypeId)
            throws ResourceException {
        // Get coach user profile
        UserProfile coach = userProfileRepository.findById(coachId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Coach not found"));

        // Get voucher type
        CoachVoucherType voucherType = voucherTypeRepository.findByIdAndActiveIsTrue(voucherTypeId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Voucher type not found"));

        // Get organization ID for coach
        String organisationId = getCoachOrganisationId(coachId, academyId);

        // Get balance
        CoachPointsBalance balance = getOrCreateAcademyBalance(coachId, academyId, organisationId);

        // Check if coach has enough points
        if (balance.getCurrentBalance() < voucherType.getPointsCost()) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Insufficient points for voucher redemption");
        }

        // Create redemption request
        CoachVoucherRedemption redemption = CoachVoucherRedemption.builder()
                .id(UUID.randomUUID().toString())
                .coach(coach)
                .voucherType(voucherType)
                .pointsSpent(voucherType.getPointsCost())
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();

        voucherRedemptionRepository.save(redemption);

        // Create point transaction for redemption - organization level only
        CoachPointTransaction transaction = CoachPointTransaction.builder()
                .id(UUID.randomUUID().toString())
                .coach(coach)
                .academy(academyId != null ? Academy.builder().id(academyId).build() : null)
                .organisation(organisationId != null ? Organisation.builder().id(organisationId).build() : null)
                .isAcademySpecific(false) // This is organization-wide
                .points(voucherType.getPointsCost())
                .actionType(IncentiveActionType.REDEEMED)
                .sourceType(IncentiveSourceType.VOUCHER)
                .sourceId(redemption.getId())
                .createdAt(LocalDateTime.now())
                .createdBy(coach)
                .build();

        transactionRepository.save(transaction);

        updateCoachAcademyBalance(coachId, academyId, organisationId, 0, voucherType.getPointsCost(), 1);

        return true;
    }

    /**
     * Get available voucher types for an organization
     * Also returns any global vouchers (not specific to any organization or
     * academy)
     */
    @Override
    public List<Map<String, Object>> getAvailableVoucherTypes(String organisationId) throws ResourceException {
        return getAvailableVoucherTypes(organisationId, null);
    }

    /**
     * Get available voucher types for an academy
     *
     * @param academyId ID of academy
     * @return List of academy-specific vouchers plus organization-wide and global
     *         vouchers
     */
    public List<Map<String, Object>> getAvailableAcademyVoucherTypes(String academyId) throws ResourceException {
        // Get academy
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Academy not found"));

        // Get organization ID from academy
        String organisationId = academy.getOrg() != null ? academy.getOrg().getId() : null;

        return getAvailableVoucherTypes(organisationId, academyId);
    }

    /**
     * Internal method to get available voucher types with flexible
     * academy/organization filtering
     *
     * @param organisationId Organization ID (optional)
     * @param academyId      Academy ID (optional)
     * @return List of voucher types as maps
     */
    private List<Map<String, Object>> getAvailableVoucherTypes(String organisationId, String academyId) {
        List<CoachVoucherType> voucherTypes = new ArrayList<>();

        // Get global vouchers (not specific to any organization or academy)
        List<CoachVoucherType> globalVouchers = voucherTypeRepository
                .findByOrganisationIdIsNullAndAcademyIdIsNullAndActiveTrue();
        voucherTypes.addAll(globalVouchers);

        if (organisationId != null) {
            // Get organization-specific vouchers (not academy-specific)
            List<CoachVoucherType> orgVouchers = voucherTypeRepository
                    .findByOrganisationIdAndIsAcademySpecificFalseAndActiveTrue(organisationId);
            voucherTypes.addAll(orgVouchers);

            if (academyId != null) {
                // Get academy-specific vouchers
                List<CoachVoucherType> academyVouchers = voucherTypeRepository
                        .findByAcademyIdAndIsAcademySpecificTrueAndActiveTrue(academyId);
                voucherTypes.addAll(academyVouchers);
            }
        }

        // Convert to maps
        return voucherTypes.stream().map(voucher -> {
            Map<String, Object> voucherMap = new HashMap<>();
            voucherMap.put("id", voucher.getId());
            voucherMap.put("name", voucher.getName());
            voucherMap.put("description", voucher.getDescription());
            voucherMap.put("pointsCost", voucher.getPointsCost());
            voucherMap.put("value", voucher.getValue());
            voucherMap.put("currency", voucher.getCurrency());
            voucherMap.put("imageUrl", voucher.getImageUrl());
            voucherMap.put("isAcademySpecific", voucher.getIsAcademySpecific());

            if (voucher.getAcademy() != null) {
                voucherMap.put("academyId", voucher.getAcademy().getId());
                voucherMap.put("academyName", voucher.getAcademy().getName());
            }

            if (voucher.getOrganisation() != null) {
                voucherMap.put("organisationId", voucher.getOrganisation().getId());
            }

            return voucherMap;
        }).collect(Collectors.toList());
    }

    /**
     * Update coach points rules
     */
    @Override
    @Transactional
    public void updatePointRules(String organisationId, Map<String, Integer> rules) {
        for (Map.Entry<String, Integer> entry : rules.entrySet()) {
            String actionType = entry.getKey();
            Integer points = entry.getValue();

            // Find existing rule or create new one
            CoachPointRule existingRule = pointRuleRepository
                    .findByActionTypeAndOrganisationIdAndActiveTrue(actionType, organisationId);

            if (existingRule != null) {
                // Update existing rule
                CoachPointRule rule = existingRule;
                rule.setPointsAwarded(points);
                pointRuleRepository.save(rule);
            } else {
                // Create new rule
                Organisation org = null;
                if (organisationId != null) {
                    org = new Organisation();
                    org.setId(organisationId);
                }

                CoachPointRule rule = CoachPointRule.builder()
                        .actionType(actionType)
                        .pointsAwarded(points)
                        .active(true)
                        .organisation(org)
                        .description("Points for " + actionType.toLowerCase())
                        .build();

                pointRuleRepository.save(rule);
            }
        }
    }

    // Helper methods

    /**
     * Update coach academy-specific balance with optimistic locking and retry
     */
    private void updateCoachAcademyBalance(String coachId, String academyId, String organisationId, int pointsEarned,
            int pointsRedeemed, int vouchersRedeemed) {
        boolean updated = false;
        int attempts = 0;
        int maxAttempts = 5;

        while (!updated && attempts < maxAttempts) {
            try {
                attempts++;
                CoachPointsBalance balance = getOrCreateAcademyBalance(coachId, academyId, organisationId);

                balance.setTotalEarned(balance.getTotalEarned() + pointsEarned);
                balance.setTotalRedeemed(balance.getTotalRedeemed() + pointsRedeemed);
                balance.setCurrentBalance(balance.getCurrentBalance() + pointsEarned - pointsRedeemed);
                balance.setVouchersRedeemed(balance.getVouchersRedeemed() + vouchersRedeemed);

                balanceRepository.save(balance);
                updated = true;
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic locking failure when updating coach academy balance, attempt {}", attempts);
                // Will retry if attempts < maxAttempts
            }
        }

        if (!updated) {
            throw new RuntimeException("Failed to update coach academy balance after " + maxAttempts + " attempts");
        }
    }

    /**
     * Get or create academy-specific balance for a coach
     *
     * @param coachId        Coach ID
     * @param academyId      Academy ID
     * @param organisationId Organization ID (associated with the academy)
     * @return Academy-specific balance record
     */
    private CoachPointsBalance getOrCreateAcademyBalance(String coachId, String academyId, String organisationId) {
        // First try to find academy-specific balance
        Optional<CoachPointsBalance> academyBalance = balanceRepository.findByCoach_IdAndAcademy_Id(coachId, academyId);
        if (academyBalance.isPresent()) {
            return academyBalance.get();
        }

        // If academy has organisation, try organisation balance
        // if (organisationId != null) {
        // Optional<CoachPointsBalance> orgBalance =
        // balanceRepository.findByCoach_IdAndOrganisation_Id(coachId,
        // organisationId);
        // if (orgBalance.isPresent()) {
        // return orgBalance.get();
        // }
        // }

        return createNewBalance(coachId, academyId, organisationId);
    }

    private CoachPointsBalance createNewBalance(String coachId, String academyId, String organisationId) {
        CoachPointsBalance coachPointsBalance = CoachPointsBalance.builder()
                .id(UUID.randomUUID().toString())
                .coach(UserProfile.builder().id(coachId).build())
                .academy(academyId != null ? Academy.builder().id(academyId).build() : null)
                .organisation(organisationId != null ? Organisation.builder().id(organisationId).build() : null)
                .isAcademySpecific(academyId != null)
                .totalEarned(0)
                .totalRedeemed(0)
                .currentBalance(0)
                .vouchersRedeemed(0)
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        return coachPointsBalance;
    }

    /**
     * Convert transaction metadata from JSON to Map
     */
    private String metadataToJson(Map<String, Object> metadata) {
        try {
            if (metadata == null) {
                return null;
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Error converting metadata to JSON", e);
            return null;
        }
    }

    /**
     * Convert JSON metadata to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMetadata(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Error parsing metadata JSON", e);
            return null;
        }
    }

    /**
     * Map transaction to DTO
     */
    private CoachPointsTransactionDto mapTransactionToDto(CoachPointTransaction transaction, UserProfile coach,
            Academy academy) {
        return CoachPointsTransactionDto.builder()
                .id(transaction.getId())
                .coachId(coach.getId())
                .coachName(coach.getDisplayName())
                .academyId(academy != null ? academy.getId() : null)
                .academyName(academy != null ? academy.getName() : null)
                .points(transaction.getPoints())
                .actionType(transaction.getActionType())
                .sourceType(transaction.getSourceType())
                .sourceId(transaction.getSourceId())
                .timestamp(transaction.getCreatedAt())
                .metadata(jsonToMetadata(transaction.getMetadata()))
                .description(generateDescription(transaction))
                .build();
    }

    /**
     * Generate human-readable description for transaction
     */
    private String generateDescription(CoachPointTransaction transaction) {
        StringBuilder desc = new StringBuilder();

        if (transaction.getActionType() == IncentiveActionType.EARNED) {
            switch (transaction.getSourceType()) {
                case ATTENDANCE:
                    desc.append("Points earned for marking attendance");
                    break;
                case PERFORMANCE_REPORT:
                    desc.append("Points earned for submitting performance report");
                    break;
                case TIMELINE:
                    desc.append("Points earned for timeline creation");
                    break;
                case MEDIA_ADD:
                    desc.append("Points earned for uploading the analysis");
                    break;
                case MEDIA_SHARE:
                    desc.append("Points earned for sharing the analysis");
                default:
                    desc.append("Points earned");
                    break;
            }
        } else {
            if (transaction.getSourceType() == IncentiveSourceType.VOUCHER) {
                desc.append("Points redeemed for voucher");
            } else {
                desc.append("Points redeemed");
            }
        }

        return desc.toString();
    }

    /**
     * Map Academy to DTO
     */
    private AcademyMinDto mapAcademyToDto(Academy academy) {
        if (academy == null) {
            return null;
        }

        AcademyMinDto dto = new AcademyMinDto();
        dto.setId(academy.getId());
        dto.setName(academy.getName());
        dto.setInternalId(academy.getInternalId());
        return dto;
    }

    /**
     * Format the points display string (e.g., "1 Voucher + 1000 points")
     */
    private String formatPointsDisplay(CoachPointsBalance balance) {
        if (balance.getVouchersRedeemed() > 0) {
            String voucherText = balance.getVouchersRedeemed() == 1 ? "Voucher" : "Vouchers";
            return balance.getVouchersRedeemed() + " " + voucherText + " + " + balance.getCurrentBalance() + " points";
        } else {
            return balance.getCurrentBalance() + " points";
        }
    }

    /**
     * Get the organization ID for a coach
     *
     * @param coachId   ID of coach
     * @param academyId ID of academy
     * @return Organization ID
     */
    private String getCoachOrganisationId(String coachId, String academyId) {
        Optional<CoachAcademyMapping> mappings = coachAcademyMappingRepo
                .findByAcademy_IdAndCoachUserProfile_Id(academyId, coachId);

        if (mappings.isPresent()) {
            // Get the academy's organization
            Academy academy = mappings.get().getAcademy();
            if (academy != null && academy.getOrg() != null) {
                return academy.getOrg().getId();
            }
        }

        return null; // This is valid for standalone academies
    }

    private List<AcademyDto> getAcademiesByCoachUserId(String coachUserId) throws ResourceException {
        List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo.findByCoachUserProfile_Id(coachUserId);

        List<String> academyIds = coachAcademyMappings.stream()
                .map(coachAcademyMapping -> coachAcademyMapping.getAcademy().getId()).toList();

        List<AcademyDto> managedAcacdemy = academyService.getAcademyByManagerUserId(coachUserId);

        List<AcademyDto> academyDtos = academyIds.isEmpty() ? new ArrayList<>()
                : academyService.getAcademyByIds(academyIds);

        if (academyDtos.isEmpty()) {
            academyDtos.addAll(managedAcacdemy);
        }

        return academyDtos;
    }
}
