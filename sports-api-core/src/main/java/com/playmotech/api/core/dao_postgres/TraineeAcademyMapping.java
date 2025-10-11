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

@Entity
@Data
@Table(name = "trainee_academy_mappings")
public class TraineeAcademyMapping {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "trainee_user_id", referencedColumnName = "id")
	private UserProfile traineeUserProfile;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "updated_on")
	private Timestamp updatedOn;
	@Column(name = "assigned_coach_user_id")
	private String assignedCoachUserId;
	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private Status status;
	@Column(name = "last_status_update_time")
	private Timestamp lastStatusUpdateTime;
}
