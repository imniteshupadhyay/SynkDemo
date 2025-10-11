package com.playmotech.api.core.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.BadmintonEventType;
import com.playmotech.api.core.dto.BadmintonMatchContext;
import com.playmotech.api.core.services.CommentaryService;

@RestController
@RequestMapping("/api/commentary")
public class CommentaryController {

    private final CommentaryService commentaryService;

    public CommentaryController(CommentaryService commentaryService) {
        this.commentaryService = commentaryService;
    }

    /**
     * Generates commentary for a badminton event using the default template.
     *
     * @param eventType The type of event
     * @param matchContext The match context containing variables to populate the template
     * @return The generated commentary
     */
    @PostMapping("/generate/badminton")
    public ResponseEntity<String> generateBadmintonCommentary(
            @RequestParam BadmintonEventType eventType,
            @RequestBody BadmintonMatchContext matchContext) {
        String commentary = commentaryService.generateCommentary(eventType, matchContext);
        return ResponseEntity.ok(commentary);
    }

    /**
     * Generates commentary using a custom template.
     *
     * @param template The template string with placeholders
     * @param variables Map of variable names to their values
     * @return The generated commentary
     */
    @PostMapping("/generate/custom")
    public ResponseEntity<String> generateCustomCommentary(
            @RequestParam String template,
            @RequestBody Map<String, String> variables) {
        String commentary = commentaryService.generateCommentary(template, variables);
        return ResponseEntity.ok(commentary);
    }
}
