package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "coach_academy_mappings")
public class CoachAcademyMapping {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "coach_user_id", referencedColumnName = "id")
	private UserProfile coachUserProfile;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Column(name = "designation")
	private String designation;

	@Column(name = "experience_in_months")
	private Integer experienceInMonths;

	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private Status status;

	@Column(name = "last_status_update_epoch")
	private Timestamp lastStatusUpdateEpoch;

	@Column(name = "role_id")
	private Long roleId;
}
