package com.playmotech.api.core.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	private final AuthenticationProvider authenticationProvider;
	private final UsernamePasswordAuthenticationProvider usernamePasswordAuthProvider;
	private final MobilePasswordAuthenticationProvider mobilePasswordAuthenticationProvider;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
			@Qualifier(value = "customAuthProvider") AuthenticationProvider authenticationProvider,
			@Qualifier(value = "emailPasswordAuthProvider") UsernamePasswordAuthenticationProvider usernamePasswordAuthProvider,
			@Qualifier(value = "mobilePasswordAuthProvider") MobilePasswordAuthenticationProvider mobilePasswordAuthenticationProvider) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.authenticationProvider = authenticationProvider;
		this.usernamePasswordAuthProvider = usernamePasswordAuthProvider;
		this.mobilePasswordAuthenticationProvider = mobilePasswordAuthenticationProvider;
	}

	@Bean
	AuthenticationManager authenticationManager() {
		return new ProviderManager(
				List.of(authenticationProvider, usernamePasswordAuthProvider, mobilePasswordAuthenticationProvider));
	}

	// @Bean
	// SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	// http.csrf(AbstractHttpConfigurer::disable)
	// .authorizeHttpRequests(auth -> auth.requestMatchers("/users/**",
	// "/actuator/**", "/academies/lead", "/v3/api-docs", "/academies/**",
	// "/video-analyzer/**", "/coaches/**", "/trainees/**", "/leads/**",
	// "/trials/**", "/web/**")
	// .permitAll()
	// .anyRequest()
	// .authenticated())
	// .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	//// .cors(c -> c.disable())
	// .sessionManagement(session ->
	// session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	// .addFilterBefore(jwtAuthenticationFilter,
	// UsernamePasswordAuthenticationFilter.class);
	//
	// return http.build();
	//
	// }

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth
				.requestMatchers("/web/academy/config/**", "/users", "/users/*", "/users/login", "/users/password-change", "/users/forgot-password"
						,"/users/otp/**", "/users/exists/**", "/users/phone/**", "/users/search/**","/users/notification/**", "/users/phone/**", "/actuator/**", "/academies/lead", "/v3/api-docs", "/academies/**")
				.permitAll().anyRequest().authenticated())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("http://sta-*.playmotech.com", "https://*.playmotech.com",
				"http://localhost:5173", "http://localhost:8005"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		// Include both uppercase and lowercase versions of the Authorization header
		configuration.setAllowedHeaders(List.of("authorization", "Authorization", "Content-Type", "Accept", "DNT",
				"User-Agent", "X-Requested-With", "If-Modified-Since", "Cache-Control", "Range"));
		configuration.setExposedHeaders(List.of("Content-Length", "Content-Range"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
