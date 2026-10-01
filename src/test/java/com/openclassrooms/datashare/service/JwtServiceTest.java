package com.openclassrooms.datashare.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {
    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generateTokenCreatesAThreePartJwt() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken(user("john"));

        assertThat(token).matches("^[^.]+\\.[^.]+\\.[^.]+$");
    }

    @Test
    void extractUsernameReturnsTokenSubject() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(user("john"));

        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void tokenIsValidForTheSameUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails user = user("john");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForAnotherUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(user("john"));

        assertThat(jwtService.isTokenValid(token, user("jane"))).isFalse();
    }

    @Test
    void expiredTokenCannotBeValidated() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.generateToken(user("john"));

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user("john")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretCannotBeParsed() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService(
                "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=", 60_000);
        String token = issuer.generateToken(user("john"));

        assertThatThrownBy(() -> verifier.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }
}
