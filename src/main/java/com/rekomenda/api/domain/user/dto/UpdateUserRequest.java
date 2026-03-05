package com.rekomenda.api.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) String nome,
        @Size(min = 3, max = 30) String username,
        @Pattern(regexp = "^$|.{8,}", message = "tamanho deve ser entre 8 e 2147483647 ou vazio") String novaSenha) {}
