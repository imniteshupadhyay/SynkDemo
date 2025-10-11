package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.dao.LeadsDao;
import com.playmotech.api.core.dao_postgres.Leads;
import com.playmotech.api.core.dto.LeadsDto;

@Component
public class LeadsMapper {
	public LeadsDto toDto(Leads lead) {
		LeadsDto dto = new LeadsDto();
		BeanUtils.copyProperties(lead, dto);
		dto.setLeadStatus(lead.getLeadStatus() != null ? lead.getLeadStatus() : null);
		dto.setLeadSource(lead.getLeadSource() != null ? lead.getLeadSource() : null);
		dto.setAssignedCoach(lead.getAssignedCoach() != null ? lead.getAssignedCoach().getDisplayName() : "N/A");
		dto.setAssignedAcademy(lead.getAssignedAcademy() != null ? lead.getAssignedAcademy().getName() : "N/A");
		dto.setLeadAssignedBy(lead.getLeadCreatedBy() != null ? lead.getLeadCreatedBy().getDisplayName() : "N/A");
		return dto;
	}

	public List<LeadsDao> mapDtoListToDaoList(List<LeadsDto> leadsListDto) {
		return leadsListDto.stream().map(each -> {
			LeadsDao dao = new LeadsDao();
			BeanUtils.copyProperties(each, dao);

			if (each.getGender() != null) {
				dao.setGender(each.getGender().name());
			}
			if (each.getSports() != null) {
				dao.setSports(each.getSports().name());
			}
			if (each.getAgeCategory() != null) {
				dao.setAgeCategory(each.getAgeCategory().getLabel());
			}
			if (each.getLeadSource() != null) {
				dao.setLeadSource(each.getLeadSource().getName());
			}
			if (each.getLeadStatus() != null) {
				dao.setStatus(each.getLeadStatus().name());
			}

			if (each.getTrialStatus() != null) {
				dao.setTrialStatus(each.getTrialStatus());
			}

			return dao;
		}).collect(Collectors.toList());
	}

}
