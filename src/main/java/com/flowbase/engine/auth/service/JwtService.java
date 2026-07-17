package com.flowbase.engine.auth.service;


import com.flowbase.engine.auth.domain.User;
import com.flowbase.engine.auth.domain.UserRole;
import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.exception.ExpiredTokenException;
import com.flowbase.engine.auth.exception.InvalidTokenException;
import com.flowbase.engine.config.JwtConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@AllArgsConstructor
public class JwtService {
    private final SecretKey secretKey;
    private final JwtConfiguration jwtConfiguration;
    
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(this.jwtConfiguration.getExpriationMinutes()));
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                   .id(jti)
                   .subject(user.email())
                   .claim("userId", user.id())
                   .claim("role", user.role())
                   .issuedAt(Date.from(now))
                   .expiration(Date.from(expiry))
                   .signWith(this.secretKey)
                   .compact();
    }
    
    public AuthenticatedUser parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(this.secretKey).build().parseSignedClaims(token).getPayload();
            return new AuthenticatedUser(
                    claims.get("userId", String.class),
                    claims.getSubject(),
                    UserRole.valueOf(claims.get("role", String.class)),
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("The authentication token is expired!");
        } catch (JwtException e) {
            throw new InvalidTokenException("The authentication token is Invalid or Malformed!");
        }
    }
}
