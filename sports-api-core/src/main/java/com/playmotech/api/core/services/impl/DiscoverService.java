package com.playmotech.api.core.services.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.constants.DiscoverCategory;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.Discover;
import com.playmotech.api.core.dto.DiscoverDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.DiscoverRepo;
import com.playmotech.api.core.services.IDiscoverService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscoverService implements IDiscoverService {
	private final DiscoverRepo discoverRepo;

	@Override
	public List<DiscoverDto> findNearby(double latitude, double longitude, int radiusKm, DiscoverCategory type) {
		List<Discover> nearbyItems = discoverRepo.findByProximity(latitude, longitude, radiusKm,
				type != null ? type.name() : null);

		return nearbyItems.stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public List<DiscoverDto> findWithAdvancedFilters(DiscoverCategory type, Double minRating, Boolean active,
			Boolean featured, List<String> tags) {
		List<Discover> filteredItems;

		if (tags != null && !tags.isEmpty()) {
			// If tags are provided, filter by tags first
			List<Discover> taggedItems = discoverRepo.findByTagsIn(tags);

			// Then apply other filters in-memory
			filteredItems = taggedItems.stream().filter(d -> type == null || d.getType() == type)
					.filter(d -> minRating == null || d.getRating() >= minRating)
					.filter(d -> active == null || d.isActive() == active)
					.filter(d -> featured == null || d.isFeatured() == featured).collect(Collectors.toList());
		} else {
			// Otherwise use the query with filters
			filteredItems = discoverRepo.findWithFilters(type, minRating, active, featured);
		}

		return filteredItems.stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public List<DiscoverDto> findAll(DiscoverCategory type, String location, String searchTxt, boolean affiliatedOnly) {
		List<Discover> entities = discoverRepo.findAll();

		if (CollectionUtils.isEmpty(entities)) {
			return List.of();
		}

		// Normalize search text
		String search = searchTxt != null ? searchTxt.trim().toLowerCase(Locale.ROOT) : null;
		String locationFilter = location != null ? location.trim().toLowerCase(Locale.ROOT) : null;

		return entities.stream().filter(e -> type == null || type.name().equalsIgnoreCase("ALL") || e.getType() == type)
				.filter(e -> locationFilter == null || (e.getLocation() != null
						&& e.getLocation().toLowerCase(Locale.ROOT).contains(locationFilter)))
				.filter(e -> search == null || matchesSearch(e, search)).map(this::toDto).collect(Collectors.toList());
	}

	private boolean matchesSearch(Discover e, String search) {
		return (e.getName() != null && e.getName().toLowerCase(Locale.ROOT).contains(search))
				|| (e.getSpecialty() != null && e.getSpecialty().toLowerCase(Locale.ROOT).contains(search))
				|| (e.getTags() != null && e.getTags().stream()
						.anyMatch(tag -> tag != null && tag.toLowerCase(Locale.ROOT).contains(search)))
				|| (e.getServicesOffered() != null && e.getServicesOffered().stream()
						.anyMatch(svc -> svc != null && svc.toLowerCase(Locale.ROOT).contains(search)));
	}

	public List<DiscoverDto> findAllOld(DiscoverCategory type, String location, String searchTxt,
			boolean affiliatedOnly) {
		List<Discover> items = discoverRepo.findAll();

		if (CollectionUtils.isEmpty(items)) {
			return List.of();
		}

		if (affiliatedOnly) {
			return items.stream()
					.filter(i -> type == null || type.name().equalsIgnoreCase("ALL") || i.getType() == type)
					.filter(i -> location == null || i.getLocation().equalsIgnoreCase(location))
					.filter(i -> searchTxt == null
							|| i.getName().toLowerCase().contains(searchTxt.trim().toLowerCase()))
					.filter(i -> !affiliatedOnly || i.isAffiliated()).map(this::toDto).collect(Collectors.toList());
		}

		return items.stream().filter(i -> type == null || type.name().equalsIgnoreCase("ALL") || i.getType() == type)
				.filter(i -> location == null || i.getLocation().equalsIgnoreCase(location))
				.filter(i -> searchTxt == null || i.getName().toLowerCase().contains(searchTxt.trim().toLowerCase()))
				.map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public DiscoverDto getById(String id) throws ResourceException {
		Discover discover = discoverRepo.findById(id)
				.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Requested object not found"));

		return toDto(discover);
	}

	private DiscoverDto toDto(Discover entity) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = null;
		if (entity.getOpeningHoursJson() != null) {
			try {
				map = mapper.readValue(entity.getOpeningHoursJson(), new TypeReference<Map<String, String>>() {
				});
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				map = null;
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				map = null;
			}
		} else {
			map = null;
		}
		return DiscoverDto.builder().id(entity.getId()).name(entity.getName()).type(entity.getType())
				.specialty(entity.getSpecialty()).location(entity.getLocation()).latitude(entity.getLatitude())
				.longitude(entity.getLongitude()).rating(entity.getRating()).reviewCount(entity.getReviewCount())
				.imageGallery(entity.getImageGallery()).affiliated(entity.isAffiliated()).featured(entity.isFeatured())
				.phone(entity.getPhone()).website(entity.getWebsite()).directionsLink(entity.getDirectionsLink())
				.openingHoursJson(map).tags(entity.getTags()).servicesOffered(entity.getServicesOffered()).build();
	}

	@Override
	public Map<String, String> getAllCategories() {
		Map<String, String> categoriesMap = Arrays.stream(DiscoverCategory.values())
				.filter(c -> c != DiscoverCategory.ALL)
				.collect(Collectors.toMap(
						Enum::name,
						c -> c.name().replace("_", " ").toUpperCase()));
		return categoriesMap;
	}
}
