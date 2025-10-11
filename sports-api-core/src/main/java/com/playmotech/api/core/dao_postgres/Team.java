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

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "teams")
@Data
public class Team {
	@Id
	private String id;
	@Column(name = "team_name")
	private String teamName;

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<TeamPlayerMapping> players;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "inactive")
	private Boolean inactive;

	@ManyToOne
	@JoinColumn(name = "badminton_tournament_id", referencedColumnName = "id")
	private BadmintonTournament badmintonTournament;

	@ManyToOne
	@JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
	private UserProfile createdByUserProfile;
}
