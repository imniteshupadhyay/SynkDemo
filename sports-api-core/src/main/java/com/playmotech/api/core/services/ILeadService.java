package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.AddEditLeadDto;
import com.playmotech.api.core.dto.LeadsDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.utils.GenericFilter;

public interface ILeadService {
	Response<?> getAllLeads(GenericFilter filter, String domainUrl);

	LeadsDto addLead(AddEditLeadDto addLeadDto) throws ResourceException;

	LeadsDto updateLead(String id, AddEditLeadDto editLeadDto) throws ResourceException;

	void inactivateLead(String id) throws ResourceException;

	void assignLeads(String coachId, String academyId, List<String> leads, boolean reassign) throws ResourceException;

	LeadsDto getLeadById(String id) throws ResourceException;

}
