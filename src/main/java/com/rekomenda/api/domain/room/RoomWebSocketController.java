package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.ChooseFilmRequest;
import com.rekomenda.api.domain.room.dto.KickRequest;
import com.rekomenda.api.domain.room.dto.SubmitPromptRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Handles inbound STOMP messages from clients on /app/room.* destinations.
 * Outbound events are broadcast by RoomService via SimpMessagingTemplate to
 * /topic/room/{roomId}.
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
            java.security.Principal principal,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        var sessionId = headerAccessor.getSessionId();
        roomService.joinRoom(roomId, UUID.fromString(principal.getName()), sessionId);
    }

    @MessageMapping("/room.leave/{roomId}")
    public void leave(@DestinationVariable String roomId, java.security.Principal principal) {
        roomService.leaveRoom(roomId, UUID.fromString(principal.getName()));
    }

    @MessageMapping("/room.kick/{roomId}")
    public void kick(
            @DestinationVariable String roomId,
            java.security.Principal principal,
            @Valid @Payload KickRequest request) {
        roomService.kickParticipant(roomId, UUID.fromString(principal.getName()), request.targetUserId());
    }

    @MessageMapping("/room.submit-prompt/{roomId}")
    public void submitPrompt(
            @DestinationVariable String roomId,
            java.security.Principal principal,
            @Valid @Payload SubmitPromptRequest request) {
        roomService.submitPrompt(roomId, UUID.fromString(principal.getName()), request.descricao());
    }

    @MessageMapping("/room.close/{roomId}")
    public void close(@DestinationVariable String roomId, java.security.Principal principal) {
        roomService.closeRoom(roomId, UUID.fromString(principal.getName()));
    }

    @MessageMapping("/room.generate-recommendations/{roomId}")
    public void generateRecommendations(@DestinationVariable String roomId, java.security.Principal principal) {
        roomService.generateRecommendations(roomId, UUID.fromString(principal.getName()));
    }

    @MessageMapping("/room.choose-film/{roomId}")
    public void chooseFilm(
            @DestinationVariable String roomId,
            @Valid @Payload ChooseFilmRequest request) {
        roomService.chooseFilm(roomId, request.movieId());
    }

    @MessageMapping("/room.reset/{roomId}")
    public void resetRoom(@DestinationVariable String roomId, java.security.Principal principal) {
        roomService.resetRoom(roomId, UUID.fromString(principal.getName()));
    }
}
