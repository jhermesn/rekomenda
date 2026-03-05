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
    private String nome;
    private String username;

    public static RoomParticipant join(UUID userId, String sessionId, String nome, String username) {
        return RoomParticipant.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status(ParticipantStatus.PENDENTE)
                .expulso(false)
                .nome(nome)
                .username(username)
                .build();
    }
}
