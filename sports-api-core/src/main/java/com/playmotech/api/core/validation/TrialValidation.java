package com.playmotech.api.core.validation;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;
import com.playmotech.api.core.dao_postgres.TrialFeedback.PerformanceLevel;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.dto.TrialFeedbackDto;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialValidation {

	private final UserProfileRepo userProfileRepository;
	private final AcademyRepo academyRepository;
	private final CourseRepo courseRepository;

	public boolean validateCreate(TrialDto dto) {
		return isValidName(dto.getName()) && isValidNonDate(dto.getTrailDate()) && isValidEmail(dto.getEmail())
				&& isValidPhone(dto.getPhone()) && isValidAddress(dto.getAddress()) && isValidCoach(dto.getCoachId())
				&& isValidGender(dto.getGender()) && isValidDob(dto.getDob()) && isValidSport(dto.getSports());
	}

	public boolean validateFeedback(TrialFeedbackDto dto) {
		return isValidSkil(dto.getTechnicalSkill()) && isValidSkil(dto.getBehaviouralSkill())
				&& isValidSkil(dto.getFitness()) && validateCourse(dto.getCourseId())
				&& isValidPerformance(dto.getPerformanceLevel()) && isValidAgegroup(dto.getAgeGroup())
				&& isValidAddress(dto.getNote()) && isValidStatus(dto.getStatus());
	}

	public boolean validateUpdate(TrialDto dto) {
		return dto.getId() != null && validateCreate(dto);
	}

	private boolean isValidName(String name) {
		return name != null && name.length() >= 2 && name.length() <= 150;
	}

	public boolean isValidNonDate(LocalDate date) {
		return date == null || !date.isBefore(LocalDate.now());
	}

	public boolean isValidDob(LocalDate date) {
		return date != null && date.isBefore(LocalDate.now());
	}

	private boolean isValidSkil(Integer skill) {
		return skill == null || (skill >= 1 && skill <= 5);
	}

	public boolean isValidDate(LocalDate date) {
		return date != null && !date.isBefore(LocalDate.now());
	}

	public boolean isValidTime(LocalTime time) {
		return time != null && !time.isBefore(LocalTime.now());
	}

	private boolean isValidEmail(String email) {
		return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");
	}

	private boolean isValidPhone(String phone) {
		return phone != null && phone.matches("^\\+?[0-9]{10,15}$");
	}

	private boolean isValidCoach(String coachId) {
		return coachId == null || userProfileRepository.existsById(coachId);
	}

	public boolean validateAcademy(String academyId) {
		return academyId != null && academyRepository.existsById(academyId);
	}

	public boolean validateCourse(String courseId) {
		return courseId == null || courseRepository.existsById(courseId);
	}

	private boolean isValidStatus(Trial.TrialStatus status) {
		return status != null;
	}

	private boolean isValidAddress(String address) {
		return address == null || (address.length() >= 1 && address.length() <= 250);
	}

	public boolean isValidReason(String reason) {
		return reason == null || (reason.length() >= 1 && reason.length() <= 250);
	}

	private boolean isValidSport(String sportName) {
		if (sportName == null) {
			return false; // Null is considered valid
		}

		try {
			Sports.valueOf(sportName.toUpperCase()); // Attempt to convert string to enum
			return true;
		} catch (IllegalArgumentException e) {
			return false; // Return false if string doesn't match any enum value
		}
	}

	private boolean isValidStatus(String status) {
		if (status == null) {
			return false; // Null is considered valid
		}

		try {
			TrialStatus.valueOf(status.toUpperCase()); // Attempt to convert string to enum
			return true;
		} catch (IllegalArgumentException e) {
			return false; // Return false if string doesn't match any enum value
		}
	}

	private boolean isValidPerformance(String performace) {
		if (performace == null) {
			return false; // Null is considered valid
		}

		try {
			PerformanceLevel.valueOf(performace.toUpperCase()); // Attempt to convert string to enum
			return true;
		} catch (IllegalArgumentException e) {
			return false; // Return false if string doesn't match any enum value
		}
	}

	private boolean isValidAgegroup(String ageGroup) {
		if (ageGroup == null) {
			return false; // Null is considered valid
		}

		try {
			AgeCategory.valueOf(ageGroup.toUpperCase()); // Attempt to convert string to enum
			return true;
		} catch (IllegalArgumentException e) {
			return false; // Return false if string doesn't match any enum value
		}
	}

	private boolean isValidGender(String gender) {
		if (gender == null) {
			return false; // Null is considered valid
		}

		try {
			Gender.valueOf(gender.toUpperCase()); // Attempt to convert string to enum
			return true;
		} catch (IllegalArgumentException e) {
			return false; // Return false if string doesn't match any enum value
		}
	}
}
