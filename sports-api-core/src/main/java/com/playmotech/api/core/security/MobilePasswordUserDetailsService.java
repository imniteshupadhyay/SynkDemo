package com.playmotech.api.core.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Qualifier("mobilePasswordUserDetailsService")
@Slf4j
public class MobilePasswordUserDetailsService implements UserDetailsService {
	private final UserProfileRepo userProfileRepo;

	public MobilePasswordUserDetailsService(UserProfileRepo userProfileRepo) {
		super();
		this.userProfileRepo = userProfileRepo;
	}

//	@Override
//	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//		UserProfile userProfile = userProfileRepo.findByUsername(username)
//				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
//		
//		return new UserDetail(userProfile.getId(), userProfile.getUsername(), userProfile.getPasswordHashed());
//	}
	
	@Override
	public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
		log.info("id {}", id);
		String username = id; // for mobile + password login flow
		String userId = id; // for jwt service (to generate token)
//		UserProfile userProfile = userProfileRepo.findById(id)
//				.orElse(userProfileRepo.findByUsernameAndPrimaryAccountIsTrue(id)
//						.orElseThrow(() -> new UsernameNotFoundException("User not found"))
//						);
		// we either receive user_id(for jwt service) or username(for mobile + password login flow)
		UserProfile userProfile = userProfileRepo.findById(userId)
				.orElse(userProfileRepo.findByUsernameAndPrimaryAccountIsTrue(username)
						.orElseThrow(() -> new UsernameNotFoundException("User not found"))
						);
		
		return new UserDetail(userProfile.getId(), userProfile.getUsername(), userProfile.getAuthDetails().getPasswordHashed());
	}

}
