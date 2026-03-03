package com.rekomenda.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Bean
    public JwtEncoder jwtEncoder() {
        var secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        var secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
