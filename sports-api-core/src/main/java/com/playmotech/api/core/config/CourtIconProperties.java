package com.playmotech.api.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "court.icons")
public class CourtIconProperties {
    private String defaultIcon = "https://sta-static.playmotech.com/defaults/courts/Courts.png";
    private Map<String, String> sports = new HashMap<>();
}
