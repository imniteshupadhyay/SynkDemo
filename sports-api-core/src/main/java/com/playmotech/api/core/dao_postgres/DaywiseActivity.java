package com.playmotech.api.core.dao_postgres;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "day_wise_activity")
public class DaywiseActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private NewSchedule schedule;
    
    @OneToMany(mappedBy = "daywiseActivity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("daywise-mapping")
    private List<DaywiseActivityMapping> activityMappings = new ArrayList<>();


    private LocalTime startTime;
    
    private LocalTime endTime;
    
    private LocalDate activityDate;
}