package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dao_postgres.LeadSource;

public interface ILeadSourcesService {

	List<LeadSource> getAllSources();

}
