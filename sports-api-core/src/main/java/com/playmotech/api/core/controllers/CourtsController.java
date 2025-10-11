package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ICourtService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/courts")
@AllArgsConstructor
public class CourtsController extends BaseController {

    private final ICourtService courtService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<CourtDto>> addCourse(
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = true) Sports sport,
            @Valid @RequestBody CourtDto courtDto) {
        try {
            courtDto.setSports(sport);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Response.<CourtDto>builder().status(HttpStatus.CREATED.value()).message("success")
                            .body(courtService.addCourt(courtDto)).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<List<CourtDto>>> getCourts(
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
            @RequestParam(value = "t", required = false) String searchTxt) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CourtDto>>builder()
                    .status(HttpStatus.OK.value()).message("success").body(courtService.getCourts(searchTxt, sport))
                    .build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<CourtDto>>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @GetMapping(value = "/{courtId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<CourtDto>> getCourt(@PathVariable("courtId") String courtId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(Response.<CourtDto>builder().status(HttpStatus.OK.value())
                    .message("success").body(courtService.getCourt(courtId)).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @DeleteMapping(value = "/{courtId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<CourtDto>> deleteCourt(@PathVariable("courtId") String courtId) {
        try {
            courtService.deleteCourt(courtId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Response.<CourtDto>builder().status(HttpStatus.OK.value()).message("success").build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }
}
