package com.playmotech.api.core.dao_postgres;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "badminton_match_play_details")
public class BadmintonMatchPlayDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "match_id", referencedColumnName = "id")
	private BadmintonMatch badmintonMatch;

	@ManyToOne
	@JoinColumn(name = "tournament_id", referencedColumnName = "id")
	private BadmintonTournament tournament;

	@ManyToOne
	@JoinColumn(name = "winning_player_id", referencedColumnName = "id")
	private UserProfile winningPlayerUserProfile;

	@Column(name = "winning_guest_player_name")
	private String winningGuestPlayerName;

	@Column(name = "is_tied")
	private Boolean isTied;

	@ManyToOne
	@JoinColumn(name = "winning_team_id", referencedColumnName = "id")
	private Team winningTeam;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "badmintonMatchPlayDetail", cascade = CascadeType.ALL)
	private List<BadmintonMatchRound> badmintonMatchRounds;
}
