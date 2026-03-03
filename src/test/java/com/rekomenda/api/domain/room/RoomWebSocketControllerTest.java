package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.ChooseFilmRequest;
import com.rekomenda.api.domain.room.dto.KickRequest;
import com.rekomenda.api.domain.room.dto.SubmitPromptRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomWebSocketControllerTest {

    private final RoomService roomService = mock(RoomService.class);
    private final RoomWebSocketController controller = new RoomWebSocketController(roomService);

    private Jwt jwtWithSubject(UUID userId) {
        return new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId.toString()));
    }

    @Test
    @DisplayName("@MessageMapping room.join delega para RoomService.joinRoom com sessionId")
    void join_delegatesToServiceWithSessionId() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var jwt = jwtWithSubject(userId);

        var headers = mock(SimpMessageHeaderAccessor.class);
        when(headers.getSessionId()).thenReturn("session-123");

        controller.join(roomId, jwt, headers);

        verify(roomService).joinRoom(roomId, userId, "session-123");
    }

    @Test
    @DisplayName("@MessageMapping room.leave delega para RoomService.leaveRoom")
    void leave_delegatesToService() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var jwt = jwtWithSubject(userId);

        controller.leave(roomId, jwt);

        verify(roomService).leaveRoom(roomId, userId);
    }

    @Test
    @DisplayName("@MessageMapping room.kick delega para RoomService.kickParticipant")
    void kick_delegatesToService() {
        var roomId = "room-1";
        var hostId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var jwt = jwtWithSubject(hostId);
        var request = new KickRequest(targetId);

        controller.kick(roomId, jwt, request);

        verify(roomService).kickParticipant(roomId, hostId, targetId);
    }

    @Test
    @DisplayName("@MessageMapping room.submit-prompt delega para RoomService.submitPrompt")
    void submitPrompt_delegatesToService() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var jwt = jwtWithSubject(userId);
        var request = new SubmitPromptRequest("Quero um filme de ação leve");

        controller.submitPrompt(roomId, jwt, request);

        verify(roomService).submitPrompt(roomId, userId, request.descricao());
    }

    @Test
    @DisplayName("@MessageMapping room.close delega para RoomService.closeRoom")
    void close_delegatesToService() {
        var roomId = "room-1";
        var hostId = UUID.randomUUID();
        var jwt = jwtWithSubject(hostId);

        controller.close(roomId, jwt);

        verify(roomService).closeRoom(roomId, hostId);
    }

    @Test
    @DisplayName("@MessageMapping room.more-recommendations delega para RoomService.requestMoreRecommendations")
    void moreRecommendations_delegatesToService() {
        var roomId = "room-1";

        controller.moreRecommendations(roomId);

        verify(roomService).requestMoreRecommendations(roomId);
    }

    @Test
    @DisplayName("@MessageMapping room.choose-film delega para RoomService.chooseFilm")
    void chooseFilm_delegatesToService() {
        var roomId = "room-1";
        var request = new ChooseFilmRequest(42L);

        controller.chooseFilm(roomId, request);

        verify(roomService).chooseFilm(roomId, request.movieId());
    }
}

