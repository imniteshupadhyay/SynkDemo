package com.playmotech.api.core.validation;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.dto.ActivityDto;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ActivityValidation {

	private final UserProfileRepo userProfileRepository;
	private final AcademyRepo academyRepository;

	public boolean validateCreate(ActivityDto dto) {
		return isValidName(dto.getName()) && isValidDescription(dto.getDescription()) && isValidName(dto.getCategory())
				&& isValidName(dto.getSubcategory()) && isValidName(dto.getName());
	}

	public boolean validateUpdate(ActivityDto dto) {
		return dto.getId() != null && validateCreate(dto);
	}

	private boolean isValidName(String name) {
		return name != null && name.length() >= 2 && name.length() <= 150;
	}

	private boolean isValidDescription(String description) {
		return description == null || (description.length() >= 5 && description.length() <= 500);
	}

}
