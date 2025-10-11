package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lead_source")
public class LeadSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private String id;

	/*
	 * Sources will be the following: Web site, Social Media. Walk In, Referral,
	 * Others
	 */
	@Column(name = "name", columnDefinition = "varchar(255)")
	private String name;
}
