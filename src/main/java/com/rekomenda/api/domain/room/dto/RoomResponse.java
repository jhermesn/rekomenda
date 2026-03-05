package com.rekomenda.api.domain.room.dto;

import com.rekomenda.api.domain.room.Room;
import com.rekomenda.api.domain.room.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomResponse(
                String id,
                UUID hostId,
                RoomStatus status,
                Instant createdAt,
                String inviteLink,
                List<ParticipantInfo> participants,
                List<com.rekomenda.api.domain.recommendation.dto.MovieResponse> filmesRecomendados,
                com.rekomenda.api.domain.recommendation.dto.MovieResponse chosenMovie) {
        public record ParticipantInfo(UUID userId, String status, String nome, String username) {
        }

        public static RoomResponse from(Room room, String baseUrl) {
                var participants = room.getParticipants().stream()
                                .filter(p -> !p.isExpulso())
                                .map(p -> new ParticipantInfo(p.getUserId(), p.getStatus().name(), p.getNome(),
                                                p.getUsername()))
                                .toList();

                return new RoomResponse(
                                room.getId(),
                                room.getHostId(),
                                room.getStatus(),
                                room.getCreatedAt(),
                                baseUrl + "/rooms/" + room.getId(),
                                participants,
                                room.getFilmesRecomendados(),
                                room.getFilmeEscolhido());
        }
}
