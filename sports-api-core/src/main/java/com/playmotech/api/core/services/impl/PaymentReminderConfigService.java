package com.playmotech.api.core.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig;
import com.playmotech.api.core.dao_postgres.PaymentReminderEmailConfig;
import com.playmotech.api.core.dao_postgres.PaymentReminderMobileConfig;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.PaymentReminderConfigDto;
import com.playmotech.api.core.dto.PaymentReminderEmailConfigDto;
import com.playmotech.api.core.dto.PaymentReminderMobileConfigDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.PaymentReminderConfigRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.IPaymentReminderConfigService;
import com.playmotech.api.core.specification.PaymentReminderConfigSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentReminderConfigService implements IPaymentReminderConfigService {

    private final PaymentReminderConfigRepo paymentReminderConfigRepo;
    private final AcademyRepo academyRepo;
    private final AcademyDomainUtil academyDomainUtil;
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;
    private final UserProfileRepo userProfileRepo;
    private final ModelMapper modelMapper = new ModelMapper();

    public PaymentReminderConfigService(PaymentReminderConfigRepo paymentReminderConfigRepo, AcademyRepo academyRepo,
            AcademyDomainUtil academyDomainUtil, CoachAcademyMappingRepo coachAcademyMappingRepo,
            UserProfileRepo userProfileRepo) {
        this.paymentReminderConfigRepo = paymentReminderConfigRepo;
        this.academyRepo = academyRepo;
        this.academyDomainUtil = academyDomainUtil;
        this.coachAcademyMappingRepo = coachAcademyMappingRepo;
        this.userProfileRepo = userProfileRepo;
    }

    @Override
    public List<PaymentReminderConfigDto> getConfigsByAcademy(String academyId) {
        List<PaymentReminderConfig> configs = paymentReminderConfigRepo.findByAcademyId(academyId);
        return configs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentReminderConfigDto getConfigById(Long id) throws ResourceException {
        PaymentReminderConfig config = paymentReminderConfigRepo.findById(id)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Payment reminder configuration not found"));
        return mapToDto(config);
    }

    @Override
    public PaymentReminderConfigDto getConfigByAcademyAndType(String academyId, PaymentReminderType reminderType) {
        Optional<PaymentReminderConfig> configOpt = paymentReminderConfigRepo
                .findByAcademyIdAndReminderType(academyId, reminderType);
        return configOpt.map(this::mapToDto).orElse(null);
    }

    @Override
    public PaymentReminderConfigDto createConfig(PaymentReminderConfigDto configDto) throws ResourceException {
        // Validate academyIds
        if (configDto.getAcademyIds() == null || configDto.getAcademyIds().isEmpty()) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "At least one academy must be provided");
        }

        // Find all academy entities
        Set<Academy> academies = findAcademies(configDto.getAcademyIds());

        // Check for existing configurations with same academy and reminder type
        for (Academy academy : academies) {
            if (paymentReminderConfigRepo.existsByAcademiesContainingAndReminderType(academy,
                    configDto.getReminderType())) {
                // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                // "Configuration already exists for academy id: " + academy.getId() +
                // " and reminder type: " + configDto.getReminderType());
                throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Configuration already exists for the academy");
            }
        }

        // Create new configuration
        PaymentReminderConfig config = new PaymentReminderConfig();
        config.setAcademies(academies);
        config.setReminderType(configDto.getReminderType());

        // channel flags
        config.setSendPushNotification(Boolean.TRUE.equals(configDto.getSendPushNotification()));
        config.setSendEmailNotification(Boolean.TRUE.equals(configDto.getSendEmailNotification()));

        // create and attach child configs if present
        if (config.getSendPushNotification() && configDto.getMobileConfig() != null) {
            PaymentReminderMobileConfig mobileCfg = PaymentReminderMobileConfig.builder()
                    .config(config)
                    .titleTemplate(configDto.getMobileConfig().getTitleTemplate())
                    .messageTemplate(configDto.getMobileConfig().getMessageTemplate())
                    .build();
            config.setMobileConfig(mobileCfg);
        }
        if (config.getSendEmailNotification() && configDto.getEmailConfig() != null) {
            PaymentReminderEmailConfig emailCfg = PaymentReminderEmailConfig.builder()
                    .config(config)
                    .subjectTemplate(configDto.getEmailConfig().getSubjectTemplate())
                    .messageBody(configDto.getEmailConfig().getMessageBody())
                    .build();
            config.setEmailConfig(emailCfg);
        }

        config.setNotificationTime(configDto.getNotificationTime());
        config.setFrequencyDays(configDto.getFrequencyDays());
        config.setEnabled(configDto.getEnabled());

        PaymentReminderConfig savedConfig = paymentReminderConfigRepo.save(config);
        log.info("Created new payment reminder configuration id: {} for {} academies, type: {}",
                savedConfig.getId(), savedConfig.getAcademies().size(), savedConfig.getReminderType());

        return mapToDto(savedConfig);
    }

    @Override
    public PaymentReminderConfigDto updateConfig(Long id, PaymentReminderConfigDto configDto)
            throws ResourceException {
        PaymentReminderConfig existingConfig = paymentReminderConfigRepo.findById(id)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Payment reminder configuration not found with id: " + id));

        // Validate academyIds
        if (configDto.getAcademyIds() == null || configDto.getAcademyIds().isEmpty()) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "At least one academy must be provided");
        }

        // Find all academy entities
        Set<Academy> newAcademies = findAcademies(configDto.getAcademyIds());

        // Check for existing configurations with same academy and reminder type
        // Only check academies that weren't already part of this config
        for (Academy academy : newAcademies) {
            if (!existingConfig.getAcademies().contains(academy) &&
                    paymentReminderConfigRepo.existsByAcademiesContainingAndReminderType(academy,
                            configDto.getReminderType())) {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                        "Configuration already exists for academy: " + academy.getName() +
                                " and reminder type: " + configDto.getReminderType());
            }
        }

        // Update the fields
        existingConfig.setAcademies(newAcademies);
        existingConfig.setReminderType(configDto.getReminderType());

        existingConfig.setSendPushNotification(Boolean.TRUE.equals(configDto.getSendPushNotification()));
        existingConfig.setSendEmailNotification(Boolean.TRUE.equals(configDto.getSendEmailNotification()));

        // handle mobile child
        if (existingConfig.getSendPushNotification()) {
            if (existingConfig.getMobileConfig() == null) {
                existingConfig.setMobileConfig(new PaymentReminderMobileConfig());
                existingConfig.getMobileConfig().setConfig(existingConfig);
            }
            if (configDto.getMobileConfig() != null) {
                existingConfig.getMobileConfig().setTitleTemplate(configDto.getMobileConfig().getTitleTemplate());
                existingConfig.getMobileConfig().setMessageTemplate(configDto.getMobileConfig().getMessageTemplate());
            }
        } else {
            existingConfig.setMobileConfig(null);
        }

        // handle email child
        if (existingConfig.getSendEmailNotification()) {
            if (existingConfig.getEmailConfig() == null) {
                existingConfig.setEmailConfig(new PaymentReminderEmailConfig());
                existingConfig.getEmailConfig().setConfig(existingConfig);
            }
            if (configDto.getEmailConfig() != null) {
                existingConfig.getEmailConfig().setSubjectTemplate(configDto.getEmailConfig().getSubjectTemplate());
                existingConfig.getEmailConfig().setMessageBody(configDto.getEmailConfig().getMessageBody());
            }
        } else {
            existingConfig.setEmailConfig(null);
        }

        existingConfig.setNotificationTime(configDto.getNotificationTime());
        existingConfig.setFrequencyDays(configDto.getFrequencyDays());
        existingConfig.setEnabled(configDto.getEnabled());

        PaymentReminderConfig updatedConfig = paymentReminderConfigRepo.save(existingConfig);
        log.info("Updated payment reminder configuration id: {} for {} academies, type: {}",
                updatedConfig.getId(), updatedConfig.getAcademies().size(), updatedConfig.getReminderType());

        return mapToDto(updatedConfig);
    }

    @Override
    public void deleteConfig(Long id) throws ResourceException {
        PaymentReminderConfig config = paymentReminderConfigRepo.findById(id)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                        "Payment reminder configuration not found"));

        // Invert the enabled flag (handles both active & inactive)
        config.setEnabled(!config.getEnabled());
        paymentReminderConfigRepo.save(config);
        log.info("Soft deleted payment reminder configuration id: {}", id);
    }

    @Override
    public List<PaymentReminderConfigDto> getAllEnabledConfigs() {
        return paymentReminderConfigRepo.findByEnabledTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentReminderConfigDto> getAllConfigs() {
        return paymentReminderConfigRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ServiceResponse getPaymentReminderConfigs(GenericFilter filter, String domainUrl) {
        log.info("Fetching payment reminder configs with filter: {}", filter);
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
            UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();

            String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
            filter.setUserRole(userRole);

            // Default sorting if not specified
            if (filter.getOrderBy() == null || filter.getOrderBy().isEmpty()) {
                filter.setOrderBy("createdDate");
                filter.setAscending(false); // Default to newest first
            }

            // Get specification from PaymentReminderConfigSpecification
            Specification<PaymentReminderConfig> spec = new PaymentReminderConfigSpecification(filter, currentUser,
                    coachAcademyMappingRepo);

            // Create pageable object for pagination if requested
            Page<PaymentReminderConfig> page;
            List<PaymentReminderConfig> configList;

            if (filter.isPageable() && filter.getCurrentPage() != null && filter.getPageSize() != null) {
                // Apply pagination and sorting
                Sort sort = Sort.by(filter.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                        filter.getOrderBy());
                Pageable pageable = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize(), sort);
                page = paymentReminderConfigRepo.findAll(spec, pageable);
                configList = page.getContent();

                // Convert entities to DTOs
                List<PaymentReminderConfigDto> dtoList = configList.stream().map(this::mapToDto)
                        .collect(Collectors.toList());

                // Create response with the right structure
                ServiceResponse serviceResponse = new ServiceResponse();
                serviceResponse.setHttpStatus(HttpStatus.OK);

                // Set pagination metadata using field names from ServiceResponse
                serviceResponse.setTotalElements(page.getTotalElements());
                serviceResponse.setTotalPages(page.getTotalPages());

                // Set the list directly as the body to avoid nesting
                serviceResponse.setBody(dtoList);

                return serviceResponse;
            } else {
                // No pagination, just apply sorting
                Sort sort = Sort.by(filter.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                        filter.getOrderBy());
                configList = paymentReminderConfigRepo.findAll(spec, sort);
                List<PaymentReminderConfigDto> dtoList = configList.stream().map(this::mapToDto)
                        .collect(Collectors.toList());

                ServiceResponse serviceResponse = new ServiceResponse();
                serviceResponse.setBody(dtoList);
                serviceResponse.setHttpStatus(HttpStatus.OK);
                return serviceResponse;
            }
        } catch (Exception e) {
            log.error("Error fetching payment reminder configs with filter", e);
            ServiceResponse errorResponse = new ServiceResponse();
            errorResponse.setMessage("Error fetching payment reminder configurations: " + e.getMessage());
            errorResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return errorResponse;
        }
    }

    /**
     * Maps a PaymentReminderConfig entity to a DTO
     * 
     * @param entity The entity to map
     * @return The DTO
     */
    private PaymentReminderConfigDto mapToDto(PaymentReminderConfig entity) {
        PaymentReminderConfigDto dto = modelMapper.map(entity, PaymentReminderConfigDto.class);

        // Map academy IDs
        Set<String> academyIds = entity.getAcademies().stream()
                .map(Academy::getId)
                .collect(Collectors.toSet());
        dto.setAcademyIds(academyIds);

        // map nested mobile config
        if (entity.getMobileConfig() != null) {
            PaymentReminderMobileConfigDto mobileDto = PaymentReminderMobileConfigDto.builder()
                    .id(String.valueOf(entity.getMobileConfig().getId()))
                    .titleTemplate(entity.getMobileConfig().getTitleTemplate())
                    .messageTemplate(entity.getMobileConfig().getMessageTemplate())
                    .build();
            dto.setMobileConfig(mobileDto);
        }

        // map nested email config
        if (entity.getEmailConfig() != null) {
            PaymentReminderEmailConfigDto emailDto = PaymentReminderEmailConfigDto.builder()
                    .id(String.valueOf(entity.getEmailConfig().getId()))
                    .subjectTemplate(entity.getEmailConfig().getSubjectTemplate())
                    .messageBody(entity.getEmailConfig().getMessageBody())
                    .build();
            dto.setEmailConfig(emailDto);
        }

        return dto;
    }

    /**
     * Find academy entities by their IDs
     * 
     * @param academyIds The set of academy IDs
     * @return Set of Academy entities
     * @throws ResourceException if any academy is not found
     */
    private Set<Academy> findAcademies(Set<String> academyIds) throws ResourceException {
        Set<Academy> academies = new HashSet<>();

        for (String academyId : academyIds) {
            Academy academy = academyRepo.findById(academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND,
                            "Academy not found with id: " + academyId));
            academies.add(academy);
        }

        return academies;
    }
}
