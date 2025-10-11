package com.playmotech.api.core.controllers;

import static com.playmotech.api.core.response.ApiResponse.INVALID_LENGTH_OR_REGEX;
import static com.playmotech.api.core.response.ApiResponse.INVALID_LISTING_FILTERS;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dto.AssessmentRegistrationBulkDto;
import com.playmotech.api.core.dto.AssessmentRegistrationDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AssessmentRegistrationService;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("assessment-registrations")
public class AssessmentRegistrationController {

    private final AssessmentRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<ServiceResponse> registerPlayersForAssessment(
            @RequestBody AssessmentRegistrationBulkDto registrationDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        ServiceResponse response = registrationService.registerPlayersForAssessment(registrationDto,
                currentUser.getUserId());
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping("/{registrationId}/re-register")
    public ResponseEntity<ServiceResponse> reRegisterPlayerForAssessment(@PathVariable String registrationId,
            @RequestBody AssessmentRegistrationDto registrationDto) {
        ServiceResponse response = registrationService.reRegisterPlayerForAssessment(registrationId, registrationDto);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @GetMapping
    public ResponseEntity<ServiceResponse> getRegistrations(
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestParam(name = "assessmentId", required = false) String assessmentId,
            @RequestParam(name = "playerId", required = false) String playerId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
            @RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
            @RequestParam(name = "orderBy", defaultValue = "registeredOn", required = false) String orderBy,
            @RequestParam(name = "pageable", required = false, defaultValue = "true") boolean pageable,
            @RequestParam(name = "ascending", defaultValue = "false", required = false) boolean ascending,
            @RequestParam(name = "playerStatus", defaultValue = "", required = false) List<String> playerStatus,
            @RequestParam(name = "paymentStatus", defaultValue = "", required = false) List<String> paymentStatus,
            @RequestParam(name = "search", defaultValue = "", required = false) String searchText) {

        Timestamp startDateLocal = null;
        Timestamp endDateLocal = null;

        if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
            try {
                startDateLocal = parseTimestamp(startDate);
                endDateLocal = parseTimestamp(endDate);
                if (startDateLocal == null || endDateLocal == null
                        || startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
                    return ResponseBuilder.badRequestEntity(INVALID_LISTING_FILTERS.message);
                }
            } catch (DateTimeParseException e) {
                return ResponseBuilder
                        .badRequestEntity(INVALID_LENGTH_OR_REGEX.message + " yyyy-MM-dd HH:mm:ss.SSSSSS");
            }
        }

        List<String> playerIds = new ArrayList<>();
        if (StringUtils.hasText(playerId)) {
            playerIds.add(playerId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        GenericFilter filter = GenericFilter.builder()
                .userId(currentUser.getUserId())
                .academyId(academyId)
                .playerIds(playerIds)
                .startDate(startDateLocal)
                .endDate(endDateLocal)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .orderBy(orderBy)
                .ascending(ascending)
                .isPageable(pageable)
                .search(searchText)
                .assessmentPlayerRegistration(
                        AssessmentPlayerRegistration.builder()
                                .assessmentId(assessmentId)
                                .playerStatuses(playerStatus)
                                .paymentStatuses(paymentStatus)
                                .build())
                .build();

        ServiceResponse response = registrationService.getRegistrations(filter);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PatchMapping("{registrationId}")
    public ResponseEntity<ServiceResponse> cancelRegistration(@PathVariable String registrationId) {
        ServiceResponse response = registrationService.cancelRegistration(registrationId);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PutMapping("change-payment-status")
    public ResponseEntity<ServiceResponse> updateRegistrationPaymentStatus(
            @RequestParam(name = "id") String assessmentId,
            @RequestBody List<String> registrationIds) {
        ServiceResponse response = registrationService.updateRegistrationPaymentStatus(assessmentId, registrationIds);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    private static Timestamp parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return null;
        }
        try {
            return Timestamp.valueOf(
                    LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
