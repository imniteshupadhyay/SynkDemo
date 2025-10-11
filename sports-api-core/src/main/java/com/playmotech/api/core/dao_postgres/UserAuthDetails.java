package com.playmotech.api.core.dao_postgres;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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

@Entity
@Table(name = "user_auth_details")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserAuthDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "default_password")
	private boolean defaultPassword;

	@Column(name = "password_hashed")
	private String passwordHashed;

	@Column(name = "otp_hashed")
	private String otpHashed;

	@Column(name = "is_phone_number_verified")
	private boolean isPhoneNumberVerified;

	@Column(name = "is_email_id_verified")
	private boolean isEmailIdVerified;

	@Column(name = "otp_expiry_time")
	private Instant otpExpiryTime;

	@Column(name = "otp_used")
	private boolean isOtpUsed;

	@JsonManagedReference
	@OneToMany(mappedBy = "authDetails", fetch = FetchType.LAZY)
	private List<UserProfile> userProfiles;
}
