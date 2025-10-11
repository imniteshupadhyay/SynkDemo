package com.playmotech.api.core.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.repo.UserProfileRepo;

@Configuration
@Qualifier("emailPasswordUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {
	private final UserProfileRepo userProfileRepo;

	public CustomUserDetailsService(UserProfileRepo userProfileRepo) {
		super();
		this.userProfileRepo = userProfileRepo;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//		UserProfile userProfile = userProfileRepo.findByEmailId(username).orElseGet(() -> userProfileRepo
//				.findByPhoneNumber(username).orElseThrow(() -> new UsernameNotFoundException("User not found")));

		UserProfile userProfile = userProfileRepo.findByEmailIdAndPrimaryAccountIsTrue(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return new UserDetail(userProfile.getId(), userProfile.getEmailId(),
				userProfile.getAuthDetails().getPasswordHashed());
	}

}
