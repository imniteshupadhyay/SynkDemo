package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.AssessmentSubmissionDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface AssessmentSubmissionService {

	ServiceResponse submitAssessment(AssessmentSubmissionDto submissionDto);

	ServiceResponse getSubmissionById(String submissionId);

	ServiceResponse getSubmissions(GenericFilter filter);

	ServiceResponse getSubmissionByRegistrationId(String registrationId);

}
