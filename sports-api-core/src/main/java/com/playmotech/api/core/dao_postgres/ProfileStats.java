package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Entity
@Table(name = "profile_stats")
@Data
public class ProfileStats {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile userProfile;

	@Column(name = "following")
	private long following;
	@Column(name = "followers")
	private long followers;
	@Column(name = "profile_views")
	private long profileViews;
}
