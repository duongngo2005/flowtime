package com.ndd.flowtime_be.google_account.service;

import com.ndd.flowtime_be.google_account.entity.GoogleAccount;
import com.ndd.flowtime_be.google_account.repository.GoogleAccountRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAccountService {

    private final GoogleAccountRepository googleAccountRepository;
    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Transactional
    public GoogleAccount saveOrUpdate(User user, OAuth2AuthorizedClient authorizedClient) {
        String googleAccountId = authorizedClient.getPrincipalName();
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();

        String refreshToken = null;
        if (authorizedClient.getRefreshToken() != null) {
            refreshToken = authorizedClient.getRefreshToken().getTokenValue();
        }

        final String finalRefreshToken = refreshToken;

        Optional<GoogleAccount> existing = googleAccountRepository.findByUser(user);

        if (existing.isPresent()) {
            GoogleAccount account = existing.get();
            account.setAccessToken(accessToken);
            account.setExpiresAt(expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600));
            if (finalRefreshToken != null) {
                account.setRefreshToken(finalRefreshToken);
            }
            return googleAccountRepository.save(account);
        }

        GoogleAccount newAccount = GoogleAccount.builder()
                .user(user)
                .googleAccountId(googleAccountId)
                .accessToken(accessToken)
                .refreshToken(finalRefreshToken)
                .expiresAt(expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600))
                .build();

        return googleAccountRepository.save(newAccount);
    }

    public Optional<GoogleAccount> findByUser(User user) {
        return googleAccountRepository.findByUser(user);
    }

    public boolean isConnected(User user) {
        return googleAccountRepository.existsByUser(user);
    }

    @Transactional
    public String getValidAccessToken(User user) {
        GoogleAccount account = googleAccountRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Google account is not connected."
                ));

        if (!account.isAccessTokenExpired()) {
            return account.getAccessToken();
        }

        if (account.getRefreshToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Google account needs to be reconnected."
            );
        }

        log.info("Google access token expired for user {}. Refreshing...", user.getEmail());
        refreshAccessToken(account);
        return account.getAccessToken();
    }

    @Transactional
    public void disconnect(User user) {
        googleAccountRepository.findByUser(user).ifPresent(googleAccountRepository::delete);
    }

    private void refreshAccessToken(GoogleAccount account) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", account.getRefreshToken());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("Failed to refresh Google access token.");
        }

        String newAccessToken = (String) response.get("access_token");
        int expiresIn = (int) response.getOrDefault("expires_in", 3600);

        account.setAccessToken(newAccessToken);
        account.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        googleAccountRepository.save(account);

        log.info("Google access token refreshed for user {}.", account.getUser().getEmail());
    }
}
