package com.playmotech.api.core.mapper;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.DaywiseActivity;
import com.playmotech.api.core.dao_postgres.DaywiseActivityMapping;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.response.dao.DaywiseActivityMappingDao;

public class DaywiseActivityMappingMapper {

    public static DaywiseActivityMappingDao mapEntityToDao(DaywiseActivityMapping entity) {
        DaywiseActivityMappingDao dao = new DaywiseActivityMappingDao();
        
        // Copy simple properties
        BeanUtils.copyProperties(entity, dao);
        
        // Map activity
        if (entity.getActivities() != null) {
           
            dao.setActivities(ActivityMapper.mapEntitiesToActivityDaos(entity.getActivities()));
        }
        
        return dao;
    }
    
    public static DaywiseActivityMapping mapDtoToEntity(DaywiseActivityMappingDto dto, DaywiseActivity daywiseActivity) {
        DaywiseActivityMapping entity = new DaywiseActivityMapping();
        
        // Copy simple properties
        BeanUtils.copyProperties(dto, entity, "id");
        
        // Set daywise activity reference
        entity.setDaywiseActivity(daywiseActivity);
        
        if (dto.getActivityIds() != null) {
        	entity.setActivities(ActivityMapper.mapIdsToEntities(dto.getActivityIds()));
		}
        
        return entity;
    }
    
    public static DaywiseActivityMapping mapDtoToExistingEntity(DaywiseActivityMappingDto dto, DaywiseActivityMapping entity) {
        // Update simple properties
        if (dto.getStartTime() != null) {
            entity.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            entity.setEndTime(dto.getEndTime());
        }
        
        // Update activity reference
        if (dto.getActivityIds() != null) {
        	entity.setActivities(ActivityMapper.mapIdsToEntities(dto.getActivityIds()));
		}
        
        return entity;
    }
}