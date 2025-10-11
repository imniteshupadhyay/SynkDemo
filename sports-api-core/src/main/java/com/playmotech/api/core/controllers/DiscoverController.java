package com.playmotech.api.core.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.DiscoverCategory;
import com.playmotech.api.core.dto.DiscoverDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IDiscoverService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("discover")
@RequiredArgsConstructor
public class DiscoverController {
        private final IDiscoverService discoverService;

        @GetMapping("categories")
        public ResponseEntity<Response<Map<String, String>>> getAllCategories() {
                Map<String, String> categories = discoverService.getAllCategories();
                return ResponseEntity.ok(Response.<Map<String, String>>builder()
                                .status(HttpStatus.OK.value())
                                // .message("Nearby items retrieved successfully")
                                .body(categories)
                                .build());
        }

        @GetMapping("nearby")
        public ResponseEntity<Response<List<DiscoverDto>>> discoverNearby(
                        @RequestParam double latitude,
                        @RequestParam double longitude,
                        @RequestParam(defaultValue = "10") int radiusKm,
                        @RequestParam(required = false) DiscoverCategory type) {
                List<DiscoverDto> response = discoverService.findNearby(latitude, longitude, radiusKm, type);

                return ResponseEntity.ok(Response.<List<DiscoverDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Nearby items retrieved successfully")
                                .body(response)
                                .build());
        }

        @GetMapping("filter")
        public ResponseEntity<Response<List<DiscoverDto>>> advancedFilter(
                        @RequestParam(required = false) DiscoverCategory type,
                        @RequestParam(required = false) Double minRating,
                        @RequestParam(required = false) Boolean active,
                        @RequestParam(required = false) Boolean featured,
                        @RequestParam(required = false) List<String> tags) {
                List<DiscoverDto> response = discoverService.findWithAdvancedFilters(
                                type, minRating, active, featured, tags);

                return ResponseEntity.ok(Response.<List<DiscoverDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Filtered items retrieved successfully")
                                .body(response)
                                .build());
        }

        @GetMapping
        public ResponseEntity<Response<List<DiscoverDto>>> discover(
                        @RequestParam(required = false) DiscoverCategory type,
                        @RequestParam(required = false) String location,
                        @RequestParam(value = "t", required = false) String searchTxt,
                        @RequestParam(defaultValue = "false") boolean affiliatedOnly) {
                List<DiscoverDto> response = discoverService.findAll(type, location, searchTxt, affiliatedOnly);
                return ResponseEntity.ok(Response.<List<DiscoverDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("List retrieved successfully")
                                .body(response)
                                .build());
        }

        @GetMapping("{id}")
        public ResponseEntity<Response<DiscoverDto>> discoverById(@PathVariable String id) {
                try {
                        DiscoverDto dto = discoverService.getById(id);

                        return ResponseEntity.ok(Response.<DiscoverDto>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("List retrieved successfully")
                                        .body(dto)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<DiscoverDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
        }
}
