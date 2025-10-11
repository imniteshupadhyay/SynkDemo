package com.playmotech.api.core.events.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.events.CoachActivityEvent;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ICoachStreakService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CoachActivityEventListener {
    @Autowired
    private ICoachStreakService coachStreakService;

    /**
     * Process coach activity events asynchronously
     * Updates the coach's activity streak when they perform any qualifying action
     */
    @EventListener
    @Async
    public void handleCoachActivity(CoachActivityEvent event) {
        try {
            String coachId = event.getCoachId();
            String academyId = event.getAcademyId();

            log.info("Received activity event for coach: {} in academy: {}", coachId, academyId);
            coachStreakService.recordActivity(coachId, academyId);
        } catch (ResourceException e) {
            // Log error but don't disrupt main flow
            log.error("Unable to record activity: {}", e.getMessage());
        } catch (Exception e) {
            // Log error but don't disrupt main flow
            log.error("Error processing coach activity event", e);
        }
    }
}
