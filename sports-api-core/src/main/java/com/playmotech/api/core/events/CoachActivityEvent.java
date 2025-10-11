package com.playmotech.api.core.events;

import org.springframework.context.ApplicationEvent;

/** 
 * Event fired when a coach performs any activity that should count toward streaks
 */
public class CoachActivityEvent extends ApplicationEvent {
    private final String coachId;
    private final String academyId;

    public CoachActivityEvent(Object source, String coachId, String academyId) {
        super(source);
        this.coachId = coachId;
        this.academyId = academyId;
    }

    public String getCoachId() {
        return coachId;
    }

    public String getAcademyId() {
        return academyId;
    }
}
