package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dto.PaymentReminderConfigDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

/**
 * Service interface for managing payment reminder configurations
 */
public interface IPaymentReminderConfigService {

    /**
     * Get all payment reminder configurations for a specific academy
     * 
     * @param academyId the academy ID
     * @return list of payment reminder configurations
     */
    List<PaymentReminderConfigDto> getConfigsByAcademy(String academyId);

    /**
     * Get a specific payment reminder configuration
     * 
     * @param id the configuration ID
     * @return the payment reminder configuration
     * @throws ResourceException if the configuration is not found
     */
    PaymentReminderConfigDto getConfigById(Long id) throws ResourceException;

    /**
     * Get a specific payment reminder configuration by academy and type
     * 
     * @param academyId    the academy ID
     * @param reminderType the reminder type
     * @return the payment reminder configuration if found, null otherwise
     */
    PaymentReminderConfigDto getConfigByAcademyAndType(String academyId, PaymentReminderType reminderType);

    /**
     * Create a new payment reminder configuration
     * 
     * @param configDto the configuration to create
     * @return the created configuration
     * @throws ResourceException if there's validation error or duplicate config
     */
    PaymentReminderConfigDto createConfig(PaymentReminderConfigDto configDto) throws ResourceException;

    /**
     * Update an existing payment reminder configuration
     * 
     * @param id        the configuration ID to update
     * @param configDto the new configuration data
     * @return the updated configuration
     * @throws ResourceException if the configuration is not found
     */
    PaymentReminderConfigDto updateConfig(Long id, PaymentReminderConfigDto configDto) throws ResourceException;

    /**
     * Delete a payment reminder configuration
     * 
     * @param id the configuration ID to delete
     * @throws ResourceException if the configuration is not found
     */
    void deleteConfig(Long id) throws ResourceException;

    /**
     * Get all enabled payment reminder configurations for all academies
     * 
     * @return list of enabled payment reminder configurations
     */
    List<PaymentReminderConfigDto> getAllEnabledConfigs();

    /**
     * Get all payment reminder configurations
     * 
     * @return list of all payment reminder configurations
     */
    List<PaymentReminderConfigDto> getAllConfigs();

    /**
     * Get payment reminder configurations with filtering and pagination
     * 
     * @param filter    - The generic filter with pagination, sorting and filtering
     *                  parameters
     * @param domainUrl - The request coming (can be null for mobile request)
     * @return ServiceResponse containing filtered and paginated payment reminder
     *         configurations
     */
    ServiceResponse getPaymentReminderConfigs(GenericFilter filter, String domainUrl);
}
