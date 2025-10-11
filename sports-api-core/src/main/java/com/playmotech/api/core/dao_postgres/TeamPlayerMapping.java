package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Table(name = "team_players")
@Entity
@Data
public class TeamPlayerMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "team_id", referencedColumnName = "id")
	private Team team;

	@ManyToOne
	@JoinColumn(name = "player_user_id", referencedColumnName = "id")
	private UserProfile playerUserProfile;
	@Column(name = "guest_player_name")
	private String guestPlayerName;
}
