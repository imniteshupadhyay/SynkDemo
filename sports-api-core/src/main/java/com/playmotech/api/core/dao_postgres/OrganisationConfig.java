package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organisation_config")
public class OrganisationConfig {

	@Id
	private Long id;

	@Column(name = "key")
	private String key;

	@Column(name = "value")
	private String value;

	@ManyToOne
	@JoinColumn(name = "org_id", referencedColumnName = "id")
	private Organisation org;
}
