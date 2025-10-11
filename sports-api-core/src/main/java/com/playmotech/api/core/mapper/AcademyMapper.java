package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.response.dao.AcademyDao;

public class AcademyMapper {

	// Method to map a single Academy entity to AcademyDao
	public static AcademyDao mapToDao(Academy academy, String userId) {
		AcademyDao academyDao = new AcademyDao();
		try {
			BeanUtils.copyProperties(academy, academyDao);
			academyDao.setPrograms(ProgramMapper.mapListToDaoList(academy.getAcademyCourseMappings(), userId));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return academyDao;
	}

	// Method to map a list of Academy entities to a list of AcademyDao objects
	public static List<AcademyDao> mapListToDaoList(List<Academy> academies, String userId) {
		return academies.stream().map((academy) -> mapToDao(academy, userId)) // Using the single object mapper here
				.collect(Collectors.toList());
	}
}
