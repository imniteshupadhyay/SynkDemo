package com.playmotech.api.core.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.DaywiseActivity;
import com.playmotech.api.core.dao_postgres.DaywiseActivityMapping;
import com.playmotech.api.core.dao_postgres.NewSchedule;
import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.response.dao.DaywiseActivityDao;
import com.playmotech.api.core.response.dao.DaywiseActivityMappingDao;

public class DaywiseActivityMapper {

    public static DaywiseActivityDao mapEntityToDao(DaywiseActivity entity) {
        DaywiseActivityDao dao = new DaywiseActivityDao();
        
        // Copy simple properties
        BeanUtils.copyProperties(entity, dao);
        
        // Map activity mappings
        
        if (entity.getActivityMappings() != null && !entity.getActivityMappings().isEmpty()) {
            List<DaywiseActivityMappingDao> mappingDaos = entity.getActivityMappings().stream()
                .sorted(Comparator.comparing(mapping -> mapping.getStartTime()))
                .map(DaywiseActivityMappingMapper::mapEntityToDao)
                .collect(Collectors.toList());
            dao.setActivityMappings(mappingDaos);
        }
        
        return dao;
    }
    
    public static DaywiseActivity mapDtoToEntity(DaywiseActivityDto dto, NewSchedule schedule) {
        DaywiseActivity entity = new DaywiseActivity();
        
        // Copy simple properties
        BeanUtils.copyProperties(dto, entity, "id");
        
        // Set schedule reference
        entity.setSchedule(schedule);
        
        // Map activity mappings
        if (dto.getActivityMappings() != null && !dto.getActivityMappings().isEmpty()) {
            List<DaywiseActivityMapping> mappings = dto.getActivityMappings().stream()
                    .map(mappingDto -> DaywiseActivityMappingMapper.mapDtoToEntity(mappingDto, entity))
                    .collect(Collectors.toList());
            entity.setActivityMappings(mappings);
        }
        
        return entity;
    }
    
    public static DaywiseActivity mapDtoToExistingEntity(DaywiseActivityDto dto, DaywiseActivity entity) {
        // Update simple properties
        if (dto.getStartTime() != null) {
            entity.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            entity.setEndTime(dto.getEndTime());
        }
        if (dto.getActivityDate() != null) {
            entity.setActivityDate(dto.getActivityDate());
        }
        
        return entity;
    }
}