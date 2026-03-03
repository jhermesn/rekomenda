package com.rekomenda.api.domain.rating.dto;

import com.rekomenda.api.domain.rating.RatingType;
import jakarta.validation.constraints.NotNull;

public record CreateRatingRequest(
        @NotNull Long conteudoId,
        @NotNull RatingType tipo
) {}
