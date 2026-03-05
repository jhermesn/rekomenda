package com.rekomenda.api.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverPasswordRequest(@NotBlank @Email @Size(max = 255) String email) {}
