package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.Master;
import com.playmotech.api.core.repo.MasterRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.CategoryDao;
import com.playmotech.api.core.response.dao.SubCategoryDao;
import com.playmotech.api.core.services.MasterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MasterServiceImpl implements MasterService {

	private final MasterRepository masterRepository;

	@Override
	public ServiceResponse getCategoryStructure(String categoryType) {
		List<Master> allCategories;
		if (categoryType == null) {
			allCategories = masterRepository.findAll();
		} else {
			allCategories = masterRepository.findByCategory(categoryType);
		}
		try {
			if (allCategories.isEmpty()) {
				log.info(ApiResponse.MASTER_DATA_NOT_FOUND.getMessage());
				return ResponseBuilder.success(ApiResponse.MASTER_DATA_NOT_FOUND, HttpStatus.OK);
			}
			Map<Long, List<Master>> childrenMap = new HashMap<>();
			List<Master> parents = new ArrayList<>();

			// Separate parents and children
			for (Master category : allCategories) {
				if (category.getParent() == null) {
					parents.add(category);
				} else {
					Long parentId = category.getParent().getId();
					childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
				}
			}

			return ResponseBuilder
					.success(parents.stream().map(parent -> mapToCategoryDao(parent, childrenMap.get(parent.getId())))
							.collect(Collectors.toList()), ApiResponse.MASTER_SUCCESS, HttpStatus.OK);
		} catch (Exception e) {
			log.error(ApiResponse.ERROR_MASTER_EXPORTING_DATA.getMessage() + e.getMessage());
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_MASTER_EXPORTING_DATA);
		}
	}

	private CategoryDao mapToCategoryDao(Master parent, List<Master> children) {
		List<SubCategoryDao> subCategories = (children != null) ? children.stream()
				.map(child -> new SubCategoryDao(child.getId(), child.getName())).collect(Collectors.toList())
				: Collections.emptyList();

		return new CategoryDao(parent.getId(), parent.getName(), subCategories);
	}
}
