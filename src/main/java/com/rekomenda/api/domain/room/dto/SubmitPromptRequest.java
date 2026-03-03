package com.rekomenda.api.domain.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitPromptRequest(@NotBlank @Size(max = 500) String descricao) {}
