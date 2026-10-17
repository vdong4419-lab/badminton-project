package com.dong.dat_san;

import com.dong.dat_san.config.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTests {
    @Test
    void generatedKeysSignAndRejectTokensFromAnotherInstance() {
        var issuer = new JwtTokenProvider("", 3600000);
        var other = new JwtTokenProvider("", 3600000);
        String token = issuer.generateToken("test@example.com");
        assertThat(issuer.validateToken(token)).isTrue();
        assertThat(issuer.getEmailFromJWT(token)).isEqualTo("test@example.com");
        assertThat(other.validateToken(token)).isFalse();
        assertThat(issuer.validateToken("invalid")).isFalse();
    }

    @Test
    void rejectsShortConfiguredKeyAndExpiredTokens() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new JwtTokenProvider(Base64.getEncoder().encodeToString(new byte[58]), 3600000));
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtTokenProvider("", 0));
        byte[] bytes = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
        var verifier = new JwtTokenProvider(Base64.getEncoder().encodeToString(bytes), 3600000);
        String expired = Jwts.builder().setSubject("test@example.com").setExpiration(new Date(0))
                .signWith(Keys.hmacShaKeyFor(bytes), SignatureAlgorithm.HS512).compact();
        assertThat(verifier.validateToken(expired)).isFalse();
    }
}
