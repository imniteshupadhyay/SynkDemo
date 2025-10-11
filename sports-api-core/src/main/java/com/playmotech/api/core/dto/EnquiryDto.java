package com.playmotech.api.core.dto;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.EnquiryStatus;
import com.playmotech.api.core.dao_postgres.Enquiry;

import lombok.Data;

@Data
public class EnquiryDto {
	private String id;
	private String description;
	private EnquiryStatus status;
	private Timestamp createdOn;
	private Timestamp updatedOn;
	private String notes;
	private UserProfileMinDto userProfile;
	private CourseMinDto course;

	public EnquiryDto(Enquiry enquiry, UserProfileMinDto userProfile, CourseMinDto course) {
		this.id = enquiry.getId();
		this.description = enquiry.getDescription();
		this.status = enquiry.getStatus();
		this.createdOn = enquiry.getCreatedOn();
		this.updatedOn = enquiry.getUpdatedOn();
		this.notes = enquiry.getNotes();
		this.userProfile = userProfile;
		this.setCourse(course);
	}
}
