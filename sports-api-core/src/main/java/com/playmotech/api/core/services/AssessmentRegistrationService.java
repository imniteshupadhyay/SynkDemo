package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.AssessmentRegistrationBulkDto;
import com.playmotech.api.core.dto.AssessmentRegistrationDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface AssessmentRegistrationService {

    ServiceResponse registerPlayersForAssessment(AssessmentRegistrationBulkDto registrationDto, String currentUserId);

    ServiceResponse reRegisterPlayerForAssessment(String existingRegistrationId,
                                                  AssessmentRegistrationDto registrationDto);

    ServiceResponse getRegistrationById(String registrationId);

    ServiceResponse getRegistrationsByAssessment(String assessmentId);

    ServiceResponse getRegistrations(GenericFilter filter);

    ServiceResponse cancelRegistration(String registrationId);

    ServiceResponse updateRegistrationPaymentStatus(String assessmentId, List<String> registrationIds);

}
