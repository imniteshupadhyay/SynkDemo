package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface BulkHistoryService {

	ServiceResponse getHistory(GenericFilter filter);

	ServiceResponse getHistoryById(Long historyId);

}
