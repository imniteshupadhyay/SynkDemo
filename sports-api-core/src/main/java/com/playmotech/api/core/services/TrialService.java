package com.playmotech.api.core.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.dto.TrialFeedbackDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface TrialService {
	ServiceResponse createTrial(TrialDto trialDto);

	ServiceResponse updateTrial(TrialDto trialDto);

	ServiceResponse deleteTrial(String id);

	ServiceResponse getTrialById(String id);

	ServiceResponse addPedningReason(String id, String reason);

	ServiceResponse markAsCompleted(String id);

	ServiceResponse addTrialFeedback(String id, TrialFeedbackDto dto);

	ServiceResponse updateTrialTime(String id, LocalDate trialDate, LocalTime trialTime);

	List<Trial> findTrialsByLeadIds(List<String> leadIds);

	void saveAllTrials(List<Trial> trials);

	ServiceResponse addToPending(String id);

	ServiceResponse getAllTrials(GenericFilter filter, String domainUrl);
}
