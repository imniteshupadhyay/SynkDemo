package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "academy_sport_mappings")
@ToString(exclude = "academy")
public class AcademySportMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	@JsonManagedReference
	private Academy academy;
	@Column(name = "sport")
	@Enumerated(value = EnumType.STRING)
	private Sports sport;
	@Column(name = "created_at_timestamp_utc")
	private Timestamp createdAtTimestampUtc;
}
