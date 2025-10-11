package com.playmotech.api.core.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentAcademyMapping;
import com.playmotech.api.core.dao_postgres.AssessmentParameter;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig;
import com.playmotech.api.core.dto.AssessmentDto;
import com.playmotech.api.core.dto.AssessmentParameterDto;
import com.playmotech.api.core.response.dao.AssessmentAcademyMappingDao;
import com.playmotech.api.core.response.dao.AssessmentDao;
import com.playmotech.api.core.response.dao.AssessmentParameterConfigDao;
import com.playmotech.api.core.response.dao.AssessmentParameterDao;

public class AssessmentMapper {

	// ===========================
	// Entity -> DAO
	// ===========================
	public static AssessmentDao mapEntityToDao(Assessment entity) {
		AssessmentDao dao = new AssessmentDao();

		// Copy simple fields
		BeanUtils.copyProperties(entity, dao, "academyMappings", "parameters");

		// Map parameters
		if (entity.getParameters() != null) {
			List<AssessmentParameterDao> parameterDaos = entity.getParameters().stream()
					.map(AssessmentMapper::mapParameterEntityToDao).collect(Collectors.toList());
			dao.setParameters(parameterDaos);
		}

		// Map academy mappings
		if (entity.getAcademyMappings() != null) {
			List<AssessmentAcademyMappingDao> mappingDaos = entity.getAcademyMappings().stream()
					.map(AssessmentMapper::mapAcademyMappingEntityToDao).collect(Collectors.toList());
			dao.setAcademyMappings(mappingDaos);
		}

		List<String> academyIds = entity.getAcademyMappings().stream().map(mapping -> mapping.getAcademy().getId())
				.collect(Collectors.toList());
		dao.setAcademyIds(academyIds);

		return dao;
	}

	// ===========================
	// DTO -> Entity
	// ===========================
	public static Assessment mapDtoToEntity(AssessmentDto dto, String userId) {
		Assessment entity = new Assessment();

		// Copy simple fields (ignore relationships)
		BeanUtils.copyProperties(dto, entity, "id", "parameters", "academyIds");

		// Map parameters
		if (dto.getParameters() != null) {
			List<AssessmentParameter> parameters = dto.getParameters().stream()
					.map(p -> mapParameterDtoToEntity(p, entity, userId)).collect(Collectors.toList());
			entity.setParameters(parameters);
		}

		// Map academy IDs only if forAllAcademies is false or not set
		if (dto.getForAllAcademies() == null || !dto.getForAllAcademies()) {
			if (dto.getAcademyIds() != null) {
				List<AssessmentAcademyMapping> mappings = dto.getAcademyIds().stream()
						.map(id -> mapAcademyIdToEntity(id, entity, userId)).collect(Collectors.toList());
				entity.setAcademyMappings(mappings);
			}
		} else {
			// If forAllAcademies is true, ensure no academy mappings are set
			entity.setAcademyMappings(null);
		}

		return entity;
	}

	public static Assessment mapDtoToExistingEntity(AssessmentDto dto, Assessment entity, String userId) {
		// Copy simple fields except id, parameters, and academyIds
		BeanUtils.copyProperties(dto, entity, "id", "parameters", "academyIds");

		// ✅ Replace parameters (mutate list, don't replace reference)
		if (dto.getParameters() != null) {
			if (entity.getParameters() == null) {
				entity.setParameters(new ArrayList<>());
			} else {
				entity.getParameters().clear();
			}

			for (AssessmentParameterDto pDto : dto.getParameters()) {
				AssessmentParameter parameter = mapParameterDtoToEntity(pDto, entity, userId);
				entity.getParameters().add(parameter);
			}
		}

		// ✅ Replace academy mappings (mutate list, don't replace reference)
		if (dto.getForAllAcademies() == null || !dto.getForAllAcademies()) {
			if (dto.getAcademyIds() != null) {
				if (entity.getAcademyMappings() == null) {
					entity.setAcademyMappings(new ArrayList<>());
				} else {
					entity.getAcademyMappings().clear();
				}

				for (String id : dto.getAcademyIds()) {
					AssessmentAcademyMapping mapping = mapAcademyIdToEntity(id, entity, userId);
					entity.getAcademyMappings().add(mapping);
				}
			}
		} else {
			// If forAllAcademies is true, clear the list (don’t replace it)
			if (entity.getAcademyMappings() != null) {
				entity.getAcademyMappings().clear();
			}
		}

		return entity;
	}

	// ===========================
	// Helper Methods
	// ===========================
	private static AssessmentParameterDao mapParameterEntityToDao(AssessmentParameter parameter) {
		AssessmentParameterDao dao = new AssessmentParameterDao();
		// Copy basic properties
		BeanUtils.copyProperties(parameter, dao, "parameterConfig");
		
		// Always set the parameterConfigId from the relationship if available
		if (parameter.getParameterConfig() != null) {
			// Set the ID first
			dao.setParameterConfigId(parameter.getParameterConfig().getParameterId());
			
			// Then map the full config object
			AssessmentParameterConfig config = parameter.getParameterConfig();
			AssessmentParameterConfigDao configDao = new AssessmentParameterConfigDao();
			BeanUtils.copyProperties(config, configDao);
			dao.setParameterConfig(configDao);
		}
		
		return dao;
	}

	private static AssessmentAcademyMappingDao mapAcademyMappingEntityToDao(AssessmentAcademyMapping mapping) {
		AssessmentAcademyMappingDao dao = new AssessmentAcademyMappingDao();
		BeanUtils.copyProperties(mapping, dao);
		dao.setAcademyId(mapping.getAcademy().getId());
		dao.setAcademyName(mapping.getAcademy().getName());
		return dao;
	}

	private static AssessmentParameter mapParameterDtoToEntity(AssessmentParameterDto dto, Assessment parent,
			String userId) {
		AssessmentParameter entity = new AssessmentParameter();
		// Copy all properties except parameterId and parameterConfig
		BeanUtils.copyProperties(dto, entity, "parameterId", "parameterConfig");
		
		// Set the parent assessment
		entity.setAssessment(parent);
		
		// Set default video upload enabled if null
		if (entity.getVideoUploadEnabled() == null) {
			entity.setVideoUploadEnabled(false);
		}
		
		// Set the parameter config if config ID is provided
		if (dto.getParameterConfigId() != null && !dto.getParameterConfigId().isEmpty()) {
			AssessmentParameterConfig config = new AssessmentParameterConfig();
			config.setParameterId(dto.getParameterConfigId());
			entity.setParameterConfig(config);
		}
		
		// Set the category, gender and sport from parent if not already set
		entity.setAgeCategory(parent.getAgeCategory());
		entity.setGender(parent.getGender());
		entity.setSport(parent.getSport());
		
		return entity;
	}

	private static AssessmentAcademyMapping mapAcademyIdToEntity(String academyId, Assessment parent, String userId) {
		AssessmentAcademyMapping mapping = new AssessmentAcademyMapping();
		mapping.setAssessment(parent);
		Academy academy = new Academy();
		academy.setId(academyId);
		mapping.setAcademy(academy);
		return mapping;
	}
}
