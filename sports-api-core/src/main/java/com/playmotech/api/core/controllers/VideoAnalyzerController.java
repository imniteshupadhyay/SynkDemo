package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.VideoAnalyzerDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.VideoAnalyzerService;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.utils.ThumbnailGenerator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("video-analyzer")
@Slf4j
public class VideoAnalyzerController {

    private final VideoAnalyzerService videoAnalyzerService;

    @GetMapping("/list")
    public ResponseEntity<ServiceResponse> getVideoAnalyzersList(
            // @RequestHeader(name = "UserId", required = true) String userId,
            HttpServletRequest request,
            @RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
            @RequestParam(name = "analysed", defaultValue = "false", required = false) boolean analysed,
            @RequestParam(name = "orderBy", defaultValue = "insertedOn", required = false) String orderBy,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestParam(name = "search", defaultValue = "", required = false) String search,
            @RequestParam(name = "analysisStatus", required = false) String analysisStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
            @RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
            @RequestParam(name = "ascending", defaultValue = "false", required = false) boolean ascending,
            @RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
            @RequestParam(name = "playerIds", required = false) List<String> playerIds,
            // @RequestParam(name = "coachId", required = false) String coachId,
            @RequestParam(name = "courseIds", required = false) List<String> courseIds,
            @RequestParam(name = "academyIds", required = false) List<String> academyIds) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        // Validate pagination parameters
        if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
            return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
        }

        // Validate orderBy parameter
        String[] allowedOrderByValues = { "title", "mediaUrl", "insertedOn" };
        if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
            return ResponseBuilder.badRequestEntity(
                    ApiResponse.INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
        }

        // Timestamp startDateLocal = null;
        // Timestamp endDateLocal = null;
        // if(startDate != null && endDate != null) {
        // try {
        // startDateLocal = parseTimestamp(startDate);
        // endDateLocal = parseTimestamp(endDate);
        // if (startDate != null && endDate != null &&
        // startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
        // return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_LISTING_FILTERS);
        GenericFilter filter = GenericFilter.builder().userId(currentUser.getUserId()).isPageable(pageable)
                .currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
                .academyId(academyId).playerIds(playerIds).notDeleted(active).userId(currentUser.getUserId())
                // .coachId(coachId)
                .programIds(courseIds).academyIds(academyIds)
                .analyzer(VideoAnalyzer.builder().isAnalysed(analysed).build())
                .analysisStatus(analysisStatus).build();

        ServiceResponse response = videoAnalyzerService.getVideoAnalyzersList(filter, null);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @GetMapping
    public ResponseEntity<ServiceResponse> getVideoAnalyzer(@RequestParam(name = "id", required = true) String id) {

        ServiceResponse response = videoAnalyzerService.getVideoAnalyzer(id);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> addVideoAnalyzer(@RequestBody VideoAnalyzerDto analyzerDto)
    // @RequestHeader(name = "UserId", required = true) String userId)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        analyzerDto.setCoachId(currentUser.getUserId());
        ServiceResponse response = videoAnalyzerService.addVideoAnalyzer(analyzerDto);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PutMapping
    public ResponseEntity<ServiceResponse> updateVideoAnalyzer(@RequestBody VideoAnalyzerDto analyzerDto
    // @RequestHeader(name = "UserId", required = true) String userId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        analyzerDto.setCoachId(currentUser.getUserId());
        ServiceResponse response = videoAnalyzerService.updateVideoAnalyzer(analyzerDto, currentUser.getUserId());
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @DeleteMapping
    public ResponseEntity<ServiceResponse> deleteVideoAnalyzer(@RequestParam(name = "id", required = false) String id
    // @RequestHeader(name = "UserId", required = true) String userId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        ServiceResponse response = videoAnalyzerService.deleteVideoAnalyzer(id, currentUser.getUserId());
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping("/share")
    public ResponseEntity<ServiceResponse> shareVideoAnalyzer(@RequestParam(name = "id", required = true) String id,
            @RequestParam(name = "courseId", required = true) String courseId,
            @RequestParam(name = "playerId", required = true) String playerId
    // @RequestHeader(name = "UserId", required = true) String userId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        ServiceResponse response = videoAnalyzerService.shareVideoAnalyzer(id, courseId, playerId,
                currentUser.getUserId());
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping("document")
    ResponseEntity<ServiceResponse> uploadDocument(@RequestParam MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        FileObjectDto fileObjectDto = new FileObjectDto();
        try {
            fileObjectDto.setOriginalFilename(file.getOriginalFilename());
            fileObjectDto.setContent(file.getBytes());
            fileObjectDto.setContentType(file.getContentType());
        } catch (IOException e) {
            log.error("Unable to set the content of the file: {}", e.getMessage());
        }

        FileObjectDto thumbnailFileObjectDto = null;
        try {
            thumbnailFileObjectDto = ThumbnailGenerator.generateThumbnail(file);
        } catch (ResourceException e) {
            log.error("Unable to generate thumbnail from the video file: {}", e.getMessage());
            log.warn("Setting the thumbnailFileObjectDto as null");
            thumbnailFileObjectDto = null;
        }

        ServiceResponse response = videoAnalyzerService.fileUpload(currentUser.getUserId(), fileObjectDto,
                thumbnailFileObjectDto);

        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @GetMapping("generate-url")
    ResponseEntity<ServiceResponse> getDirectUploadUrl(@RequestParam(required = true) String fileName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        ServiceResponse response = videoAnalyzerService.generateDirectUploadUrl(currentUser.getUserId(), fileName);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @PostMapping("generate-url/confirm")
    ResponseEntity<ServiceResponse> confirmUpload(@RequestParam(required = true) String id,
            @RequestParam(required = true) String fileKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            ServiceResponse response = videoAnalyzerService.confirmDirectUpload(fileKey, id);

            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (ResourceException e) {
            ServiceResponse response = new ServiceResponse();
            response.setMessage(e.getMessage());
            response.setStatus(e.getErrorCodes().getHttpStatusCode());
            response.setHttpStatus(HttpStatus.valueOf(e.getErrorCodes().getHttpStatusCode()));

            return ResponseEntity.status(response.getStatus()).body(response);
        }
    }
}