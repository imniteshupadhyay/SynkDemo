package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.response.dao.TraineeViewDao;
import com.playmotech.api.core.views.TraineeView;

public class PlayerMapper {

	// Method to map a single TraineeView entity to TraineeViewDao
	public static TraineeViewDao mapTraineeViewToDao(TraineeView entity) {
	    TraineeViewDao dao = new TraineeViewDao();
	    try {
	        BeanUtils.copyProperties(entity, dao);

	        // Set playerName: show blank if null or empty
	        String displayName = entity.getDisplayName();
	        dao.setPlayerName(displayName != null && !displayName.trim().isEmpty() ? displayName : "");

	        // Set programNames from courseNames list: filter out null/empty values and show blank if resulting list is empty
	        List<String> courseNames = entity.getCourseNames();
	        if (courseNames != null && !courseNames.isEmpty()) {
	            // Filter out null and empty/blank strings
	            List<String> filteredNames = courseNames.stream()
	                .filter(name -> name != null && !name.trim().isEmpty())
	                .collect(Collectors.toList());
	            
	            dao.setProgramNames(filteredNames.isEmpty() ? "" : String.join(", ", filteredNames));
	        } else {
	            dao.setProgramNames("");
	        }

	        // You can do similar checks for other custom mappings if needed

	    } catch (Exception e) {
	        e.printStackTrace(); // Consider logging this instead in production
	    }
	    return dao;
	}

}
