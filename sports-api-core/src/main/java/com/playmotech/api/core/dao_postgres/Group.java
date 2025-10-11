package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created By: deep.patel
 **/

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "groups")
public class Group {
	@Id
	private String id;
	@Column(name = "name")
	private String name;
	@Column(name = "inactive")
	private Boolean inactive;

	@ManyToOne
	@JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
	private UserProfile createdByUserProfile;
	@Column(name = "created_on")
	private Timestamp createdOn;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<GroupAdminMapping> groupAdminMappings;

	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<GroupMemberMapping> groupMemberMappings;
}
