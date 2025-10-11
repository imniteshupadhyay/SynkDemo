package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "pickleball_match_team_player_mapping")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PickleballMatchTeamPlayerMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", referencedColumnName = "id")
    private PickleballMatch pickleballMatch;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private PickleballTeam team;

    @ManyToOne
    @JoinColumn(name = "player_user_id", referencedColumnName = "id")
    private UserProfile playerUserProfile;

    @Column(name = "guest_name")
    private String guestName;
}
