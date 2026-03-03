package com.rekomenda.api.domain.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket session disconnections.
 * When the disconnected session belongs to a room host, the room is terminated
 * and all participants are notified via HOST_DISCONNECTED.
 */
@Component
public class RoomSessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(RoomSessionEventListener.class);

    private final RoomService roomService;

    public RoomSessionEventListener(RoomService roomService) {
        this.roomService = roomService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        var sessionId = event.getSessionId();
        log.debug("WebSocket session disconnected: {}", sessionId);
        roomService.handleHostDisconnect(sessionId);
    }
}
