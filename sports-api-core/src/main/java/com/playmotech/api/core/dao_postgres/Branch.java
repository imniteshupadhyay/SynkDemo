package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

/**
 * Created By: deep.patel
 **/

@Entity
@Table(name = "branches")
@Data
@ToString(exclude = "academy")
public class Branch {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	@JsonManagedReference
	private Academy academy;
	@Column(name = "name")
	private String name;
	@Column(name = "address_line_1")
	private String addressLine1;
	@Column(name = "address_line_2")
	private String addressLine2;
	@Column(name = "pincode")
	private String pincode;
	@Column(name = "city")
	private String city;
	@Column(name = "state")
	private String state;
	@Column(name = "country")
	private String country;
	@Column(name = "phone_number")
	private String phoneNumber;
	@Column(name = "inactive")
	private boolean inactive;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "updated_on")
	private Timestamp updatedOn;
}
