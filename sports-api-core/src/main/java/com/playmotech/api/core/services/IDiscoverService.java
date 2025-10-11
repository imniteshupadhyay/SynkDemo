package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.DiscoverCategory;
import com.playmotech.api.core.dto.DiscoverDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IDiscoverService {
    List<DiscoverDto> findAll(DiscoverCategory type, String location, String searchTxt, boolean affiliatedOnly);

    DiscoverDto getById(String id) throws ResourceException;

    List<DiscoverDto> findNearby(double latitude, double longitude, int radiusKm, DiscoverCategory type);

    List<DiscoverDto> findWithAdvancedFilters(DiscoverCategory type, Double minRating, Boolean active, Boolean featured,
            List<String> tags);

    Map<String, String> getAllCategories();
}
