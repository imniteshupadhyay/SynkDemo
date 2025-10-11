package com.playmotech.api.core.dao_postgres;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation")
@ToString(exclude = { "academies", "configs" })
public class Organisation {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "domain_url")
	private String domainUrl;

	@Column(name = "app_package_name")
	private String appPackageName;

	@OneToMany(mappedBy = "org", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Academy> academies;

	@OneToMany(mappedBy = "org", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OrganisationConfig> configs;
}
