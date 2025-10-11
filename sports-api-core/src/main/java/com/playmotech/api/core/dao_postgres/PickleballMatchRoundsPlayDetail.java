package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pickleball_match_round_play_details")
public class PickleballMatchRoundsPlayDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "match_round_id", referencedColumnName = "id")
    private PickleballMatchRound pickleballMatchRound;

    @ManyToOne
    @JoinColumn(name = "player_user_id", referencedColumnName = "id")
    private UserProfile playerUserProfile;

    @Column(name = "guest_player_name")
    private String guestPlayerName;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private PickleballTeam team;

    private Long score;
}
