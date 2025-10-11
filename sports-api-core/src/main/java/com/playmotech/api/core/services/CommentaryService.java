package com.playmotech.api.core.services;

import java.util.Map;

import com.playmotech.api.core.constants.BadmintonEventType;
import com.playmotech.api.core.dto.BadmintonMatchContext;

public interface CommentaryService {
    
    /**
     * Generates commentary for a badminton event using the default template.
     * 
     * @param eventType The type of event
     * @param matchContext The match context containing variables to populate the template
     * @return The generated commentary
     */
    String generateCommentary(BadmintonEventType eventType, BadmintonMatchContext matchContext);
    
    /**
     * Generates commentary using a custom template.
     * 
     * @param template The template string with placeholders
     * @param variables Map of variable names to their values
     * @return The generated commentary
     */
    String generateCommentary(String template, Map<String, String> variables);
}
