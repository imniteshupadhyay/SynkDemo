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
@Table(name = "pickleball_match_play_details")
public class PickleballMatchPlayDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", referencedColumnName = "id")
    private PickleballMatch pickleballMatch;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id")
    private PickleballTournament tournament;

    @ManyToOne
    @JoinColumn(name = "winning_player_id", referencedColumnName = "id")
    private UserProfile winningPlayerUserProfile;

    @Column(name = "winning_guest_player_name")
    private String winningGuestPlayerName;

    @Column(name = "is_tied")
    private Boolean isTied;

    @ManyToOne
    @JoinColumn(name = "winning_team_id", referencedColumnName = "id")
    private PickleballTeam winningTeam;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "pickleballMatchPlayDetail", cascade = CascadeType.ALL)
    private List<PickleballMatchRound> pickleballMatchRounds;
}
