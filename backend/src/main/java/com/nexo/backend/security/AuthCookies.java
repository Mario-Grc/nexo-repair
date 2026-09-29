package com.nexo.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Builds the session cookie from properties so token and cookie lifetimes stay in sync.
@Component
public class AuthCookies {

    private final long expirationHours;
    private final boolean secure;

    public AuthCookies(@Value("${nexo.jwt.expiration-hours}") long expirationHours,
                       @Value("${nexo.cookie.secure}") boolean secure) {
        this.expirationHours = expirationHours;
        this.secure = secure;
    }

    public ResponseCookie session(String token) {
        return ResponseCookie.from(JwtService.TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(expirationHours))
                .build();
    }

    public ResponseCookie cleared() {
        return ResponseCookie.from(JwtService.TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
