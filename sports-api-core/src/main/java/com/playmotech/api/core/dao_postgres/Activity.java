package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "activity")
public class Activity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String description;

	private String category;

	private String subcategory;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organisation_id", referencedColumnName = "id")
	private Organisation organisation;

	@JsonIgnore
	@Column(name = "deleted")
	private Boolean deleted;

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
