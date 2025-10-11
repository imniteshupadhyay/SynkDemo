package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.AcademySportMapping;
import com.playmotech.api.core.helper.AcademyHelper;
import com.playmotech.api.core.repo.AcademySportMappingRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.MiscellaneousService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MiscellaneousServiceImpl implements MiscellaneousService {

	private final AcademySportMappingRepository academySportMappingRepository;

	private final AcademyHelper academyHelper;

	@Override
	public ServiceResponse getAllAgeCategories() {
		List<AgeCategory> ageCategories = List.of(AgeCategory.values());

		if (ageCategories.isEmpty()) {
			return ResponseBuilder.success("No age categories found");
		}

		return ResponseBuilder.success(ageCategories, "Age categories fetched successfully");
	}

	@Override
	public ServiceResponse getAllGenders() {
		List<Gender> genders = List.of(Gender.values());

		if (genders.isEmpty()) {
			return ResponseBuilder.success("No genders found");
		}

		return ResponseBuilder.success(genders, "Genders fetched successfully");
	}

	@Override
	public ServiceResponse getAllSports() {
		List<Sports> sportsList = List.of(Sports.values());

		if (sportsList.isEmpty()) {
			return ResponseBuilder.success(ApiResponse.SPORTS_NOT_FOUND, HttpStatus.OK);
		}

		return ResponseBuilder.success(sportsList, ApiResponse.SPORTS_LIST_FETCHED, HttpStatus.OK);
	}

	@Override
	public ServiceResponse getSportsByAcademy(String academyId) {

		ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);

		if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
			return serviceResponse;
		}

		List<AcademySportMapping> mappings = academySportMappingRepository.findByAcademyId(academyId);
		if (mappings.isEmpty()) {
			return ResponseBuilder.success(ApiResponse.SPORTS_NOT_FOUND, HttpStatus.OK);
		}
		List<Sports> sports = mappings.stream().map(AcademySportMapping::getSport).toList();

		return ResponseBuilder.success(sports, ApiResponse.SPORTS_LIST_FETCHED, HttpStatus.OK);
	}

	@Override
	public ServiceResponse getSportsByAcademies(List<String> academyIds) {

		Set<Sports> uniqueSports = new HashSet<>();

		for (String academyId : academyIds) {
			ServiceResponse serviceResponse = academyHelper.fetchAcademyById(academyId);

			if (!serviceResponse.getHttpStatus().is2xxSuccessful()) {
				return serviceResponse;
			}

			List<AcademySportMapping> mappings = academySportMappingRepository.findByAcademyId(academyId);
			if (!mappings.isEmpty()) {
				List<Sports> sports = mappings.stream().map(AcademySportMapping::getSport).toList();
				uniqueSports.addAll(sports);
			}
		}

		if (uniqueSports.isEmpty()) {
			return ResponseBuilder.success(ApiResponse.SPORTS_NOT_FOUND, HttpStatus.OK);
		}

		return ResponseBuilder.success(new ArrayList<>(uniqueSports), ApiResponse.SPORTS_LIST_FETCHED, HttpStatus.OK);
	}

}
