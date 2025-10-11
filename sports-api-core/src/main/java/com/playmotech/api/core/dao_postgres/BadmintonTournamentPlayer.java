package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created By: deep.patel
 **/

@Data
@Entity
@Table(name = "badminton_tournament_players")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BadmintonTournamentPlayer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "player_user_id", referencedColumnName = "id")
	private UserProfile playerUserProfile;

	@ManyToOne
	@JoinColumn(name = "badminton_tournament_id", referencedColumnName = "id")
	private BadmintonTournament badmintonTournament;

	@Column(name = "created_on")
	private Timestamp createdOn;

}
