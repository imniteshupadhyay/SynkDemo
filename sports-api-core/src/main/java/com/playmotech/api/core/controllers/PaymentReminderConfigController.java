package com.playmotech.api.core.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig;
import com.playmotech.api.core.dto.PaymentReminderConfigDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.IPaymentReminderConfigService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for payment reminder configurations
 */
@Slf4j
@RestController
@RequestMapping("payment-reminder-configs")
public class PaymentReminderConfigController {

    private final IPaymentReminderConfigService paymentReminderConfigService;

    public PaymentReminderConfigController(IPaymentReminderConfigService paymentReminderConfigService) {
        this.paymentReminderConfigService = paymentReminderConfigService;
    }

    /**
     * Get all payment reminder configurations with advanced filtering and
     * pagination
     * 
     * @param active       Filter by enabled status (default: true)
     * @param orderBy      Sort field (default: createdDate)
     * @param academyId    Filter by specific academy ID
     * @param search       Search term for title/body
     * @param reminderType Filter by reminder type (UPCOMING/OVERDUE)
     * @param currentPage  Current page number (default: 1)
     * @param pageSize     Page size (default: 10)
     * @param ascending    Sort order (default: false - descending)
     * @param pageable     Whether to paginate results (default: false)
     * @param academyIds   Filter by multiple academy IDs
     * @return ServiceResponse with filtered payment reminder configurations
     */
    @GetMapping("/list")
    public ResponseEntity<ServiceResponse> getPaymentReminderConfigsList(
            HttpServletRequest request,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "orderBy", defaultValue = "createdAt", required = false) String orderBy,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestParam(name = "search", defaultValue = "", required = false) String search,
            @RequestParam(name = "reminderType", required = false) String reminderType,
            @RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
            @RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
            @RequestParam(name = "ascending", defaultValue = "false", required = false) boolean ascending,
            @RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
            @RequestParam(name = "academyIds", required = false) List<String> academyIds) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        // Extract domain URL from request header
        String domainUrl = request.getHeader("origin");

        // Validate pagination parameters
        if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
            ServiceResponse errorResponse = new ServiceResponse();
            errorResponse.setStatus(400); // Bad request status
            errorResponse.setMessage("Invalid pagination parameters");
            errorResponse.setHttpStatus(HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(errorResponse, errorResponse.getHttpStatus());
        }

        // Validate orderBy parameter
        String[] allowedOrderByValues = { "id", "title", "body", "type", "createdDate", "notificationTime",
                "frequencyDays", "reminderType" };
        if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
            orderBy = "createdAt";
        }

        // Build generic filter with PaymentReminderConfig for advanced filtering
        PaymentReminderConfig configFilter = new PaymentReminderConfig();
        if (active != null) {
            configFilter.setEnabled(active);
        }

        if (reminderType != null) {
            configFilter.setReminderTypeFilter(reminderType);
        }

        GenericFilter filter = GenericFilter.builder()
                .currentPage(currentPage)
                .pageSize(pageSize)
                .isPageable(pageable)
                .ascending(ascending)
                .orderBy(orderBy)
                .search(search)
                .academyId(academyId)
                .academyIds(academyIds)
                .userId(currentUser.getUserId())
                .paymentReminderConfig(configFilter)
                .build();

        // Add reminder type information if provided
        if (reminderType != null && !reminderType.isEmpty()) {
            filter.setApiCalledFrom("payment-reminder"); // Mark filter source
            filter.setSearch(search); // Set search again in case it gets overridden

            // Store the reminder type in a property the service can read
            // Using userRole as a generic property to store the reminder type
            // filter.setUserRole(reminderType);
        }

        ServiceResponse response = paymentReminderConfigService.getPaymentReminderConfigs(filter, domainUrl);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    /**
     * Get all payment reminder configurations (simple list without filtering)
     * 
     * @return list of all payment reminder configurations
     */
    @GetMapping
    public ResponseEntity<List<PaymentReminderConfigDto>> getAllConfigs() {
        log.info("Fetching all payment reminder configurations");
        List<PaymentReminderConfigDto> configs = paymentReminderConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get all payment reminder configurations for an academy
     * 
     * @param academyId the academy ID
     * @return list of payment reminder configurations
     */
    @GetMapping("academies/{academyId}")
    public ResponseEntity<List<PaymentReminderConfigDto>> getConfigsByAcademy(
            @PathVariable("academyId") String academyId) {
        log.info("Fetching payment reminder configurations for academy: {}", academyId);
        List<PaymentReminderConfigDto> configs = paymentReminderConfigService.getConfigsByAcademy(academyId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Get a specific payment reminder configuration
     * 
     * @param id the configuration ID
     * @return the payment reminder configuration
     * @throws ResourceException if the configuration is not found
     */
    @GetMapping("{id}")
    public ResponseEntity<PaymentReminderConfigDto> getConfigById(@PathVariable("id") Long id)
            throws ResourceException {
        log.info("Fetching payment reminder configuration with id: {}", id);
        PaymentReminderConfigDto config = paymentReminderConfigService.getConfigById(id);
        return ResponseEntity.ok(config);
    }

    /**
     * Create a new payment reminder configuration
     * 
     * @param configDto the configuration to create
     * @return the created configuration
     * @throws ResourceException if there's validation error or duplicate config
     */
    @PostMapping
    public ResponseEntity<?> createConfig(@Valid @RequestBody PaymentReminderConfigDto configDto)
            throws ResourceException {
        try {
            log.info("Creating new payment reminder configuration for academies: {} and type: {}",
                    configDto.getAcademyIds().stream().collect(Collectors.joining(", ")), configDto.getReminderType());
            PaymentReminderConfigDto createdConfig = paymentReminderConfigService.createConfig(configDto);
            return new ResponseEntity<>(createdConfig, HttpStatus.CREATED);
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                    .body(Response.builder().status(e.getErrorCodes().getCustomError())
                            .message(e.getMessage()).build());
        }
    }

    /**
     * Update an existing payment reminder configuration
     * 
     * @param id        the configuration ID to update
     * @param configDto the new configuration data
     * @return the updated configuration
     * @throws ResourceException if the configuration is not found
     */
    @PutMapping("{id}")
    public ResponseEntity<?> updateConfig(@PathVariable("id") Long id,
            @Valid @RequestBody PaymentReminderConfigDto configDto) throws ResourceException {
        try {
            log.info("Updating payment reminder configuration with id: {}", id);
            PaymentReminderConfigDto updatedConfig = paymentReminderConfigService.updateConfig(id, configDto);
            return ResponseEntity.ok(updatedConfig);
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                    .body(Response.builder().status(e.getErrorCodes().getCustomError())
                            .message(e.getMessage()).build());
        }
    }

    /**
     * Delete a payment reminder configuration
     * 
     * @param id the configuration ID to delete
     * @throws ResourceException if the configuration is not found
     */
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable("id") Long id) throws ResourceException {
        try {
            log.info("Deleting payment reminder configuration with id: {}", id);
            paymentReminderConfigService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                    .body(Response.builder().status(e.getErrorCodes().getCustomError())
                            .message(e.getMessage()).build());
        }
    }

    /**
     * Get a specific payment reminder configuration by academy and type
     * 
     * @param academyId    the academy ID
     * @param reminderType the reminder type
     * @return the payment reminder configuration if found, 404 otherwise
     */
    @GetMapping("/academies/{academyId}/type/{reminderType}")
    public ResponseEntity<PaymentReminderConfigDto> getConfigByAcademyAndType(
            @PathVariable("academyId") String academyId,
            @PathVariable("reminderType") PaymentReminderType reminderType) {
        log.info("Fetching payment reminder configuration for academy: {} and type: {}", academyId, reminderType);
        PaymentReminderConfigDto config = paymentReminderConfigService.getConfigByAcademyAndType(academyId,
                reminderType);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
}
