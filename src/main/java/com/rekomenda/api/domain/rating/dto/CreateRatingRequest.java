package com.rekomenda.api.domain.rating.dto;

import com.rekomenda.api.domain.rating.RatingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRatingRequest(
        @NotNull @Positive Long conteudoId,
        @NotNull RatingType tipo
) {}
