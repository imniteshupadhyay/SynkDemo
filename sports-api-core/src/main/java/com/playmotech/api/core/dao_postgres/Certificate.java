package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "certificate")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@ToString(exclude = { "academy", "course", "user" })
@EqualsAndHashCode(exclude = { "academy", "course", "user" })
public class Certificate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@CreationTimestamp
	@Column(name = "created_on", updatable = false)
	private Timestamp createdOn;

	@UpdateTimestamp
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Column(name = "issued_on")
	private Timestamp issuedOn;

	@ManyToOne
	@JoinColumn(name = "created_by", referencedColumnName = "id")
	private UserProfile createdBy;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "program_id", referencedColumnName = "id")
	private Course course;
	
	@Column(name = "event_name")
	private String eventName;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile user;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "certificate_type")
	private String certificateType;

	@Column(name = "rank")
	private Long rank;

	@Column(name = "certificate_url")
	private String certificateUrl;

	@Column(name = "deleted")
	private Boolean inactive;

}
