package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.EnquiryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "enquiries")
@Data
public class Enquiry {
	@Id
	private String id;

	@Column(name = "description")
	private String description;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile userProfile;

	@ManyToOne
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private EnquiryStatus status;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

}
