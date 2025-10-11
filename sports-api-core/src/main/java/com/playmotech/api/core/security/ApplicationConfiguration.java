package com.playmotech.api.core.security;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.amazonaws.util.CollectionUtils;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.IUserProfileService;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class ApplicationConfiguration {
	private final IUserProfileService userProfileService;
	private UserProfileRepo userRepo;

	public ApplicationConfiguration(IUserProfileService userProfileService, UserProfileRepo userRepo) {
		this.userProfileService = userProfileService;
		this.userRepo = userRepo;
	}

	@Bean
	BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean("mobileOtpUserDetailsService")
	UserDetailsService userDetailsService() throws UsernameNotFoundException {
		return username -> userProfileService.userDetail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Bean("userDetailsServiceForJwt")
	UserDetailsService userDetailsServiceForJwt() throws UsernameNotFoundException {
		return userId -> userProfileService.userById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("User with id not found"));
	}

	@Bean("customAuthProvider")
	AuthenticationProvider authenticationProvider(BCryptPasswordEncoder passwordEncoder) {
		return new CustomAuthenticationProvider(userProfileService, passwordEncoder, userRepo);
	}

	private static class CustomAuthenticationProvider implements AuthenticationProvider {
		private final IUserProfileService userProfileService;
		private final BCryptPasswordEncoder passwordEncoder;
		private final UserProfileRepo userRepo;

		public CustomAuthenticationProvider(IUserProfileService userProfileService,
				BCryptPasswordEncoder passwordEncoder, UserProfileRepo userRepo) {
			this.userProfileService = userProfileService;
			this.passwordEncoder = passwordEncoder;
			this.userRepo = userRepo;
		}

		@Value("${otp-bypass}")
		private Boolean otpByPass;

		@Value("${otp-bypass-username}")
		private List<String> otpByPassUsernames;

		private boolean shouldBypassOtp(String username) {
			if (this.otpByPass) {
				return true;
			}
			return !CollectionUtils.isNullOrEmpty(otpByPassUsernames)
					&& otpByPassUsernames.contains(username);
		}

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			String username = authentication.getName();
			String otpHashed = authentication.getCredentials().toString();

			UserDetail userDetail = userProfileService.userDetail(username)
					.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

			UserProfile userProfile = this.userRepo.findByUsernameAndPrimaryAccountIsTrue(userDetail.getUsername())
					.orElseThrow(() -> new BadCredentialsException("Invalid Password"));

			// Usage:
			boolean isOtpByPass = shouldBypassOtp(username);
			log.warn("isOtpByPass in CustomAuthenticationProvider is {}", isOtpByPass);

			if (!isOtpByPass) {
				if (userProfile.getAuthDetails().isOtpUsed()) {
					throw new BadCredentialsException("OTP is already used. Please generate a new OTP and try again");
				}

				if (userProfile.getAuthDetails().getOtpExpiryTime().isBefore(Instant.now())) {
					throw new BadCredentialsException("OTP is expired. Please generate a new OTP and try again");
				}
			}

			if (!passwordEncoder.matches(otpHashed, userDetail.getPassword())) {
				throw new BadCredentialsException("Invalid OTP");
			}

			return new UsernamePasswordAuthenticationToken(userDetail, otpHashed, userDetail.getAuthorities());
		}

		@Override
		public boolean supports(Class<?> authentication) {
			return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
		}
	}
}
