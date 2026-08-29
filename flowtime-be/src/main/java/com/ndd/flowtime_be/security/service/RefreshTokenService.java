package com.ndd.flowtime_be.security.service;

import com.ndd.flowtime_be.security.config.JwtProperties;
import com.ndd.flowtime_be.security.entity.RefreshToken;
import com.ndd.flowtime_be.security.repository.RefreshTokenRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String createRefreshToken(User user) {
        String tokenStr = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenStr)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }

    @Transactional
    public TokenRotationResult verifyAndRotateRefreshToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing");
        }

        RefreshToken oldToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (!oldToken.isValid()) {
            refreshTokenRepository.deleteByToken(tokenStr);
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user = oldToken.getUser();
        String userId = user.getEmail();
        log.debug("Rotating refresh token for user: {}", userId);

        refreshTokenRepository.deleteByToken(tokenStr);

        String newRefreshTokenStr = createRefreshToken(user);

        return new TokenRotationResult(user, newRefreshTokenStr);
    }

    @Transactional
    public void revokeRefreshToken(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            refreshTokenRepository.deleteByToken(tokenStr);
        }
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path("/")
                .maxAge(jwtProperties.getRefreshExpirationMs() / 1000)
                .build();
    }

    public ResponseCookie createCleanRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }

    public record TokenRotationResult(User user, String newRefreshToken) {}
}