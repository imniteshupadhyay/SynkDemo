package com.playmotech.api.core.dao_postgres;

import com.playmotech.api.core.constants.Sports;

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

/**
 * Created By: deep.patel
 **/

@Entity
@Table(name = "user_preferred_sports_mappings")
@Data
public class UserPreferredSportsMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile userProfile;

	@Enumerated(value = EnumType.STRING)
	@Column
	private Sports sport;
}
