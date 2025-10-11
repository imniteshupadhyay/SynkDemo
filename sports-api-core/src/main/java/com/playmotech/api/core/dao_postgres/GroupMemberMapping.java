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

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "group_member_mappings")
public class GroupMemberMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "group_id", referencedColumnName = "id")
	private Group group;

	@ManyToOne
	@JoinColumn(name = "group_member_user_id", referencedColumnName = "id")
	private UserProfile groupMemberUserProfile;
	@Column(name = "created_on")
	private Timestamp createdOn;
}
