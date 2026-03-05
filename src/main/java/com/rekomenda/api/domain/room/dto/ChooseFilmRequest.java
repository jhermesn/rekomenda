package com.rekomenda.api.domain.room.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChooseFilmRequest(@NotNull @Positive Long movieId) {}
