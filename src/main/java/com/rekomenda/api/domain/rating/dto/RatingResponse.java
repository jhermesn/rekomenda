package com.rekomenda.api.domain.rating.dto;

import com.rekomenda.api.domain.rating.Rating;
import com.rekomenda.api.domain.rating.RatingType;

import java.time.Instant;
import java.util.UUID;

public record RatingResponse(
        UUID id,
        Long conteudoId,
        RatingType tipo,
        Instant dataAvaliacao
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getConteudoId(),
                rating.getTipo(),
                rating.getDataAvaliacao()
        );
    }
}
