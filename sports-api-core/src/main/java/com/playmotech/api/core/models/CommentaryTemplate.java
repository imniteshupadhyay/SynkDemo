package com.playmotech.api.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a template for generating commentary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentaryTemplate {
    private String id;
    private String name;
    private String template;
    private String sportType;  // e.g., "BADMINTON", "TENNIS", etc.
    private String eventType;  // Reference to event types like those in BadmintonEventType
}
