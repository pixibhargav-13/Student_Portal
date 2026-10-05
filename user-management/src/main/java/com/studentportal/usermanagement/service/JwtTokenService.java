package com.studentportal.usermanagement.service;

import com.studentportal.usermanagement.config.JwtProperties;
import com.studentportal.usermanagement.model.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Issues and parses JWTs. Subject is the user id; {@code role} and {@code type} (access/refresh)
 * are added as claims so the frontend can read the role without an extra call.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {
  public static final String CLAIM_TYPE = "type";
  public static final String CLAIM_ROLE = "role";
  public static final String TYPE_ACCESS = "access";
  public static final String TYPE_REFRESH = "refresh";

  private final JwtProperties jwtProperties;
  private SecretKey signingKey;

  @PostConstruct
  void initKey() {
    signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretKey()));
  }

  public String generateAccessToken(UserEntity user) {
    return generateToken(user, TYPE_ACCESS, jwtProperties.getAccessTokenValidity());
  }

  public String generateRefreshToken(UserEntity user) {
    return generateToken(user, TYPE_REFRESH, jwtProperties.getRefreshTokenValidity());
  }

  public long accessTokenValiditySeconds() {
    return jwtProperties.getAccessTokenValidity() / 1000;
  }

  /**
   * Verifies the signature and expiry and returns the claims.
   *
   * @throws io.jsonwebtoken.ExpiredJwtException when expired
   * @throws io.jsonwebtoken.JwtException for any other invalid token
   */
  public Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  private String generateToken(UserEntity user, String type, long validityMillis) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim(CLAIM_TYPE, type)
        .claim(CLAIM_ROLE, user.getRole().name())
        .issuedAt(new Date(now))
        .expiration(new Date(now + validityMillis))
        .signWith(signingKey)
        .compact();
  }
}
