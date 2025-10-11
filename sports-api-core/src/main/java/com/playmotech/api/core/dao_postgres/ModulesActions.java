package com.playmotech.api.core.dao_postgres;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "module_actions")
public class ModulesActions {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "action_code")
	private String actionCode;

	@Column(name = "action_name")
	private String actionName;

	@Column(name = "disabled", nullable = false)
	private Boolean disabled = true;

	@ManyToOne
	@JoinColumn(name = "module_master_id", referencedColumnName = "id")
	private Modules modules;

	@JsonIgnore
	@ManyToMany(mappedBy = "moduleActions")
	@ToString.Exclude // Excluding toString()
	private Collection<Roles> roles;
}
