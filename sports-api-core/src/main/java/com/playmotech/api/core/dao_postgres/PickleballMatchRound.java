package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
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
@Table(name = "pickleball_match_rounds")
public class PickleballMatchRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "match_play_details_id", referencedColumnName = "id")
    private PickleballMatchPlayDetail pickleballMatchPlayDetail;

    @Column(name = "round_start_time")
    private Timestamp roundStartTime;

    @Column(name = "round_end_time")
    private Timestamp roundEndTime;

    @Column(name = "round_number")
    private Integer roundNumber;

    @ManyToOne
    @JoinColumn(name = "winning_user_id", referencedColumnName = "id")
    private UserProfile winningPlayerUserProfile;

    @Column(name = "winning_guest_player_name")
    private String winningGuestPlayerName;

    @ManyToOne
    @JoinColumn(name = "winning_team_id", referencedColumnName = "id")
    private PickleballTeam winningTeam;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "pickleballMatchRound", cascade = CascadeType.ALL)
    private List<PickleballMatchRoundsPlayDetail> pickleballMatchRoundsPlayDetails;
}
