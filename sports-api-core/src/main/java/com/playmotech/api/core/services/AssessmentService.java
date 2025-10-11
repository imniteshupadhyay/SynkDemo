package com.playmotech.api.core.services;

import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.dto.AssessmentDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface AssessmentService {

	ServiceResponse createAssessment(AssessmentDto assessmentDto);

	ServiceResponse updateAssessment(String assessmentId, AssessmentDto assessmentDto);

	ServiceResponse getAssessmentById(String assessmentId);

	ServiceResponse getPublishedAssessments(String domainUrl);

	ServiceResponse getClosedAssessments(String domainUrl);

	ServiceResponse getAssessments(GenericFilter filter);

	ServiceResponse getActions();

	ServiceResponse updateAssessmentStatus(String assessmentId, AssessmentStatus status);

	ServiceResponse getParameterConfigs(String sport);

}
