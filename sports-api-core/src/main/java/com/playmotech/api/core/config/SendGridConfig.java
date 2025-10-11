package com.playmotech.api.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.sendgrid.SendGrid;

@Configuration
@EnableRetry
public class SendGridConfig {

    @Value("${sendgrid.api.key}")
    private String apiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name}")
    private String fromName;

    @Value("${sendgrid.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${sendgrid.retry.delay}")
    private long retryDelay;

    @Bean
    SendGrid sendGrid() {
        return new SendGrid(apiKey);
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryDelay() {
        return retryDelay;
    }
}
