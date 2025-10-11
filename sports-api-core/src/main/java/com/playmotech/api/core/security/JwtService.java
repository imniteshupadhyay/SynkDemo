package com.playmotech.api.core.security;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dto.UserDetail;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {
	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration-time}")
	private long jwtExpiration;

	@Value("${security.jwt.refresh-expiration-time}")
	private long jwtRefreshExpiration;

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}
	
	public String extractUserId(String token) {
		String userId = extractClaim(token, Claims::getId);
		return userId;
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public Boolean isRefreshToken(String token) {
		final Claims claims = extractAllClaims(token);
		return claims.get("isRefreshToken", Boolean.class);
	}

	public String generateToken(UserDetail userDetail) {
		Map<String, Object> extraClaims = new HashMap<>();
		extraClaims.put("isRefreshToken", false);
		extraClaims.put("jti", userDetail.getUserId());
		return generateToken(extraClaims, userDetail, jwtExpiration);
	}

	public String generateRefreshToken(UserDetail userDetail) {
		Map<String, Object> extraClaims = new HashMap<>();
		extraClaims.put("isRefreshToken", true);
		extraClaims.put("jti", userDetail.getUserId());
		return generateToken(extraClaims, userDetail, jwtRefreshExpiration);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		return buildToken(extraClaims, userDetails, expiration);
	}

	public long getExpirationTime() {
		return jwtExpiration;
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(Instant.now().toEpochMilli()))
				.setExpiration(new Date(Instant.now().toEpochMilli() + expiration))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
	}

	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
