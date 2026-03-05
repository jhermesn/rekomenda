package com.rekomenda.api.domain.user.dto;

import com.rekomenda.api.shared.validation.Alphanumeric;
import com.rekomenda.api.shared.validation.NoControlChars;
import com.rekomenda.api.shared.validation.NotBlankWhenPresent;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) @NotBlankWhenPresent @NoControlChars String nome,
        @Size(min = 3, max = 30) @Alphanumeric String username,
        @Size(min = 8) String novaSenha) {}
