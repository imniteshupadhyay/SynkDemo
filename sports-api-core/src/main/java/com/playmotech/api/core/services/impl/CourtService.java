package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.config.CourtIconProperties;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Court;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.CourtRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICourtService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourtService implements ICourtService {

	private final ModelMapper modelMapper = new ModelMapper();
	private final CourtRepo courtRepo;
	private final IAcademyService academyService;
	private final CourtIconProperties iconProperties;

	private Map<Sports, String> sportIconMap;

	@PostConstruct
	private void initIconMap() {
		sportIconMap = new EnumMap<>(Sports.class);
		String defaultIcon = iconProperties.getDefaultIcon();
		Map<String, String> sportsIcons = iconProperties.getSports();
		
		// Automatically map all sports to their corresponding icons
		for (Sports sport : Sports.values()) {
			String iconKey = sport.name().toLowerCase().replace("_", "");
			sportIconMap.put(sport, sportsIcons.getOrDefault(iconKey, defaultIcon));
		}
	}

	@Autowired
	public CourtService(final CourtRepo courtRepo, final IAcademyService academyService, final CourtIconProperties iconProperties) {
		this.courtRepo = courtRepo;
		this.academyService = academyService;
		this.iconProperties = iconProperties;
	}

	@Override
	public CourtDto addCourt(String academyId, CourtDto courtDto) throws ResourceException {
		academyService.getAcademyById(academyId);

		if (courtDto.getSports() == null) {
			courtDto.setSports(Sports.BADMINTON);
		}

		List<CourtDto> courtDtos = getCourts(academyId, null, courtDto.getSports());
		if (courtDtos.stream().anyMatch(c -> c.getCourtName().equalsIgnoreCase(courtDto.getCourtName()))) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Court already exists");
		}

		Court court = modelMapper.map(courtDto, Court.class);
		court.setId(UUID.randomUUID().toString());
		court.setCreateOn(Timestamp.from(Instant.now()));
		court.setIsInActive(false);
		court.setAcademy(Academy.builder().id(academyId).build());
		court.setSport(courtDto.getSports());

		// Set court image
		court.setCourtImage(sportIconMap.get(court.getSport()));

		Court saved = courtRepo.save(court);
		CourtDto response = modelMapper.map(saved, CourtDto.class);
		response.setSports(saved.getSport());
		return response;
	}

	@Override
	public CourtDto addCourt(CourtDto courtDto) throws ResourceException {
		if (courtDto.getSports() == null) {
			courtDto.setSports(Sports.BADMINTON);
		}

		List<CourtDto> courtDtos = getCourts(null, courtDto.getSports());
		if (courtDtos.stream().anyMatch(c -> c.getCourtName().equalsIgnoreCase(courtDto.getCourtName()))) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Court already exists");
		}

		Court court = modelMapper.map(courtDto, Court.class);
		court.setId(UUID.randomUUID().toString());
		court.setCreateOn(Timestamp.from(Instant.now()));
		court.setIsInActive(false);
		court.setSport(courtDto.getSports());

		// Set court image
		court.setCourtImage(sportIconMap.get(court.getSport()));

		Court saved = courtRepo.save(court);
		return modelMapper.map(saved, CourtDto.class);
	}

	@Override
	public List<CourtDto> getCourts(String academyId, String searchTxt, Sports sport) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<Court> courts = StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2
				? courtRepo.findByAcademy_IdAndSearchByCourtName(searchTxt, academyId)
				: courtRepo.findByAcademy_Id(academyId);

		if (courts == null || courts.isEmpty())
			return List.of();

		return courts.stream().filter(c -> !c.getIsInActive()).filter(c -> sport.equals(c.getSport()))
				.map(c -> modelMapper.map(c, CourtDto.class)).toList();
	}

	@Override
	public List<CourtDto> getCourts(String searchTxt, Sports sport) throws ResourceException {
		List<Court> courts = StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2
				? courtRepo.findByAcademyIsNull(searchTxt)
				: courtRepo.findByAcademyIsNull();

		if (courts == null || courts.isEmpty())
			return List.of();

		return courts.stream().filter(c -> !c.getIsInActive()).filter(c -> sport.equals(c.getSport()))
				.map(c -> modelMapper.map(c, CourtDto.class)).toList();
	}

	@Override
	public CourtDto getCourt(String academyId, String courtId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Court court = courtRepo.findById(courtId).orElse(null);
		if (court != null && !court.getIsInActive()) {
			return modelMapper.map(court, CourtDto.class);
		}
		throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Court not found");
	}

	@Override
	public CourtDto getCourt(String courtId) throws ResourceException {
		Court court = courtRepo.findByAcademyIsNullAndId(courtId).orElse(null);
		if (court != null && !court.getIsInActive()) {
			return modelMapper.map(court, CourtDto.class);
		}
		throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Court not found");
	}

	@Override
	public void deleteCourt(String academyId, String courtId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Court court = courtRepo.findById(courtId).orElse(null);
		if (court != null) {
			court.setIsInActive(true);
			courtRepo.save(court);
			return;
		}
		throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Court not found");
	}

	@Override
	public void deleteCourt(String courtId) throws ResourceException {
		Court court = courtRepo.findByAcademyIsNullAndId(courtId).orElse(null);
		if (court != null) {
			court.setIsInActive(true);
			courtRepo.save(court);
			return;
		}
		throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Court not found");
	}
}
