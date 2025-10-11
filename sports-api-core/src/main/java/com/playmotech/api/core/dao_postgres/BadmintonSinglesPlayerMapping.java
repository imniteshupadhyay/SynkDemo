package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Table(name = "badminton_singles_player_mapping")
@Entity
@Data
public class BadmintonSinglesPlayerMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "match_id", referencedColumnName = "id")
	private BadmintonMatch badmintonMatch;

	@ManyToOne
	@JoinColumn(name = "player_user_id", referencedColumnName = "id")
	private UserProfile playerUserProfile;

	@Column(name = "guest_name")
	private String guestName;
}