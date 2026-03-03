package com.rekomenda.api.domain.room;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomParticipant {

    private UUID userId;
    private String sessionId;
    private ParticipantStatus status;
    private String descricaoDesejo;
    private boolean expulso;

    public static RoomParticipant join(UUID userId, String sessionId) {
        return RoomParticipant.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status(ParticipantStatus.PENDENTE)
                .expulso(false)
                .build();
    }
}
