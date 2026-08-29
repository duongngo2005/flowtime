package com.ndd.flowtime_be.security.service;

import com.ndd.flowtime_be.security.config.JwtProperties;
import com.ndd.flowtime_be.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN = "access";

    private final JwtProperties jwtProperties;

    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("name", user.getName());
        extraClaims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN);
        return buildToken(extraClaims, user.getEmail(), jwtProperties.getAccessExpirationMs());
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(User user, String token) {
        Claims claims = extractClaims(token);
        return ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && claims.getSubject().equalsIgnoreCase(user.getEmail());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims()
                .add(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .and()
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        String secret = jwtProperties.getSecret();
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e1) {
            try {
                byte[] keyBytes = Decoders.BASE64URL.decode(secret);
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception e2) {
                byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                return Keys.hmacShaKeyFor(keyBytes);
            }
        }
    }
}
