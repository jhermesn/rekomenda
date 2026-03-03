package com.rekomenda.api.domain.room.dto;

/**
 * Envelope for all outbound STOMP messages on /topic/room/{roomId}.
 * The payload is kept as Object to accommodate different event data shapes.
 */
public record RoomEvent(EventType type, Object payload) {

    public enum EventType {
        PARTICIPANT_JOINED,
        PARTICIPANT_LEFT,
        PARTICIPANT_KICKED,
        PARTICIPANT_READY,
        RECOMMENDATIONS_READY,
        MORE_RECOMMENDATIONS_READY,
        FILM_CHOSEN,
        ROOM_CLOSED,
        HOST_DISCONNECTED
    }

    public static RoomEvent of(EventType type, Object payload) {
        return new RoomEvent(type, payload);
    }
}
