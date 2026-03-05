package com.rekomenda.api.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResetPasswordRequest(
        @NotNull UUID token,
        @NotBlank @Size(min = 8, max = 72) String novaSenha
) {}
