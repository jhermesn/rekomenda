package com.rekomenda.api.domain.room.dto;

import jakarta.validation.constraints.NotNull;

public record ChooseFilmRequest(@NotNull Long movieId) {}
