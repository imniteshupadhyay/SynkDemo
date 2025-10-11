package com.playmotech.api.core.views;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.annotation.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Immutable
@Table(name = "trainee_view")
public class TraineeView {

	@Id
	private String id; // user_id

	@Column(name = "username")
	private String username;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "dob")
	private String dob;

	@Column(name = "email_id")
	private String emailId;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "profile_picture_url")
	private String profilePictureUrl;

	@Column(name = "about_me")
	private String aboutMe;

	private String gender;

	@Column(name = "created_on")
	private Timestamp createdOn;

	private boolean inactive;

	@Column(name = "is_phone_number_verified")
	private boolean isPhoneNumberVerified;

	@Column(name = "is_email_id_verified")
	private boolean isEmailIdVerified;

	@Column(name = "experience_in_months")
	private Integer experienceInMonths;

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

	@Column(name = "role")
	private String role;

	private List<String> academyIds;
	private List<String> academyNames;
	private List<String> domainUrls;
	private List<String> statuses;
	private List<String> courseIds;

	private List<String> courseNames;
	private List<String> coachUserIds;
	private List<String> maintainerIds;
	private List<String> managerUserIds;

//    @Column(name = "statuses")
//    private List<String> status;
}
