package com.rekomenda.api.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every request to check if the JWT's jti is in the Redis blacklist
 * (i.e. logged out).
 * Runs before the standard OAuth2 resource server filter chain.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final JwtService jwtService;

    public JwtAuthFilter(JwtDecoder jwtDecoder, JwtService jwtService) {
        this.jwtDecoder = jwtDecoder;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);
        try {
            var jwt = jwtDecoder.decode(token);
            if (jwtService.isTokenRevoked(jwt)) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }
        } catch (JwtException ignored) {
            // Invalid tokens are handled by the downstream resource server filter
        }

        filterChain.doFilter(request, response);
    }
}
