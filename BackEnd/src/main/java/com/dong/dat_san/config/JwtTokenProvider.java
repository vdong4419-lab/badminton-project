package com.dong.dat_san.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final Key signingKey;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${app.jwt.secret:}") String secret,
                            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("Thời hạn JWT phải lớn hơn 0.");
        }
        this.expirationMs = expirationMs;
        if (secret.isBlank()) {
            signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            log.info("Dùng khóa JWT tạm thời cho phát triển; token hết hiệu lực sau khi khởi động lại. Cấu hình JWT_SECRET khi triển khai.");
        } else {
            byte[] bytes = Decoders.BASE64.decode(secret);
            if (bytes.length < 64) {
                throw new IllegalArgumentException("JWT_SECRET phải là Base64 của ít nhất 64 byte ngẫu nhiên.");
            }
            signingKey = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder().setSubject(email).setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS512).compact();
    }

    public String getEmailFromJWT(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            String email = getEmailFromJWT(token);
            return email != null && !email.isBlank();
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
