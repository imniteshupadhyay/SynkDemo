package com.playmotech.api.core.dao_postgres;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.auto.value.AutoValue.Builder;
import com.playmotech.api.core.constants.ScheduleType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "new_schedule")
public class NewSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "type")
	@Enumerated(value = EnumType.STRING)
	private ScheduleType type;
	
	private String dayJson;
	
    private String name;
    
    private String description;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private LocalTime startTime;
    
    private LocalTime endTime;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("schedule-daywise")
    private List<DaywiseActivity> daywiseActivities = new ArrayList<>();
    
	@JsonIgnore
	@Column(name = "deleted")
	private Boolean deleted;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organisation_id", referencedColumnName = "id")
	private Organisation organisation;
	
	@JsonIgnore
	@CreationTimestamp
	@Column(name = "inserted_on", updatable = false)
	private LocalDateTime insertedOn;

	@JsonIgnore
	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private LocalDateTime updatedOn;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_id", referencedColumnName = "id")
	private UserProfile createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_id", referencedColumnName = "id")
	private UserProfile updatedBy;
}
