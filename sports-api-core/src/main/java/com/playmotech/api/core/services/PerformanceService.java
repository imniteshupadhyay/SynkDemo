package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface PerformanceService {

	ServiceResponse getTraineePerformanceList(GenericFilter filter);

	ServiceResponse getCoachPerformanceList(GenericFilter filter);

}
