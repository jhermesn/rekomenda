package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.room.dto.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma sala e designa o usuário como host")
    public RoomResponse create(@AuthenticationPrincipal Jwt jwt) {
        return roomService.createRoom(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Retorna o estado atual da sala")
    public RoomResponse getRoom(@PathVariable String roomId) {
        return roomService.getRoom(roomId);
    }

    @PostMapping("/{roomId}/join")
    @Operation(summary = "Entra na sala via link de convite (fallback HTTP)")
    public RoomResponse joinViaLink(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        roomService.joinRoom(roomId, UUID.fromString(jwt.getSubject()), null);
        return roomService.getRoom(roomId);
    }
}
