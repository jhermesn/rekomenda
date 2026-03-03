package com.rekomenda.api.domain.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) String nome,
        @Size(min = 3, max = 30) String username,
        @Size(min = 8) String novaSenha
) {}
