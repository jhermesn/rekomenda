package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.ChooseFilmRequest;
import com.rekomenda.api.domain.room.dto.KickRequest;
import com.rekomenda.api.domain.room.dto.SubmitPromptRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import java.security.Principal;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomWebSocketControllerTest {

    private final RoomService roomService = mock(RoomService.class);
    private final RoomWebSocketController controller = new RoomWebSocketController(roomService);

    private Principal principalWithSubject(UUID userId) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(userId.toString());
        return principal;
    }

    @Test
    @DisplayName("@MessageMapping room.join delega para RoomService.joinRoom com sessionId")
    void join_delegatesToServiceWithSessionId() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var principal = principalWithSubject(userId);

        var headers = mock(SimpMessageHeaderAccessor.class);
        when(headers.getSessionId()).thenReturn("session-123");

        controller.join(roomId, principal, headers);

        verify(roomService).joinRoom(roomId, userId, "session-123");
    }

    @Test
    @DisplayName("@MessageMapping room.leave delega para RoomService.leaveRoom")
    void leave_delegatesToService() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var principal = principalWithSubject(userId);

        controller.leave(roomId, principal);

        verify(roomService).leaveRoom(roomId, userId);
    }

    @Test
    @DisplayName("@MessageMapping room.kick delega para RoomService.kickParticipant")
    void kick_delegatesToService() {
        var roomId = "room-1";
        var hostId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var principal = principalWithSubject(hostId);
        var request = new KickRequest(targetId);

        controller.kick(roomId, principal, request);

        verify(roomService).kickParticipant(roomId, hostId, targetId);
    }

    @Test
    @DisplayName("@MessageMapping room.submit-prompt delega para RoomService.submitPrompt")
    void submitPrompt_delegatesToService() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        var principal = principalWithSubject(userId);
        var request = new SubmitPromptRequest("Quero um filme de ação leve");

        controller.submitPrompt(roomId, principal, request);

        verify(roomService).submitPrompt(roomId, userId, request.descricao());
    }

    @Test
    @DisplayName("@MessageMapping room.close delega para RoomService.closeRoom")
    void close_delegatesToService() {
        var roomId = "room-1";
        var hostId = UUID.randomUUID();
        var principal = principalWithSubject(hostId);

        controller.close(roomId, principal);

        verify(roomService).closeRoom(roomId, hostId);
    }

    @Test
    @DisplayName("@MessageMapping room.generate-recommendations delega para RoomService.generateRecommendations")
    void generateRecommendations_delegatesToService() {
        String roomId = "room-123";
        UUID hostId = UUID.randomUUID();
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(hostId.toString());

        controller.generateRecommendations(roomId, principal);

        verify(roomService).generateRecommendations(roomId, hostId);
    }

    @Test
    @DisplayName("@MessageMapping room.reset delega para RoomService.resetRoom")
    void resetRoom_delegatesToService() {
        var roomId = "room-1";
        var hostId = UUID.randomUUID();
        var principal = principalWithSubject(hostId);

        controller.resetRoom(roomId, principal);

        verify(roomService).resetRoom(roomId, hostId);
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
