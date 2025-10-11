package com.playmotech.api.core.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService,
//			@Qualifier("mobileOtpUserDetailsService") UserDetailsService userDetailsService
			@Qualifier("userDetailsServiceForJwt") UserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			final String jwt = authHeader.substring(7);
			final String userEmail = jwtService.extractUsername(jwt);
			String userId = jwtService.extractUserId(jwt);

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (userId != null && authentication == null) {
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(userId);

				boolean isRefreshToken = jwtService.isRefreshToken(jwt);

				if (isRefreshToken != request.getRequestURI().endsWith("/users/refresh")) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}

				if (jwtService.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());

					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}

			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			log.error("Token is expired ", e);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (Exception exception) {
			log.error("Error occurred while processing JWT token", exception);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}
}