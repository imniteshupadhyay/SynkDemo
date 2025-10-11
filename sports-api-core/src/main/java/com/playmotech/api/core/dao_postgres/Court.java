package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "courts")
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Court {
	@Id
	private String id;
	@Column(name = "court_name")
	private String courtName;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@Column(name = "created_on")
	private Timestamp createOn;

	@Column(name = "is_inactive")
	private Boolean isInActive;

	@Column(name = "icon_url")
	private String courtImage;

	@Column(name = "sport")
	@Enumerated(value = EnumType.STRING)
	private Sports sport;
}
