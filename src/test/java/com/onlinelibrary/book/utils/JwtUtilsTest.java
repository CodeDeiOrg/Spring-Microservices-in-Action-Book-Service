package com.onlinelibrary.book.utils;

import com.onlinelibrary.book.exception.EmailMissingException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    @Test
    void extractEmail_withEmailClaim_returnsEmail() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("email", "user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThat(JwtUtils.extractEmail(jwt)).isEqualTo("user@test.com");
    }

    @Test
    void extractEmail_withMissingEmailClaim_throwsEmailMissingException() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThatThrownBy(() -> JwtUtils.extractEmail(jwt))
                .isInstanceOf(EmailMissingException.class);
    }
}
