package com.ndd.flowtime_be.security.controller;

import com.ndd.flowtime_be.security.service.JwtService;
import com.ndd.flowtime_be.security.service.RefreshTokenService;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "timezone", user.getTimezone()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = RefreshTokenService.REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie
    ) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token is missing"));
        }

        try {
            RefreshTokenService.TokenRotationResult result = refreshTokenService.verifyAndRotateRefreshToken(refreshTokenCookie);
            String newAccessToken = jwtService.generateToken(result.user());
            ResponseCookie newCookie = refreshTokenService.createRefreshCookie(result.newRefreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, newCookie.toString())
                    .body(Map.of(
                            "accessToken", newAccessToken,
                            "token", newAccessToken
                    ));
        } catch (IllegalArgumentException ex) {
            log.warn("Refresh token failed: {}", ex.getMessage());
            ResponseCookie cleanCookie = refreshTokenService.createCleanRefreshCookie();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = RefreshTokenService.REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie
    ) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshTokenCookie);
        }

        ResponseCookie cleanCookie = refreshTokenService.createCleanRefreshCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
