package com.playmotech.api.core.dao_postgres;

import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Roles {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "role_name", nullable = false)
	private String roleName;

	@Column(name = "description")
	private String description;

	@Column(name = "editable", columnDefinition = "boolean default true")
	private boolean editable = true; // true == it is editable

	@Column(name = "sequence")
	private Long sequence;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "roles_modules_action_mapping", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "action_id", referencedColumnName = "id"))
	private Collection<ModulesActions> moduleActions;
}
