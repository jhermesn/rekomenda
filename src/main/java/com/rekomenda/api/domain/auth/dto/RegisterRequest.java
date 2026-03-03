package com.rekomenda.api.domain.auth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String nome,
        @NotBlank @Size(min = 3, max = 30) String username,
        @NotBlank @Email String email,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Size(min = 8) String senha
) {}
