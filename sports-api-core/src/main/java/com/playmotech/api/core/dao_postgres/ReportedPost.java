package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.ReportStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "reported_posts")
public class ReportedPost {

	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "reported_by", referencedColumnName = "id")
	private UserProfile reportedByUserProfile;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private ReportStatus status;

	@Column(name = "last_status_update_epoch")
	private Timestamp lastStatusUpdateTimestamp;

	@Column(name = "post_id")
	private String postId;
}
