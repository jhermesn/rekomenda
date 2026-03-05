package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.RoomResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomControllerTest {

    private final RoomService roomService = mock(RoomService.class);
    private final RoomController controller = new RoomController(roomService);

    private static RoomResponse sampleRoom(String roomId, UUID hostId) {
        return new RoomResponse(
                roomId,
                hostId,
                RoomStatus.AGUARDANDO,
                Instant.now(),
                "http://localhost/rooms/" + roomId + "/join",
                List.of(),
                List.of(),
                null);
    }

    @Test
    @DisplayName("create delega para RoomService.createRoom usando subject do Jwt")
    void create_delegatesToService() {
        var hostId = UUID.randomUUID();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", hostId.toString()));

        when(roomService.createRoom(any())).thenReturn(sampleRoom("room-1", hostId));

        controller.create(jwt);

        verify(roomService).createRoom(hostId);
    }

    @Test
    @DisplayName("getRoom delega para RoomService.getRoom")
    void getRoom_delegatesToService() {
        var roomId = "room-1";
        when(roomService.getRoom(roomId)).thenReturn(sampleRoom(roomId, UUID.randomUUID()));

        controller.getRoom(roomId);

        verify(roomService).getRoom(roomId);
    }

    @Test
    @DisplayName("joinViaLink delega para RoomService.joinRoom e depois getRoom")
    void joinViaLink_delegatesToService() {
        var roomId = "room-1";
        var userId = UUID.randomUUID();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId.toString()));

        when(roomService.getRoom(roomId)).thenReturn(sampleRoom(roomId, userId));

        controller.joinViaLink(roomId, jwt);

        verify(roomService).joinRoom(roomId, userId, null);
        verify(roomService).getRoom(roomId);
    }
}
