package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.DiscoverCategory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoverDto {
    private String id;
    private String name;
    private DiscoverCategory type;
    private String specialty;
    private String location;
    private Double latitude;
    private Double longitude;
    private double rating;
    private Integer reviewCount;
    private List<String> imageGallery;
    private boolean affiliated;
    private boolean featured;
    private String phone;
    private String website;
    private String directionsLink;
    private Map<String, String> openingHoursJson;
    private List<String> tags;
    private List<String> servicesOffered;
    private String reviewSnippet;
    private String affiliationPerks;
}
