package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "pickleball_team_mapping")
@Data
public class PickleballTeamMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", referencedColumnName = "id")
    private PickleballMatch pickleballMatch;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private PickleballTeam team;
}