package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.UserType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(exclude = { "userDocuments", "rolesActions" })
public class UserProfile {

	@Id
	private String id;

	@Column(name = "username")
	private String username;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "dob")
	private String dob;

	@Column(name = "user_type")
	@Enumerated(value = EnumType.STRING)
	private UserType userType;

	@OneToOne(fetch = FetchType.EAGER, mappedBy = "userProfile", cascade = CascadeType.ALL)
	private ProfileStats profileStats;

	@Column(name = "google_advertising_id")
	private String googleAdvertisingId;

	@Column(name = "email_id")
	private String emailId;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "profile_picture_url")
	private String profilePictureUrl;

	@Column(name = "about_me")
	private String aboutMe;

	@Column(name = "gender")
	@Enumerated(value = EnumType.STRING)
	private Gender gender;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "userProfile", cascade = CascadeType.ALL)
	private List<UserPreferredSportsMapping> preferredSports;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "inactive")
	private boolean inactive;

	@Column(name = "android_fcm_push_token")
	private String androidFcmPushToken;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "userProfile", cascade = CascadeType.ALL)
	private List<UserRolesMapping> roles;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "userProfile", cascade = CascadeType.ALL)
	private List<UserExpertiseMapping> expertiseLevel;

	// @Column(name = "default_password")
	// private boolean defaultPassword;

	// @Column(name = "password_hashed")
	// private String passwordHashed;

	// @Column(name = "otp_hashed")
	// private String otpHashed;

	// @Column(name = "is_phone_number_verified")
	// private boolean isPhoneNumberVerified;

	// @Column(name = "is_email_id_verified")
	// private boolean isEmailIdVerified;

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

	@Enumerated(value = EnumType.STRING)
	@Column(name = "role")
	private Role role;

	// @ManyToOne
	// @JoinColumn(name = "role_id", referencedColumnName = "id")
	// private Roles rbacRoles;

	@JsonIgnore
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private UsersActionsMapping rolesActions;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private UserDocuments userDocuments;

	// @Column(name = "otp_expiry_time")
	// private Instant otpExpiryTime;

	// @Column(name = "otp_used")
	// private boolean isOtpUsed;

	@Column(name = "is_primary_account", columnDefinition = "boolean default false")
	private boolean primaryAccount;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auth_details", referencedColumnName = "id")
	private UserAuthDetails authDetails;
}
