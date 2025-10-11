package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "badminton_live_scores")
@Data
public class BadmintonLiveScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport")
    private Sports sport;

    @Column(name = "round_number")
    private Long roundNumber;

    @Column(name = "player_id")
    private String playerId;

    @Column(name = "guest_player_name")
    private String guestPlayerName;

    @Column(name = "team_id")
    private String teamId;

    private Long score;

    @Column(name = "score_time")
    private String scoreTime;

    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "shot_type")
    private String shotType;

    @Column(name = "commentary")
    private String commentary;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "initial")
    private Boolean initial;
}
