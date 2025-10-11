package com.playmotech.api.core.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.LeadSource;
import com.playmotech.api.core.repo.LeadSourceRepo;
import com.playmotech.api.core.services.ILeadSourcesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadSourcesService implements ILeadSourcesService {

	private final LeadSourceRepo leadSourceRepo;

	@Override
	public List<LeadSource> getAllSources() {
		return leadSourceRepo.findAll();
	}

}
