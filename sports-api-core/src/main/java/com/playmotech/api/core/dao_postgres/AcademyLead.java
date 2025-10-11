package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "academy_leads")
public class AcademyLead {
	@Id
	private String id;
	@Column(name = "academy_name")
	private String academyName;
	@Column(name = "contact_name")
	private String contactName;
	@Column(name = "phone_number")
	private String phoneNumber;
	@Column(name = "about")
	private String about;
	@Column(name = "created_at_timestamp_utc")
	private Timestamp createdAtTimestampUtc;
}
