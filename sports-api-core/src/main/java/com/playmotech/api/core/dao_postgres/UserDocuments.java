package com.playmotech.api.core.dao_postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "user_documents")
@EqualsAndHashCode(exclude = { "user" })
public class UserDocuments {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "aadhar_url", columnDefinition = "TEXT")
	private String aadharUrl;

	@Column(name = "pan_url", columnDefinition = "TEXT")
	private String panUrl;

	@OneToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	@JsonBackReference
	private UserProfile user;
}