package com.playmotech.api.core.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.playmotech.api.core.dao_postgres.CoachPointTransaction;
import com.playmotech.api.core.dao_postgres.CoachPointTransactionError;
import com.playmotech.api.core.repo.CoachPointTransactionErrorRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CoachPointTransactionErrorService {
    private final CoachPointTransactionErrorRepository coachPointTransactionErrorRepository;
    private final ObjectMapper objectMapper;

    public CoachPointTransactionErrorService(CoachPointTransactionErrorRepository coachPointTransactionErrorRepository,
            ObjectMapper objectMapper) {
        this.coachPointTransactionErrorRepository = coachPointTransactionErrorRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTransactionError(CoachPointTransaction failedTransaction, Exception exception) {

        String jsonString = "";
        Map<String, Object> entityJson = new HashMap<>();
        entityJson.put("id", failedTransaction.getId());
        entityJson.put("coachId", failedTransaction.getCoach() != null ? failedTransaction.getCoach().getId() : null);
        entityJson.put("academyId",
                failedTransaction.getAcademy() != null ? failedTransaction.getAcademy().getId() : null);
        entityJson.put("organisationId",
                failedTransaction.getOrganisation() != null ? failedTransaction.getOrganisation().getId() : null);
        entityJson.put("isAcademySpecific", failedTransaction.getIsAcademySpecific());
        entityJson.put("points", failedTransaction.getPoints());
        entityJson.put("actionType",
                failedTransaction.getActionType() != null ? failedTransaction.getActionType().toString() : null);
        entityJson.put("sourceType",
                failedTransaction.getSourceType() != null ? failedTransaction.getSourceType().toString() : null);
        entityJson.put("sourceId", failedTransaction.getSourceId());
        entityJson.put("metadata", failedTransaction.getMetadata());
        entityJson.put("createdBy",
                failedTransaction.getCreatedBy() != null ? failedTransaction.getCreatedBy().getId() : null);
        entityJson.put("createdAt",
                failedTransaction.getCreatedAt() != null ? failedTransaction.getCreatedAt().toString() : null);

        try {
            jsonString = objectMapper.writeValueAsString(entityJson);
        } catch (JsonProcessingException jpe) {
            log.error("Error while processing entity json: {}", jpe.getMessage());
            jsonString = entityJson.toString();
        }

        CoachPointTransactionError errorEntity = CoachPointTransactionError.builder()
                .id(UUID.randomUUID().toString())
                .originalTransactionId(failedTransaction.getId())
                .errorMessage(exception.getMessage())
                .failedEntityData(jsonString)
                .errorTime(LocalDateTime.now())
                .build();

        coachPointTransactionErrorRepository.save(errorEntity);
        log.info("Data successfully persisted in the [coach_point_transaction_error] table");
    }
}
