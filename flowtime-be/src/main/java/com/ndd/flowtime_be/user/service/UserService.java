package com.ndd.flowtime_be.user.service;

import com.ndd.flowtime_be.user.entity.User;
import com.ndd.flowtime_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User processGoogleUser(OidcUser oidcUser){
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Email not provided by Google OAuth2");
        }

        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf("@"));
        }

        final String resolvedName = name;

        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(resolvedName)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
