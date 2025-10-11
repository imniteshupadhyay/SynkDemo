package com.playmotech.api.core.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.dto.TraineeViewDto;
import com.playmotech.api.core.views.TraineeView;

@Component
public class TraineeViewMapper {
	private final ModelMapper modelMapper = new ModelMapper();

	public TraineeViewDto toDto(TraineeView trainee) {
		TraineeViewDto dto = modelMapper.map(trainee, TraineeViewDto.class);
		System.out.println("dto: " + dto.getId());
		return dto;
	}
}
