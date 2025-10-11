package com.playmotech.api.core.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dto.ActivityDto;
import com.playmotech.api.core.response.dao.ActivityDao;
import com.playmotech.api.core.response.dao.ActivityExportDao;

public class ActivityMapper {

	public static ActivityDao mapEntityToDao(Activity entity) {
		ActivityDao dao = new ActivityDao();
		BeanUtils.copyProperties(entity, dao);

		if (entity.getOrganisation() != null) {
			Organisation organisation = new Organisation();
			organisation.setId(entity.getOrganisation().getId());
			organisation.setDomainUrl(entity.getOrganisation().getDomainUrl());
			dao.setOrganisation(organisation);
		}

		if (entity.getCreatedBy() != null) {
			dao.setCreatedBy(entity.getCreatedBy().getDisplayName());
		}

		dao.setInsertedOn(entity.getInsertedOn());
		return dao;
	}

	public static ActivityExportDao mapEntityToExportDao(Activity entity) {
		ActivityExportDao dao = new ActivityExportDao();
		BeanUtils.copyProperties(entity, dao);

		if (entity.getCreatedBy() != null) {
			dao.setCreatedByName(entity.getCreatedBy().getDisplayName());
		}

		dao.setInsertedOn(entity.getInsertedOn());
		return dao;
	}

	public static Activity mapDtoToEntity(ActivityDto dto) {
		Activity entity = new Activity();
		BeanUtils.copyProperties(dto, entity);

		entity.setDeleted(false);
		return entity;
	}

	public static Activity mapIdToEntity(Long id) {
		Activity entity = new Activity();
		entity.setId(id);

		return entity;
	}

	public static List<Activity> mapIdsToEntities(List<Long> ids) {
		List<Activity> entities = new ArrayList<>();
		for (Long id : ids) {
			entities.add(mapIdToEntity(id));
		}
		return entities;
	}

	public static ActivityDao mapEntityToActivityDao(Activity entity) {
		ActivityDao dao = new ActivityDao();
		BeanUtils.copyProperties(entity, dao, "organisation", "createdBy");
		return dao;
	}

	public static List<ActivityDao> mapEntitiesToActivityDaos(List<Activity> activities) {
		List<ActivityDao> daos = new ArrayList<>();
		for (Activity activity : activities) {
			daos.add(mapEntityToActivityDao(activity));
		}
		return daos;
	}

	public static Activity mapDtoToExistingEntity(ActivityDto dto, Activity existing) {
		BeanUtils.copyProperties(dto, existing);

		existing.setDeleted(false);
		return existing;
	}
}
