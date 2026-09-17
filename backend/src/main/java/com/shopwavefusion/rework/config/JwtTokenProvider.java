package com.shopwavefusion.rework.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.domain.UserEntity;

import org.springframework.http.HttpStatus;

@Component
public class JwtTokenProvider {
    private static final long TTL_SECONDS = 30 * 60;
    private final SecretKey key;

    public JwtTokenProvider(@Value("${shopwave.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenData issue(UserEntity user) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(TTL_SECONDS);
        String token = Jwts.builder().setIssuer("shopwave").setAudience("shopwave-api")
                .setSubject(user.getId().toString()).claim("tokenVersion", user.getTokenVersion())
                .claim("role", user.getRole().name()).setIssuedAt(Date.from(now)).setExpiration(Date.from(expires))
                .setId(UUID.randomUUID().toString()).signWith(key).compact();
        return new TokenData(token, expires, user.getId(), user.getTokenVersion());
    }

    public TokenData parse(String raw) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).requireIssuer("shopwave")
                    .requireAudience("shopwave-api").build().parseClaimsJws(raw).getBody();
            return new TokenData(raw, claims.getExpiration().toInstant(), UUID.fromString(claims.getSubject()),
                    ((Number) claims.get("tokenVersion")).intValue());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is invalid");
        }
    }

    public record TokenData(String value, Instant expiresAt, UUID userId, int tokenVersion) {}
}
