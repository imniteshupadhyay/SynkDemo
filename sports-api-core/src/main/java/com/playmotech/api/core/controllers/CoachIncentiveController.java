package com.playmotech.api.core.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.dto.CoachDashboardDto;
import com.playmotech.api.core.dto.CoachPointRulesDto;
import com.playmotech.api.core.dto.CoachPointsBalanceDto;
import com.playmotech.api.core.dto.CoachPointsLeaderboardEntryDto;
import com.playmotech.api.core.dto.CoachPointsTransactionDto;
import com.playmotech.api.core.dto.CoachStreakDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ICoachIncentiveService;
import com.playmotech.api.core.services.ICoachStreakService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing coach incentive points, leaderboards, and voucher
 * redemptions
 */
@Slf4j
@RestController
@RequestMapping("/coach-incentives")
@AllArgsConstructor
public class CoachIncentiveController extends BaseController {

        private final ICoachIncentiveService coachIncentiveService;
        private final ICoachStreakService coachStreakService;

        /**
         * Get the current coach's point balance
         *
         * @return Coach points balance details
         */
        @GetMapping(value = "/balance", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<CoachPointsBalanceDto>> getMyPointsBalance(
                        @RequestParam(required = true) String academyId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

                        CoachPointsBalanceDto pointsBalance = coachIncentiveService
                                        .getCoachPointsBalance(currentUser.getUserId(), academyId);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<CoachPointsBalanceDto>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(pointsBalance)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<CoachPointsBalanceDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get the coach's rank in the organization
         *
         * @return Coach rank (integer value)
         */
        @GetMapping(value = "/rank", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<Map<String, Object>>> getMyRank(
                        @RequestParam(required = true) String academyId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                UserDetail currentUser = (UserDetail) authentication.getPrincipal();

                int rank = coachIncentiveService.getCoachRank(currentUser.getUserId(), academyId);

                return ResponseEntity.status(HttpStatus.OK)
                                .body(Response.<Map<String, Object>>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("success")
                                                .body(Map.of("rank", rank))
                                                .build());
        }

        /**
         * Get a specific coach's point balance (admin or academy owner access)
         *
         * @param coachId ID of the coach
         * @return Coach points balance details
         */
        @GetMapping(value = "/{coachId}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<CoachPointsBalanceDto>> getCoachPointsBalance(
                        @PathVariable("coachId") String coachId,
                        @RequestParam(required = true) String academyId) {
                try {
                        CoachPointsBalanceDto pointsBalance = coachIncentiveService.getCoachPointsBalance(coachId,
                                        academyId);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<CoachPointsBalanceDto>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(pointsBalance)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<CoachPointsBalanceDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get transaction history for the current coach
         *
         * @param page Page number (0-based)
         * @param size Items per page
         * @return Page of transaction history items
         */
        @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<Page<CoachPointsTransactionDto>>> getMyTransactions(
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                UserDetail currentUser = (UserDetail) authentication.getPrincipal();

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<CoachPointsTransactionDto> transactions = coachIncentiveService
                                .getCoachTransactions(currentUser.getUserId(), pageable);

                return ResponseEntity.status(HttpStatus.OK)
                                .body(Response.<Page<CoachPointsTransactionDto>>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("success")
                                                .body(transactions)
                                                .build());
        }

        /**
         * Get the organization leaderboard
         *
         * @param organisationId ID of organization
         * @param page           Page number (0-based)
         * @param size           Items per page
         * @param timePeriod     Optional time period filter (WEEKLY, MONTHLY, YEARLY,
         *                       ALL_TIME)
         * @param searchQuery    Optional search query for coach name
         * @param sortBy         Optional field to sort by (POINTS, STREAK, REDEMPTIONS)
         * @param sortDirection  Optional sort direction (ASC, DESC)
         * @return Page of leaderboard entries
         */
        @GetMapping(value = "/leaderboard/organisation/{organisationId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<ServiceResponse>> getOrganisationLeaderboard(
                        @PathVariable("organisationId") String organisationId,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size,
                        @RequestParam(value = "timePeriod", required = false, defaultValue = "ALL_TIME") String timePeriod,
                        @RequestParam(value = "search", required = false) String searchQuery,
                        @RequestParam(value = "sortBy", required = false, defaultValue = "POINTS") String sortBy,
                        @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection) {
                try {
                        Sort sort = createSort(sortBy, sortDirection);
                        Pageable pageable = PageRequest.of(page, size, sort);

                        ServiceResponse leaderboard = coachIncentiveService
                                        .getOrganisationLeaderboard(
                                                        organisationId,
                                                        timePeriod,
                                                        searchQuery,
                                                        pageable);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<ServiceResponse>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(leaderboard)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<ServiceResponse>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get the academy leaderboard
         *
         * @param academyId     ID of academy
         * @param page          Page number (0-based)
         * @param size          Items per page
         * @param timePeriod    Optional time period filter (WEEKLY, MONTHLY, YEARLY,
         *                      ALL_TIME)
         * @param searchQuery   Optional search query for coach name
         * @param sortBy        Optional field to sort by (POINTS, STREAK, REDEMPTIONS)
         * @param sortDirection Optional sort direction (ASC, DESC)
         * @return Page of leaderboard entries
         */
        @GetMapping(value = "/leaderboard/academy/{academyId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<?>> getAcademyLeaderboard(
                        @PathVariable("academyId") String academyId,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size,
                        @RequestParam(value = "timePeriod", required = false, defaultValue = "ALL_TIME") String timePeriod,
                        @RequestParam(value = "search", required = false) String searchQuery,
                        @RequestParam(value = "sortBy", required = false, defaultValue = "POINTS") String sortBy,
                        @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
                        @RequestParam(value = "ranking", required = false, defaultValue = "ACADEMY") RankLevel ranking) {
                try {
                        Sort sort = createSort(sortBy, sortDirection);
                        Pageable pageable = PageRequest.of(page, size, sort);

                        ServiceResponse leaderboard = coachIncentiveService.getAcademyLeaderboardV2(
                                        academyId,
                                        timePeriod,
                                        searchQuery,
                                        pageable,
                                        ranking);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.builder()
                                                        .status(leaderboard.getHttpStatus().value())
                                                        .message(leaderboard.getMessage())
                                                        .body(leaderboard.getBody())
                                                        .totalPages(leaderboard.getTotalPages())
                                                        .totalElements(leaderboard.getTotalElements())
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<ServiceResponse>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get available voucher types for redemption
         *
         * @param organisationId ID of organization
         * @return List of voucher types
         */
        @GetMapping(value = "/vouchers/available/{organisationId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<Map<String, Object>>>> getAvailableVoucherTypes(
                        @PathVariable("organisationId") String organisationId) {
                try {
                        List<Map<String, Object>> voucherTypes = coachIncentiveService
                                        .getAvailableVoucherTypes(organisationId);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<List<Map<String, Object>>>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(voucherTypes)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<Map<String, Object>>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Request redemption of points for a voucher
         *
         * @param voucherTypeId ID of the voucher type to redeem
         * @return Success status
         */
        @PostMapping(value = "/vouchers/redeem/{voucherTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<Boolean>> redeemVoucher(@PathVariable("voucherTypeId") Long voucherTypeId,
                        @RequestParam(required = true) String academyId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

                        boolean success = coachIncentiveService.requestVoucherRedemption(currentUser.getUserId(),
                                        academyId, voucherTypeId);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<Boolean>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message(success ? "Voucher redemption requested successfully"
                                                                        : "Voucher redemption failed")
                                                        .body(success)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<Boolean>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get dashboard info for the current coach (combined points, rank, and streak)
         */
        @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<CoachDashboardDto>> getMyDashboard() {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        String coachId = currentUser.getUserId();

                        return ResponseEntity.ok(Response.<CoachDashboardDto>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("success")
                                        .body(getDashboardInfo(coachId, null))
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<CoachDashboardDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get academy-specific dashboard for the current coach
         */
        @GetMapping(value = "/dashboard/academy/{academyId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<CoachDashboardDto>> getMyAcademyDashboard(
                        @PathVariable("academyId") String academyId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        String coachId = currentUser.getUserId();

                        return ResponseEntity.ok(Response.<CoachDashboardDto>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("success")
                                        .body(getDashboardInfo(coachId, academyId))
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<CoachDashboardDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get current coach's leaderboard entry with rank information
         * For displaying the user's own position at the bottom of leaderboards
         *
         * @param organisationId ID of organization
         * @param academyId      Optional academy ID (if null, uses organization scope)
         * @param timePeriod     Optional time period filter (WEEKLY, MONTHLY, YEARLY,
         *                       ALL_TIME)
         * @return Leaderboard entry with rank information for the current coach
         */
        @GetMapping(value = "/leaderboard/me", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<CoachPointsLeaderboardEntryDto>> getMyLeaderboardEntry(
                        @RequestParam(required = true) String academyId,
                        @RequestParam(value = "timePeriod", required = false, defaultValue = "ALL_TIME") String timePeriod) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        String coachId = currentUser.getUserId();

                        CoachPointsLeaderboardEntryDto entry = coachIncentiveService.getCoachLeaderboardEntry(
                                        coachId, academyId, timePeriod);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<CoachPointsLeaderboardEntryDto>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(entry)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<CoachPointsLeaderboardEntryDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Get available point rules for an academy
         */
        @GetMapping(value = "/point-rules/academy", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<Map<String, CoachPointRulesDto>>> getAcademyPointRules(
                        @RequestParam(required = true) String academyId) {
                try {
                        boolean hasAcademyId = StringUtils.hasText(academyId);
                        // Retrieve point rules for the academy from service
                        Map<String, CoachPointRulesDto> pointRules = coachIncentiveService.getPointRules(academyId);

                        return ResponseEntity.ok(Response.<Map<String, CoachPointRulesDto>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("success")
                                        .body(pointRules)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<Map<String, CoachPointRulesDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }

        /**
         * Helper method to assemble dashboard information
         */
        /**
         * Helper method to create Sort object based on parameters
         *
         * @param sortBy        Field to sort by (POINTS, STREAK, REDEMPTIONS)
         * @param sortDirection Sort direction (ASC, DESC)
         * @return Sort object for Spring Data
         */
        private Sort createSort(String sortBy, String sortDirection) {
                Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC
                                : Sort.Direction.DESC;

                String sortProperty;
                switch (sortBy.toUpperCase()) {
                        case "STREAK":
                                sortProperty = "dayStreak";
                                break;
                        case "REDEMPTIONS":
                                sortProperty = "vouchersRedeemed";
                                break;
                        case "POINTS":
                        default:
                                sortProperty = "totalEarned";
                                break;
                }

                return Sort.by(direction, sortProperty);
        }

        private CoachDashboardDto getDashboardInfo(String coachId, String academyId) throws ResourceException {
                CoachPointsBalanceDto balance;
                CoachStreakDto streak;
                Integer rank;

                if (academyId != null) {
                        // Get academy-specific data
                        balance = coachIncentiveService.getCoachAcademyPointsBalance(coachId, academyId);
                        streak = coachStreakService.getCoachAcademyStreak(coachId, academyId);
                        rank = balance.getRank(); // Use rank from balance
                } else {
                        // Get organization-wide data
                        balance = coachIncentiveService.getCoachPointsBalance(coachId, academyId);
                        streak = coachStreakService.getCoachStreak(coachId);
                        rank = coachIncentiveService.getCoachRank(coachId, academyId);
                }

                return CoachDashboardDto.builder()
                                .coachId(coachId)
                                .coachName(balance.getCoachName())
                                .profilePictureUrl(balance.getCoachProfilePic())
                                .totalPoints(balance.getTotalEarned())
                                .currentBalance(balance.getCurrentBalance())
                                .vouchersRedeemed(balance.getVouchersRedeemed())
                                .displayFormat(balance.getDisplayFormat())
                                .weeklyRank(rank) // We could add weekly rank in the future
                                .overallRank(rank)
                                .dayStreak(streak.getCurrentStreak())
                                .longestStreak(streak.getLongestStreak())
                                .academyId(balance.getAcademyId())
                                .organisationId(balance.getOrganisationId())
                                .build();
        }
}
