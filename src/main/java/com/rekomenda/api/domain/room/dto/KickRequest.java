package com.rekomenda.api.domain.room.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KickRequest(@NotNull UUID targetUserId) {}
