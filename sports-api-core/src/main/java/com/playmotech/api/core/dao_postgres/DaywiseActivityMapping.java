package com.playmotech.api.core.dao_postgres;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "day_wise_activity_mapping")
public class DaywiseActivityMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "daywise_activity_id")
    @JsonBackReference("daywise-mapping")
    private DaywiseActivity daywiseActivity;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "daywise_activity_mapping_activities",
        joinColumns = @JoinColumn(name = "daywise_activity_mapping_id"),
        inverseJoinColumns = @JoinColumn(name = "activity_id")
    )
    private List<Activity> activities = new ArrayList<>();

    private LocalTime startTime;
    private LocalTime endTime;
}