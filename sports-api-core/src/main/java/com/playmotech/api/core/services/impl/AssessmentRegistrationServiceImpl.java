package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.RegistrationPaymentStatus;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AssessmentRegistrationBulkDto;
import com.playmotech.api.core.dto.AssessmentRegistrationDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.repo.AssessmentPlayerRegistrationRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AssessmentRegistrationService;
import com.playmotech.api.core.specification.AssessmentRegistrationSpecification;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentRegistrationServiceImpl implements AssessmentRegistrationService {

    private final ModelMapper modelMapper;
    private final AssessmentPlayerRegistrationRepository registrationRepo;
    private final EntityManager entityManager;

    @Override
    public ServiceResponse registerPlayersForAssessment(AssessmentRegistrationBulkDto registrationDto,
            String currentUserId) {
        try {
            // Validate root-level fields
            if (registrationDto.getAssessmentId() == null || registrationDto.getAssessmentId().isBlank()) {
                return ResponseBuilder.badRequest("Assessment ID is required.");
            }

            if (registrationDto.getRegistrationDate() == null) {
                return ResponseBuilder.badRequest("Registration date is required.");
            }

            List<AssessmentRegistrationDto> players = registrationDto.getPlayers();
            if (players == null || players.isEmpty()) {
                return ResponseBuilder.badRequest("At least one player must be provided for registration.");
            }

            // Validate each player's registrationNumber
            List<String> invalidPlayers = new ArrayList<>();

            for (AssessmentRegistrationDto playerDto : players) {
                if (playerDto.getRegistrationNumber() == null || playerDto.getRegistrationNumber().isBlank()) {
                    String identifier = (playerDto.getPlayerId() != null) ? playerDto.getPlayerId() : "Unknown Player";
                    invalidPlayers.add(identifier);
                }
            }

            if (!invalidPlayers.isEmpty()) {
                String message = "Registration number is missing for the following player IDs: "
                        + String.join(", ", invalidPlayers);
                return ResponseBuilder.badRequest(message);
            }

            // Get reference to Assessment (assumes it exists)
            Assessment assessmentRef = entityManager.getReference(Assessment.class, registrationDto.getAssessmentId());

            // Map and prepare registrations
            List<AssessmentPlayerRegistration> registrations = players.stream()
                    .filter(playerDto -> !StringUtils.hasText(playerDto.getRegistrationId()))
                    .map(playerDto -> {
                        AssessmentPlayerRegistration registration = modelMapper.map(playerDto,
                                AssessmentPlayerRegistration.class);

                        // Use entity references instead of builder
                        registration.setAssessment(assessmentRef);
                        if (playerDto.getPlayerId() != null) {
                            registration
                                    .setPlayer(entityManager.getReference(UserProfile.class, playerDto.getPlayerId()));
                        }
                        if (playerDto.getAcademyId() != null) {
                            registration
                                    .setAcademy(entityManager.getReference(Academy.class, playerDto.getAcademyId()));
                        }

                        registration.setRegistrationDate(registrationDto.getRegistrationDate());
                        registration.setPlayerStatus(PlayerStatus.REGISTERED);
                        registration
                                .setPaymentStatus(playerDto.getPaymentStatus() != null ? playerDto.getPaymentStatus()
                                        : RegistrationPaymentStatus.PENDING);
                        registration.setRegistrationNumber(playerDto.getRegistrationNumber());
                        registration.setCreatedBy(currentUserId);

                        return registration;
                    })
                    .toList();

            registrationRepo.saveAll(registrations);

            return ResponseBuilder.success("Players registered successfully.");
        } catch (Exception e) {
            log.error("Error registering players: {}", e.getMessage(), e);
            return ResponseBuilder.error(null, "An unexpected error occurred while registering players.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ServiceResponse reRegisterPlayerForAssessment(String existingRegistrationId,
            AssessmentRegistrationDto registrationDto) {
        Optional<AssessmentPlayerRegistration> existing = registrationRepo.findById(existingRegistrationId);
        if (existing.isEmpty()) {
            return ResponseBuilder.notFound("No existing registration found");
        }

        AssessmentPlayerRegistration registration = existing.get();

        // Allow updating limited fields
        registration.setAcademy(Academy.builder().id(registrationDto.getAcademyId()).build());
        registration.setPlayer(UserProfile.builder().id(registrationDto.getPlayerId()).build());
        registration.setAssessment(Assessment.builder().id(registrationDto.getAssessmentId()).build());

        registration.setGender(registrationDto.getGender());
        registration.setPlayerStatus(registrationDto.getPlayerStatus());

        registrationRepo.save(registration);

        return ResponseBuilder.success("Player re-registered successfully");
    }

    @Override
    // DONE
    public ServiceResponse getRegistrationById(String id) {
        Optional<AssessmentPlayerRegistration> registration = registrationRepo.findById(id);
        if (registration.isEmpty()) {
            return ResponseBuilder.success(Optional.empty(), "No registration found");
        }
        return ResponseBuilder.success(convertToDto(registration.get()), "Registration found");
    }

    @Override
    public ServiceResponse getRegistrationsByAssessment(String assessmentId) {
        List<AssessmentPlayerRegistration> registrations = registrationRepo.findAll().stream()
                .filter(r -> r.getAssessment().getId().equals(assessmentId)).toList();

        if (registrations.isEmpty()) {
            return ResponseBuilder.success(List.of(), "No registrations found for assessment");
        }

        List<AssessmentRegistrationDto> result = registrations.stream().map(this::convertToDto).toList();

        return ResponseBuilder.success(result, "Registrations found for assessment");
    }

    @Override
    public ServiceResponse getRegistrations(GenericFilter filter) {
        // Build specification
        Specification<AssessmentPlayerRegistration> spec = new AssessmentRegistrationSpecification(filter);

        // Pageable for pagination + sorting
        Pageable pageable = PageRequest.of(filter.getCurrentPage() != null ? filter.getCurrentPage() - 1 : 0,
                filter.getPageSize() != null ? filter.getPageSize() : 10);

        Page<AssessmentPlayerRegistration> registrations = Page.empty();
        List<AssessmentPlayerRegistration> registrations2 = new ArrayList<>();
        // Fetch filtered & paginated result
        if (filter.isPageable()) {
            registrations = registrationRepo.findAll(spec, pageable);

            // Convert to DTO
            List<AssessmentRegistrationDto> result = registrations.getContent()
                    .stream()
                    .map(this::convertToDto)
                    .toList();

            // Build response with pagination metadata
            return ResponseBuilder.success(result, ApiResponse.FETCHED_LIST, registrations.getTotalPages(),
                    registrations.getTotalElements());

        } else {
            registrations2 = registrationRepo.findAll(spec);

            List<AssessmentRegistrationDto> result = registrations2
                    .stream()
                    .map(this::convertToDto)
                    .toList();
            return ResponseBuilder.success(result, ApiResponse.FETCHED_LIST);
        }
    }

    @Override
    // DONE
    public ServiceResponse cancelRegistration(String registrationId) {
        Optional<AssessmentPlayerRegistration> playerRegistrationOptional = registrationRepo.findById(registrationId);
        if (playerRegistrationOptional.isEmpty()) {
            return ResponseBuilder.notFound("No player registration found");
        }

        AssessmentPlayerRegistration playerRegistration = playerRegistrationOptional.get();

        playerRegistration.setPlayerStatus(AssessmentPlayerRegistration.PlayerStatus.WITHDRAWN);

        // If a player is withdrawn and the existing payment status is PAID, mark status
        // as REFUNDED. Otherwise, keep it as it is.
        if (playerRegistration.getPaymentStatus() == RegistrationPaymentStatus.PAID) {
            playerRegistration.setPaymentStatus(RegistrationPaymentStatus.REFUNDED);
        }

        registrationRepo.save(playerRegistration);

        return ResponseBuilder.success("Player registration cancelled successfully");
    }

    @Override
    // DONE
    public ServiceResponse updateRegistrationPaymentStatus(String assessmentId, List<String> registrationIds) {
        try {
            if (registrationIds.size() == 0) {
                // return not found
                return ResponseBuilder.badRequest("Empty registration ids provided");
            }
            List<AssessmentPlayerRegistration> registrationList = registrationRepo
                    .findByRegistrationIdIn(registrationIds);

            registrationList.forEach(each -> each.setPaymentStatus(RegistrationPaymentStatus.PAID));

            registrationRepo.saveAll(registrationList);
            return ResponseBuilder.success("Payment status updated to " + RegistrationPaymentStatus.PAID.name());
        } catch (Exception e) {
            log.error("Something unexpected happened: {}", e.getMessage());
            return ResponseBuilder.error(null, "Something unexpected happened. Please try again",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private AssessmentRegistrationDto convertToDto(AssessmentPlayerRegistration registration) {
        AssessmentRegistrationDto registrationDto = modelMapper.map(registration, AssessmentRegistrationDto.class);

        registrationDto.setAssessmentId(registration.getAssessment().getId());

        registrationDto.setPlayerId(registration.getPlayer().getId());
        registrationDto.setPlayerProfile(modelMapper.map(registration.getPlayer(), UserProfileMinDto.class));

        if (registration.getAcademy() != null) {
            registrationDto.setAcademy(modelMapper.map(registration.getAcademy(), AcademyDto.class));
            registrationDto.setAcademyId(registration.getAcademy().getId());
        }

        return registrationDto;
    }

    // Utility method for generating registration numbers
    private String generateRegistrationNumber(AssessmentPlayerRegistration registration) {
        return "REG-" + registration.getAssessment().getId() + "-" + System.currentTimeMillis();
    }

}
