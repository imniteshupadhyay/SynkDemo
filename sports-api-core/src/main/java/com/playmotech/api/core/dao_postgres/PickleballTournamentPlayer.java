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
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "pickleball_tournament_players")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PickleballTournamentPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_user_id", referencedColumnName = "id")
    private UserProfile playerUserProfile;

    @ManyToOne
    @JoinColumn(name = "pickleball_tournament_id", referencedColumnName = "id")
    private PickleballTournament pickleballTournament;

    @Column(name = "created_on")
    private Timestamp createdOn;
}
