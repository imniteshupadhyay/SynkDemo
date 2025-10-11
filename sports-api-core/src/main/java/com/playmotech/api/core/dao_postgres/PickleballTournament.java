package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.TournamentType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity for Pickleball Tournaments
 **/

@Data
@Entity
@Table(name = "pickleball_tournaments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PickleballTournament {
    @Id
    private String id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "inactive")
    private boolean inactive;
    
    @Column(name = "created_on")
    private Timestamp createdOn;
    
    @Column(name = "start_date")
    private String startDate;
    
    @Column(name = "end_date")
    private String endDate;
    
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TournamentType type;
    
    @Column(name = "format")
    @Enumerated(EnumType.STRING)
    private GameFormat format;
    
    @ManyToOne
    @JoinColumn(name = "scorer_user_id", referencedColumnName = "id")
    private UserProfile scorerUserProfile;
    
    @ManyToOne
    @JoinColumn(name = "referee_user_id", referencedColumnName = "id")
    private UserProfile refereeUserProfile;
    
    @ManyToOne
    @JoinColumn(name = "match_official_user_id", referencedColumnName = "id")
    private UserProfile matchOfficialUserProfile;
    
    @ManyToOne
    @JoinColumn(name = "academy_id", referencedColumnName = "id")
    private Academy academy;
    
    @OneToMany(mappedBy = "pickleballTournament", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PickleballTeam> teams;
    
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PickleballMatch> matches;
    
    @ManyToOne
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
    private UserProfile createdByUserProfile;
    
    @OneToMany(mappedBy = "pickleballTournament", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PickleballTournamentGalleryMedia> galleryMedia;
}
