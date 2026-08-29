package com.ndd.flowtime_be.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app.jwt")
@Configuration
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long accessExpirationMs = 900000L;
    private long refreshExpirationMs = 604800000L;
    private boolean refreshCookieSecure = false;
    private String refreshCookieSameSite = "None";
}
