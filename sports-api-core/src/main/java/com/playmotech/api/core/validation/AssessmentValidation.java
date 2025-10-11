package com.playmotech.api.core.validation;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.dto.AssessmentDto;
import com.playmotech.api.core.dto.AssessmentParameterDto;
import com.playmotech.api.core.repo.AcademyRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AssessmentValidation {

	private final AcademyRepo academyRepository;

	private static final String TITLE_REGEX = "^.{5,100}$";
	private static final String DESCRIPTION_REGEX = "^.{10,1000}$";
	private static final String LOCATION_REGEX = "^.{3,200}$";
	private static final String PARAMETER_NAME_REGEX = "^.{3,50}$";

	public boolean validate(AssessmentDto dto) {
		// If forAllAcademies is true, skip academy ID validation
		if (dto.getForAllAcademies() == null || !dto.getForAllAcademies()) {
			if (!validateAcademyIds(dto.getAcademyIds())) {
				return false;
			}
		}
		
		return isNotEmpty(dto.getAssessmentTitle(), dto.getStartDate(), dto.getEndDate())
				&& validateTitle(dto.getAssessmentTitle()) 
				&& validateDescription(dto.getAssessmentDescription())
				&& validateLocation(dto.getLocation()) 
				&& validateDates(dto.getStartDate(), dto.getEndDate())
				&& validateParameters(dto.getParameters())
				&& validateRequiredFields(dto);
	}

	public boolean validateForUpdate(AssessmentDto dto) {
		return dto.getId() != null && validate(dto) && validateStatus(dto.getAssessmentStatus());
	}

	private boolean validateTitle(String title) {
		return isValidRegex(title, TITLE_REGEX);
	}

	private boolean validateDescription(String description) {
		return description == null || isValidRegex(description, DESCRIPTION_REGEX);
	}

	private boolean validateLocation(String location) {
		return location == null || isValidRegex(location, LOCATION_REGEX);
	}

	private boolean validateDates(Date startDate, Date endDate) {
		return startDate != null && endDate != null && !startDate.after(endDate);
	}

	private boolean validateStatus(AssessmentStatus status) {
		return status != null;
	}

	private boolean validateAcademyIds(List<String> academyIds) {
		if (academyIds == null || academyIds.isEmpty())
			return false;

		return academyIds.stream().allMatch(id -> id != null && academyRepository.existsById(id));
	}

	private boolean validateParameters(List<AssessmentParameterDto> parameters) {
		if (parameters == null || parameters.isEmpty())
			return false;

		return parameters.stream().allMatch(param -> isNotEmpty(param.getParameterName(), param.getParameterWeight(),
				param.getParameterType(), param.getUnitType(), param.getComparisonType(), param.getVideoUploadEnabled())
				&& isValidRegex(param.getParameterName(), PARAMETER_NAME_REGEX) && param.getParameterWeight() > 0);
	}

	private boolean validateRequiredFields(AssessmentDto dto) {
		return dto.getSport() != null && dto.getAgeCategory() != null && dto.getGender() != null;
	}

	// --- Helper Methods ---

	private static boolean isValidRegex(String value, String regex) {
		return value != null && value.matches(regex);
	}

	private static boolean isNotEmpty(Object... fields) {
		return Arrays.stream(fields).allMatch(field -> {
			if (field instanceof String) {
				return !((String) field).isBlank();
			}
			return field != null;
		});
	}
}
