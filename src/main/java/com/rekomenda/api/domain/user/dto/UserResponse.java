package com.rekomenda.api.domain.user.dto;

import com.rekomenda.api.domain.user.User;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nome,
        String username,
        String email,
        LocalDate dataNascimento
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getUsername(),
                user.getEmail(),
                user.getDataNascimento()
        );
    }
}
