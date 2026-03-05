package com.rekomenda.api.domain.room;

import lombok.*;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Room is stored as a Redis Hash under key "room:{id}" with a 30-minute TTL.
 * It is serialised/deserialised manually via RedisTemplate to avoid
 * the limitations of @RedisHash with nested collections.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    private String id;
    private UUID hostId;
    private RoomStatus status;
    private Instant createdAt;
    private List<RoomParticipant> participants;
    private List<MovieResponse> filmesRecomendados;
    private MovieResponse filmeEscolhido;

    public static Room create(UUID hostId) {
        return Room.builder()
                .id(UUID.randomUUID().toString())
                .hostId(hostId)
                .status(RoomStatus.AGUARDANDO)
                .createdAt(Instant.now())
                .participants(new ArrayList<>())
                .filmesRecomendados(new ArrayList<>())
                .filmeEscolhido(null)
                .build();
    }

    public boolean allParticipantsReady() {
        return !participants.isEmpty() &&
                participants.stream().allMatch(p -> ParticipantStatus.PRONTO == p.getStatus());
    }

    public boolean isExpired(long ttlMinutes) {
        return Instant.now().isAfter(createdAt.plusSeconds(ttlMinutes * 60));
    }
}
