package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Created By: deep.patel
 **/

@Data
@Entity
@Table(name = "academies")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = { "branches", "academyCourseMappings", "academySportMappings" })
public class Academy {
	@Id
	private String id;
	@Column(name = "internal_id")
	private String internalId;
	@Column(name = "name")
	private String name;
	@Column(name = "manager_user_id")
	private String managerUserId;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "academy", cascade = CascadeType.ALL)
	@JsonIgnore
	private List<Branch> branches;

	@OneToMany(mappedBy = "academy", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<GeoFence> geoFences;
	@Column(name = "inactive")
	private boolean inactive;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "email_id")
	private String emailId;
	@Column(name = "phone_number")
	private String phoneNumber;
	@Column(name = "headquarter")
	private String headquarter;
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
	@Column(name = "start_time")
	private String startTime;
	@Column(name = "end_time")
	private String endTime;
	@Column(name = "icon_url")
	private String iconUrl;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "academy", cascade = CascadeType.ALL)
	@JsonIgnore
	@JsonBackReference
	private List<AcademySportMapping> academySportMappings;

//	@OneToMany(fetch = FetchType.EAGER, mappedBy = "academy", cascade = CascadeType.ALL)
//	private List<AcademySportMapping> academySportMappings;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "academy", cascade = CascadeType.ALL)
	private List<Course> academyCourseMappings;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organisation_id", referencedColumnName = "id")
	private Organisation org;

}
