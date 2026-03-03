package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.ChooseFilmRequest;
import com.rekomenda.api.domain.room.dto.KickRequest;
import com.rekomenda.api.domain.room.dto.SubmitPromptRequest;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Handles inbound STOMP messages from clients on /app/room.* destinations.
 * Outbound events are broadcast by RoomService via SimpMessagingTemplate to /topic/room/{roomId}.
 */
@Controller
public class RoomWebSocketController {

    private final RoomService roomService;

    public RoomWebSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/room.join/{roomId}")
    public void join(
            @DestinationVariable String roomId,
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor
    ) {
        var sessionId = headerAccessor.getSessionId();
        roomService.joinRoom(roomId, UUID.fromString(jwt.getSubject()), sessionId);
    }

    @MessageMapping("/room.leave/{roomId}")
    public void leave(@DestinationVariable String roomId, @AuthenticationPrincipal Jwt jwt) {
        roomService.leaveRoom(roomId, UUID.fromString(jwt.getSubject()));
    }

    @MessageMapping("/room.kick/{roomId}")
    public void kick(
            @DestinationVariable String roomId,
            @AuthenticationPrincipal Jwt jwt,
            @Payload KickRequest request
    ) {
        roomService.kickParticipant(roomId, UUID.fromString(jwt.getSubject()), request.targetUserId());
    }

    @MessageMapping("/room.submit-prompt/{roomId}")
    public void submitPrompt(
            @DestinationVariable String roomId,
            @AuthenticationPrincipal Jwt jwt,
            @Payload SubmitPromptRequest request
    ) {
        roomService.submitPrompt(roomId, UUID.fromString(jwt.getSubject()), request.descricao());
    }

    @MessageMapping("/room.close/{roomId}")
    public void close(@DestinationVariable String roomId, @AuthenticationPrincipal Jwt jwt) {
        roomService.closeRoom(roomId, UUID.fromString(jwt.getSubject()));
    }

    @MessageMapping("/room.more-recommendations/{roomId}")
    public void moreRecommendations(@DestinationVariable String roomId) {
        roomService.requestMoreRecommendations(roomId);
    }

    @MessageMapping("/room.choose-film/{roomId}")
    public void chooseFilm(
            @DestinationVariable String roomId,
            @Payload ChooseFilmRequest request
    ) {
        roomService.chooseFilm(roomId, request.movieId());
    }
}
