package com.nexo.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "test-only-secret-0123456789abcdef0123456789abcdef0123456789abcd";
    private static final String OTHER_SECRET =
            "other-only-secret-0123456789abcdef0123456789abcdef0123456789abcd";

    private final JwtService jwt = new JwtService(SECRET, 8);

    @Test
    void parseToken_roundTrip_returnsSameIdentity() {
        String token = jwt.generateToken(7L, "tech@nexo.com", "TECHNICIAN");

        Claims claims = jwt.parseToken(token);

        assertThat(claims.get("employeeId", Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.getSubject()).isEqualTo("tech@nexo.com");
        assertThat(claims.get("role", String.class)).isEqualTo("TECHNICIAN");
    }

    @Test
    void parseToken_expired_throwsExpired() {
        JwtService expired = new JwtService(SECRET, -1);
        String token = expired.generateToken(7L, "tech@nexo.com", "TECHNICIAN");

        assertThatThrownBy(() -> expired.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseToken_otherKey_throws() {
        String token = jwt.generateToken(7L, "tech@nexo.com", "TECHNICIAN");
        JwtService otherKey = new JwtService(OTHER_SECRET, 8);

        assertThatThrownBy(() -> otherKey.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_roleEscalatedPayload_throws() {
        String techToken = jwt.generateToken(7L, "tech@nexo.com", "TECHNICIAN");
        String adminToken = jwt.generateToken(7L, "admin@nexo.com", "ADMIN");
        String[] techParts = techToken.split("\\.");
        String[] adminParts = adminToken.split("\\.");
        String forged = techParts[0] + "." + adminParts[1] + "." + techParts[2];

        assertThatThrownBy(() -> jwt.parseToken(forged))
                .isInstanceOf(JwtException.class);
    }
}
