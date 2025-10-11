package com.playmotech.api.core.mapper;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.BulkUploadHistoryDto;
import com.playmotech.api.core.response.dao.AcademyDao;
import com.playmotech.api.core.response.dao.BulkUploadHistoryDao;
import com.playmotech.api.core.response.dao.CourseDao;
import com.playmotech.api.core.response.dao.UserProfileDao;

public class BulkUploadHistoryMapper {

	public static BulkUploadHistoryDao mapEntityToDao(BulkUploadHistory entity) {
		if (entity == null) {
			return null;
		}

		BulkUploadHistoryDao dao = new BulkUploadHistoryDao();
		BeanUtils.copyProperties(entity, dao);

		// Manual mapping for Academy
		if (entity.getAcademy() != null) {
			AcademyDao academyDao = new AcademyDao();
			BeanUtils.copyProperties(entity.getAcademy(), academyDao);
			dao.setAcademy(academyDao);
		}

		// Manual mapping for Program/Course
		if (entity.getProgram() != null) {
			CourseDao programDao = new CourseDao();
			BeanUtils.copyProperties(entity.getProgram(), programDao);
			dao.setProgram(programDao);
		}

		// Manual mapping for UploadedBy User
		if (entity.getUploadedBy() != null) {
			UserProfileDao userDao = new UserProfileDao();
			BeanUtils.copyProperties(entity.getUploadedBy(), userDao);
			dao.setUploadedBy(userDao);
		}

		return dao;
	}

	public static BulkUploadHistory mapDtoToEntity(BulkUploadHistoryDto dto) {
		if (dto == null) {
			return null;
		}

		BulkUploadHistory entity = new BulkUploadHistory();
		BeanUtils.copyProperties(dto, entity);

		// Manual mapping for Academy
		if (dto.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(dto.getAcademyId());
			entity.setAcademy(academy);
		}

		// Manual mapping for Program/Course
		if (dto.getProgramId() != null) {
			Course program = new Course();
			program.setId(dto.getProgramId());
			entity.setProgram(program);
		}

		// Manual mapping for UploadedBy User
		if (dto.getUploadedById() != null) {
			UserProfile uploadedBy = new UserProfile();
			uploadedBy.setId(dto.getUploadedById());
			entity.setUploadedBy(uploadedBy);
		}

		return entity;
	}
}