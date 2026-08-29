package com.ndd.flowtime_be.security.handler;

import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.security.service.JwtService;
import com.ndd.flowtime_be.security.service.RefreshTokenService;
import com.ndd.flowtime_be.user.entity.User;
import com.ndd.flowtime_be.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleAccountService googleAccountService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        User user = userService.processGoogleUser(oidcUser);

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (authorizedClient != null) {
            googleAccountService.saveOrUpdate(user, authorizedClient);
            log.info("Google credentials saved for user {}. refresh_token present: {}",
                    user.getEmail(),
                    authorizedClient.getRefreshToken() != null);
        } else {
            log.warn("OAuth2AuthorizedClient not found for user {}. Google credentials NOT saved.", user.getEmail());
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = refreshTokenService.createRefreshCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        log.info("User {} authenticated. Redirecting to frontend.", user.getEmail());
        response.sendRedirect(targetUrl);
    }
}
