package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface AssessmentResultService {

	ServiceResponse calculateScores(String assessmentId);

	ServiceResponse calculateGlobalScores();

	ServiceResponse getResults(String assessmentId);

	ServiceResponse getGlobalResults(GenericFilter filter);

	ServiceResponse getKpis(String assessmentId);

}
