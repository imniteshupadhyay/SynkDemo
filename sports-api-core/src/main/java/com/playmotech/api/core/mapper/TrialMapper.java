package com.playmotech.api.core.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.Trial.TrialStatus;
import com.playmotech.api.core.dao_postgres.TrialFeedback;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.response.dao.AcademyDao;
import com.playmotech.api.core.response.dao.TrialDao;
import com.playmotech.api.core.response.dao.TrialExportDao;
import com.playmotech.api.core.response.dao.TrialFeedbackDao;
import com.playmotech.api.core.response.dao.UserProfileDao;

public class TrialMapper {

	public static TrialDao mapEntityToDao(Trial entity) {
		TrialDao dao = new TrialDao();
		BeanUtils.copyProperties(entity, dao);

		if (entity.getCoach() != null) {
			UserProfileDao coach = new UserProfileDao();
			coach.setId(entity.getCoach().getId());
			coach.setUsername(entity.getCoach().getUsername());
			coach.setDisplayName(entity.getCoach().getDisplayName());
			dao.setCoach(coach);
		}
		if (entity.getCreatedBy() != null) {
			UserProfileDao user = new UserProfileDao();
			user.setId(entity.getCreatedBy().getId());
			user.setUsername(entity.getCreatedBy().getUsername());
			user.setDisplayName(entity.getCreatedBy().getDisplayName());
			dao.setCreatedBy(user);
		}
		if (entity.getUpdatedBy() != null) {
			UserProfileDao user = new UserProfileDao();
			user.setId(entity.getUpdatedBy().getId());
			user.setUsername(entity.getUpdatedBy().getUsername());
			user.setDisplayName(entity.getUpdatedBy().getDisplayName());
			dao.setUpdatedBy(user);
		}
		if (entity.getAcademy() != null) {
			AcademyDao academyDao = new AcademyDao();
			academyDao.setId(entity.getAcademy().getId());
			academyDao.setName(entity.getAcademy().getName());
			dao.setAcademy(academyDao);
		}
		if (entity.getFeedback() != null) {
			List<TrialFeedbackDao> feedbackDaoList = new ArrayList<>();

			// Iterate over the list of feedbacks
			for (TrialFeedback feedback : entity.getFeedback()) {
				TrialFeedbackDao feedbackDao = TrialFeedbackMapper.mapEntityToDao(feedback);
				feedbackDaoList.add(feedbackDao);
			}

			// Set the mapped list of feedback DAOs to your DAO
			dao.setFeedback(feedbackDaoList);
		}

		return dao;
	}

	public static TrialExportDao mapEntityToExportDao(Trial entity) {
		TrialExportDao dao = new TrialExportDao();
		BeanUtils.copyProperties(entity, dao);

		if (entity.getCoach() != null) {
			dao.setCoach(entity.getCoach().getDisplayName());
		}
		if (entity.getCreatedBy() != null) {
			dao.setCreatedBy(entity.getCreatedBy().getDisplayName());
		}
		if (entity.getUpdatedBy() != null) {
			dao.setUpdatedBy(entity.getUpdatedBy().getDisplayName());
		}
		if (entity.getAcademy() != null) {
			dao.setAcademy(entity.getAcademy().getName());
		}
		if (entity.getGender() != null) {
			dao.setGender(entity.getGender().toString());
		}
		if (entity.getSports() != null) {
			dao.setSports(entity.getSports().toString());
		}
		if (entity.getStatus() != null) {
			dao.setSports(entity.getStatus().toString());
		}

		return dao;
	}

	public static Trial mapDtoToEntity(TrialDto dto) {
		Trial entity = new Trial();
		BeanUtils.copyProperties(dto, entity);

		if (dto.getCoachId() != null) {
			UserProfile userProfile = new UserProfile();
			userProfile.setId(dto.getCoachId());
			entity.setCoach(userProfile);
		}

		// Academy relationship mapping
		if (dto.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(dto.getAcademyId());
			entity.setAcademy(academy);
		}
		entity.setDeleted(false);
		entity.setCompleted(false);
		entity.setStatus(TrialStatus.PENDING);
		entity.setSports(Sports.valueOf(dto.getSports().toUpperCase()));
		entity.setGender(Gender.valueOf(dto.getGender().toUpperCase()));
		return entity;
	}

	public static Trial mapDtoToExistingEntity(TrialDto dto, Trial existing) {
		BeanUtils.copyProperties(dto, existing);
		if (dto.getCoachId() != null) {
			UserProfile userProfile = new UserProfile();
			userProfile.setId(dto.getCoachId());
			existing.setCoach(userProfile);
		}

		// Academy relationship mapping
		if (dto.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(dto.getAcademyId());
			existing.setAcademy(academy);
		}
		existing.setDeleted(false);
		existing.setCompleted(false);
		existing.setStatus(TrialStatus.PENDING);
		existing.setSports(Sports.valueOf(dto.getSports().toUpperCase()));
		existing.setGender(Gender.valueOf(dto.getGender().toUpperCase()));

		return existing;
	}
}
