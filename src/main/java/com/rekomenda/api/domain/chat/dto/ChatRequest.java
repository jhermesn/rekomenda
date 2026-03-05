package com.rekomenda.api.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank @Size(max = 500) String descricao,
        @Size(max = 20) List<Long> excludedMovieIds
) {
    public ChatRequest {
        excludedMovieIds = excludedMovieIds != null
                ? excludedMovieIds.stream().filter(id -> id != null && id > 0).toList()
                : List.of();
    }

    public ChatRequest(String descricao) {
        this(descricao, null);
    }
}
