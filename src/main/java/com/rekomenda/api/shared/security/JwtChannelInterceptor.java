package com.rekomenda.api.shared.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Validates JWT on WebSocket CONNECT frames so that only authenticated users can open a STOMP session.
 */
@NullMarked
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtService jwtService;
    private final JwtAuthenticationConverter authConverter;

    public JwtChannelInterceptor(JwtDecoder jwtDecoder, JwtService jwtService) {
        this.jwtDecoder = jwtDecoder;
        this.jwtService = jwtService;
        this.authConverter = new JwtAuthenticationConverter();
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        var token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            var jwt = jwtDecoder.decode(token.substring(7));
            if (!jwtService.isTokenRevoked(jwt)) {
                Authentication auth = authConverter.convert(jwt);
                accessor.setUser(auth);
            }
        }

        return message;
    }
}
