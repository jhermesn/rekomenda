package com.rekomenda.api.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long expirationMs;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.redisTemplate = redisTemplate;
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject) {
        var now = Instant.now();
        var expiry = now.plusMillis(expirationMs);
        var claims = JwtClaimsSet.builder()
                .issuer("rekomenda-api")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiry)
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Adds the token's jti to the Redis blacklist with TTL matching the token's remaining validity,
     * effectively revoking it for the logout use case.
     */
    public void revokeToken(String token) {
        Jwt decoded = jwtDecoder.decode(token);
        var expiry = decoded.getExpiresAt();
        if (expiry == null) return;

        var remaining = Duration.between(Instant.now(), expiry);
        if (remaining.isNegative()) return;

        redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + decoded.getId(), "revoked", remaining);
    }

    public boolean isTokenRevoked(Jwt jwt) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jwt.getId()));
    }
}
