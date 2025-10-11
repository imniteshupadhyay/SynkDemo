package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created By: deep.patel
 **/

@Data
@Table(name = "group_admin_mappings")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAdminMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "group_id", referencedColumnName = "id")
	private Group group;

	@ManyToOne
	@JoinColumn(name = "group_admin_user_id", referencedColumnName = "id")
	private UserProfile groupAdminUserProfile;
	@Column(name = "created_on")
	private Timestamp createdOn;
}
