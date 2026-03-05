package com.rekomenda.api.domain.auth.dto;

import jakarta.validation.constraints.*;
import com.rekomenda.api.shared.validation.Alphanumeric;
import com.rekomenda.api.shared.validation.NoControlChars;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 100) @NoControlChars String nome,
        @NotBlank @Size(min = 3, max = 30) @Alphanumeric String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Size(min = 8, max = 72) String senha
) {}
